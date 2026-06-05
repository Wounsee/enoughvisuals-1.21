package fun.enoughvisuals;

import fun.enoughvisuals.rt.guard.QRuntime;
import fun.enoughvisuals.systems.config.ConfigSystem;
import fun.enoughvisuals.systems.hud.DragAndDrop;
import fun.enoughvisuals.systems.modules.api.ModuleManager;
import fun.enoughvisuals.systems.modules.modules.utils.*;
import fun.enoughvisuals.systems.modules.modules.visual.*;
import fun.enoughvisuals.ui.hud.BindsHud;
import fun.enoughvisuals.ui.hud.CustomScoreboardHud;
import fun.enoughvisuals.ui.hud.EffectsHud;
import fun.enoughvisuals.ui.hud.TargetHud;
import fun.enoughvisuals.ui.hud.WatermarkHud;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.render.CustomModelRenderer;
import fun.enoughvisuals.utils.sound.CombatSoundService;
import fun.enoughvisuals.utils.system.FuntimeEventSystem;
import fun.enoughvisuals.utils.system.NowPlayingService;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryUtil;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class EnoughCore {

    private EnoughCore() {
    }

    public static void initializeClient() {
        boolean strictRuntime = isStrictRuntime();
        if (!QRuntime.javaChecks() && strictRuntime) {
            Runtime.getRuntime().halt(0xE01);
        }

        if (!QRuntime.integrityChecks() && strictRuntime) {
            Runtime.getRuntime().halt(0xE02);
        }

        if (!QRuntime.nativeChecks() && strictRuntime) {
            Runtime.getRuntime().halt(0xE03);
        }

        QRuntime.startWatchdog();
        initCustomFontRendererCompat();

        ModuleManager manager = ModuleManager.getInstance();
        manager.register(
                new AspectRatio(),
                new Cooldowns(),
                new Crosshair(),
                new Animations(),
                new CustomWorld(),
                new CustomModel(),
                new SkyBox(),
                new BlockOverlay(),
                new CustomTab(),
                new CustomHitbox(),
                new GuiSize(),
                new CustomTheme(),
                new HitParticles(),
                new WorldParticles(),
                new MouseParticles(),
                new JumpEffect(),
                new Trails(),
                new Fullbright(),
                new ItemPhysics(),
                new ItemLight(),
                new NoRender(),
                new SwingAnimation(),
                new ViewModel(),
                new BetterHands(),
                new ShaderHands(),
                new Pet(),
                new FriendMarker(),
                new TargetESP(),
                new MineRadar(),
                new FakePlayer(),
                new ClickFriend(),
                new HitSound(),
                new KillSound(),
                new AutoAccept(),
                new AutoPotion(),
                new AutoReconnect(),
                new AutoRespawn(),
                new FastSwap(),
                new ItemPickupLogger(),
                new AutoResell(),
                new NameProtect(),
                new Prediction(),
                new StreamerMode(),
                new FastXP(),
                new LockSlot(),
                new ItemScroller(),
                new FTHelper(),
                new Zoom());

        manager.init();
        WorldRenderEvents.LAST.register(BlockOverlay::renderInWorld);
        WorldRenderEvents.LAST.register(CustomModelRenderer::renderInWorld);
        WorldRenderEvents.LAST.register(TargetESP::renderInWorld);
        WorldRenderEvents.LAST.register(FriendMarker::renderInWorld);
        WorldRenderEvents.LAST.register(CustomHitbox::renderInWorld);
        WorldRenderEvents.LAST.register(WorldParticles::renderInWorld);
        WorldRenderEvents.LAST.register(JumpEffect::renderInWorld);
        WorldRenderEvents.LAST.register(Trails::renderInWorld);
        HudRenderCallback.EVENT.register((drawContext, tickCounter) ->
                ModuleManager.getInstance().onRender(tickCounter.getTickDelta(true)));
        ClientTickEvents.END_CLIENT_TICK.register(CombatSoundService::onClientTick);
        ClientTickEvents.END_CLIENT_TICK.register(client -> ModuleManager.getInstance().onTick());
        ClientTickEvents.END_CLIENT_TICK.register(client -> ConfigSystem.getInstance().onClientTick(client));
        ClientTickEvents.END_CLIENT_TICK.register(FuntimeEventSystem.getInstance()::onClientTick);
        ClientTickEvents.END_CLIENT_TICK.register(client -> NowPlayingService.getNowPlaying());

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) {
                Animations.markChatMessage();
            }
            String line = message.getString();
            FuntimeEventSystem.getInstance().onChatLine(line);
        });
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, timestamp) -> {
            Animations.markChatMessage();
            String line = message.getString();
            FuntimeEventSystem.getInstance().onChatLine(line);
        });

        DragAndDrop dad = DragAndDrop.getInstance();
        dad.register("watermark", "Остров", null, WatermarkHud::new);
        dad.register("targethud", "Таргет Худ", null, TargetHud::new);
        dad.register("effects", "Эффекты", null, EffectsHud::new);
        dad.register("binds", "Бинды", null, BindsHud::new);
        dad.register("custom_scoreboard", "Скорборд", null, CustomScoreboardHud::new);

        ConfigSystem configSystem = ConfigSystem.getInstance();
        configSystem.initialize();
        boolean loaded = configSystem.loadLastOrDefault();
        if (!loaded) {
            dad.addElement("watermark", 0f, 0f);
            configSystem.save("default");
            configSystem.setLastConfigName("default");
        } else if (dad.getActiveElements().isEmpty()) {
            dad.addElement("watermark", 0f, 0f);
        }

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ConfigSystem.getInstance().autosaveOnExit());
    }

    public static void setIcon() {
        if (MinecraftClient.getInstance().getWindow() == null) return;

        Path iconPath = net.fabricmc.loader.api.FabricLoader.getInstance()
                .getModContainer("enoughvisuals")
                .flatMap(c -> c.findPath("assets/enough/logo/icon.png"))
                .orElse(null);

        if (iconPath == null) return;

        ByteBuffer byteBuffer = null;
        GLFWImage.Buffer imageBuffer = null;

        try (InputStream in = Files.newInputStream(iconPath);
             NativeImage image = NativeImage.read(in)) {

            byteBuffer = MemoryUtil.memAlloc(image.getWidth() * image.getHeight() * 4);

            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int argb = image.getColorArgb(x, y);
                    byteBuffer.put((byte) ((argb >> 16) & 0xFF));
                    byteBuffer.put((byte) ((argb >> 8) & 0xFF));
                    byteBuffer.put((byte) (argb & 0xFF));
                    byteBuffer.put((byte) ((argb >> 24) & 0xFF));
                }
            }
            byteBuffer.flip();
            imageBuffer = GLFWImage.malloc(1);
            imageBuffer.position(0);
            imageBuffer.width(image.getWidth());
            imageBuffer.height(image.getHeight());
            imageBuffer.pixels(byteBuffer);
            long windowHandle = MinecraftClient.getInstance().getWindow().getHandle();
            GLFW.glfwSetWindowIcon(windowHandle, imageBuffer);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (byteBuffer != null) MemoryUtil.memFree(byteBuffer);
            if (imageBuffer != null) imageBuffer.free();
        }
    }

    private static void initCustomFontRendererCompat() {
        try {
            CustomFontRenderer.init();
            return;
        } catch (NoSuchMethodError ignored) {
            // fallback below
        }

        try {
            Method fallback = CustomFontRenderer.class.getDeclaredMethod("init", int.class);
            fallback.setAccessible(true);
            fallback.invoke(null, 0);
        } catch (Throwable throwable) {
            throw new RuntimeException("font init error", throwable);
        }
    }

    public static boolean isStrictRuntime() {
        String env = System.getenv("WOUNSEE_STRICT_RUNTIME");
        if (env == null) return true;
        return !("0".equals(env) || "false".equalsIgnoreCase(env));
    }
}
