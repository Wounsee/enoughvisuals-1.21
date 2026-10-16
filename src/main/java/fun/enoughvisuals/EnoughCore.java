package fun.enoughvisuals;

import fun.enoughvisuals.systems.config.ConfigSystem;
import fun.enoughvisuals.systems.cosmetic.CosmeticCatalog;
import fun.enoughvisuals.systems.hud.DragAndDrop;
import fun.enoughvisuals.systems.modules.api.ModuleManager;
import fun.enoughvisuals.systems.modules.modules.client.*;
import fun.enoughvisuals.systems.modules.modules.utils.*;
import fun.enoughvisuals.systems.modules.modules.visual.*;
import fun.enoughvisuals.systems.network.RemoteConfig;
import fun.enoughvisuals.systems.network.DiscordPresenceService;
import fun.enoughvisuals.systems.events.EventFeedService;
import fun.enoughvisuals.ui.gate.GateScreen;
import fun.enoughvisuals.ui.mainmenu.WindowTitleAnimator;
import fun.enoughvisuals.ui.hud.BindsHud;
import fun.enoughvisuals.ui.hud.CooldownsHud;
import fun.enoughvisuals.ui.hud.EffectsHud;
import fun.enoughvisuals.ui.hud.TargetHud;
import fun.enoughvisuals.ui.hud.WatermarkHud;
import fun.enoughvisuals.ui.hud.PlayerHud;
import fun.enoughvisuals.ui.hud.NotificationHud;
import fun.enoughvisuals.ui.hud.ArmorHud;
import fun.enoughvisuals.ui.hud.InventoryHud;
import fun.enoughvisuals.ui.hud.HotbarHud;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.render.ShaderEspRenderer;
import fun.enoughvisuals.utils.sound.CombatSoundService;
import fun.enoughvisuals.utils.system.FuntimeEventSystem;
import fun.enoughvisuals.utils.system.NowPlayingService;
import fun.enoughvisuals.utils.system.TitleBarHelper;
import fun.enoughvisuals.ui.clickgui.components.IntegratedToolPage;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import fun.enoughvisuals.systems.emotions.EmotionWheelManager;
import fun.enoughvisuals.systems.graffiti.GraffitiManager;
import fun.enoughvisuals.ui.screen.EmotionWheelScreen;
import fun.enoughvisuals.ui.screen.GraffitiWheelScreen;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryUtil;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class EnoughCore {

    private static boolean windowIconApplied;

    private EnoughCore() {
    }

    public static void initializeClient() {

        initCustomFontRendererCompat();
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public Identifier getFabricId() {
                        return Identifier.of("enough", "pulse_emotions");
                    }

                    @Override
                    public void reload(ResourceManager manager) {
                        EmotionWheelManager.getInstance().reloadResources();
                        MinecraftClient client = MinecraftClient.getInstance();
                        if (client != null) client.execute(IntegratedToolPage::invalidateCosmeticPreviewCache);
                    }
                });
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> {
            IntegratedToolPage.invalidateCosmeticPreviewCache();
            GraffitiManager.getInstance().clearWorld();
        });
        applyFirstLaunchDisplayDefaults();
        RemoteConfig.getInstance().start();
        // initializeClient itself is invoked from CLIENT_STARTED. Registering
        // another CLIENT_STARTED callback here is too late: that callback will
        // never see the event which is already being dispatched.
        DiscordPresenceService.getInstance().start();
        ClientTickEvents.END_CLIENT_TICK.register(EventFeedService.getInstance()::onClientTick);
        ClientTickEvents.END_CLIENT_TICK.register(TitleBarHelper::applyOnce);
        ClientTickEvents.END_CLIENT_TICK.register(RemoteConfig.getInstance()::onClientTick);
        ModuleManager manager = ModuleManager.getInstance();
        manager.register(
                new AspectRatio(),
                new Cooldowns(),
                new Crosshair(),
                new Animations(),
                new WorldTweaks(),
                new SkyBox(),
                new BlockOverlay(),
                new CustomHitbox(),
                new GuiSize(),
                new CustomTheme(),
                new Friends(),
                new Configs(),
                new GPS(),
                new Globals(),
                new HUD(),
                new HitParticles(),
                new JumpCircle(),
                new ChinaHat(),
                new KillEffect(),
                new Particles(),
                new AtmoDawnFog(),
                new AttackEffect(),
                new ColorPlus(),
                new MotionBlur(),
                new WorldCubes(),
                new MouseParticles(),
                new Fullbright(),
                new ItemPhysics(),
                new ItemLight(),
                new NoRender(),
                new SwingAnimation(),
                new ViewModel(),
                new BetterHands(),
                new ShaderHands(),
                new ShaderPlayer(),
                new JumpCube(),
                new Svetoch(),
                new FreeLook(),
                new Emotions(),
                new Graffiti(),
                new WebPanels(),
                new FriendMarker(),
                new TargetESP(),
                new Optimization(),
                new FakePlayer(),
                new ClickFriend(),
                new ChatHelper(),
                new HitSound(),
                new KillSound(),
                new AutoAccept(),
                new AutoReconnect(),
                new AutoRespawn(),
                new AutoSprint(),
                new PvPSave(),
                new SendCord(),
                new CustomSphereSkin(),
                new FastSearch(),
                new FastSwap(),
                new ItemPickupLogger(),
                new NameProtect(),
                new Macros(),
                new Prediction(),
                new StreamerMode(),
                new LockSlot(),
                new ItemScroller(),
                new FTHelper(),
                new Zoom(),
                new ItemSwap(),
                new HitColor(),
                new LowArmorNotify(),
                new Trails(),
                new SpecBind(),
                new DeathCords(),
                new AutoNear(),
                new HealingHelper(),
                new TrapTimer(),
                new HealthIndicator(),
                new HitRange(),
                new MaceIndicator(),
                new NoFluid(),
                new ShulkerPreview(),
                new TotemTracker()
        );

        manager.init();

        WorldRenderEvents.BEFORE_ENTITIES.register(ShaderEspRenderer::beginWorldFrame);
        WorldRenderEvents.LAST.register(ShaderEspRenderer::renderInWorld);
        WorldRenderEvents.LAST.register(AtmoDawnFog::renderInWorld);
        WorldRenderEvents.LAST.register(BlockOverlay::renderInWorld);
        WorldRenderEvents.LAST.register(TargetESP::renderInWorld);
        WorldRenderEvents.LAST.register(KillEffect::renderInWorld);
        WorldRenderEvents.LAST.register(FriendMarker::renderInWorld);
        WorldRenderEvents.LAST.register(CustomHitbox::renderInWorld);
        WorldRenderEvents.LAST.register(Particles::renderInWorld);
        WorldRenderEvents.LAST.register(WorldCubes::renderInWorld);
        WorldRenderEvents.LAST.register(JumpCircle::renderInWorld);
        WorldRenderEvents.LAST.register(JumpEffect::renderInWorld);
        WorldRenderEvents.LAST.register(JumpCube::renderInWorld);
        WorldRenderEvents.LAST.register(Svetoch::renderInWorld);
        WorldRenderEvents.LAST.register(Trails::renderInWorld);
        WorldRenderEvents.LAST.register(AttackEffect::renderInWorld);
        WorldRenderEvents.LAST.register(GraffitiManager.getInstance()::renderWorld);
        WorldRenderEvents.LAST.register(WebPanels::renderInWorld);

        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            ModuleManager.getInstance().onRender(tickCounter.getTickDelta(true));
            if (!(MinecraftClient.getInstance().currentScreen instanceof EmotionWheelScreen)) {
                EmotionWheelManager.getInstance().render(drawContext);
            }
            if (!(MinecraftClient.getInstance().currentScreen instanceof GraffitiWheelScreen)) {
                GraffitiManager.getInstance().renderWheel(drawContext);
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            if (!windowIconApplied && client.getWindow() != null) {

                setIcon();

                windowIconApplied = true;

                client.updateWindowTitle();
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(EnoughCore::tickGate);
        ClientTickEvents.END_CLIENT_TICK.register(WindowTitleAnimator::tick);

        ClientTickEvents.END_CLIENT_TICK.register(CombatSoundService::onClientTick);

        ClientTickEvents.END_CLIENT_TICK.register(client -> ModuleManager.getInstance().onTick());
        ClientTickEvents.END_CLIENT_TICK.register(client -> CosmeticCatalog.restoreWhenReady());

        ClientTickEvents.END_CLIENT_TICK.register(client -> ConfigSystem.getInstance().onClientTick(client));

        ClientTickEvents.END_CLIENT_TICK.register(FuntimeEventSystem.getInstance()::onClientTick);

        ClientTickEvents.END_CLIENT_TICK.register(client -> NowPlayingService.getNowPlaying());

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {

            if (!overlay) {

                Animations.markChatMessage();
            }

            String line = message.getString();

            FuntimeEventSystem.getInstance().onChatLine(line);
            EventFeedService.getInstance().onChatLine(line);
        });

        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, timestamp) -> {

            Animations.markChatMessage();

            String line = message.getString();

            FuntimeEventSystem.getInstance().onChatLine(line);
            EventFeedService.getInstance().onChatLine(line);
        });

        DragAndDrop dad = DragAndDrop.getInstance();

        dad.register("watermark", "Ватермарка", null, WatermarkHud::new);

        dad.register("targethud", "Таргет Худ", null, TargetHud::new);

        dad.register("effects", "Эффекты", null, EffectsHud::new);

        dad.register("binds", "Бинды", null, BindsHud::new);

        dad.register("cooldowns", "Кулдауны", null, CooldownsHud::new);
        dad.register("player", "Плеер", null, PlayerHud::new);
        dad.register("notifications", "Уведомления", null, NotificationHud::new);
        dad.register("armor", "Броня", null, ArmorHud::new);
        dad.register("inventory", "Инвентарь", null, InventoryHud::new);
        dad.register("custom_hotbar", "Кастомный хотбар", null, HotbarHud::new);

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

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            IntegratedToolPage.invalidateCosmeticPreviewCache();
            DiscordPresenceService.getInstance().stop();
            ConfigSystem.getInstance().autosaveOnExit();
        });
    }

    private static void applyFirstLaunchDisplayDefaults() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null || client.getWindow() == null) {
            return;
        }

        Path stateDir = client.runDirectory.toPath().resolve("enoughvisuals");
        Path marker = stateDir.resolve("display-defaults-v1.done");
        if (Files.exists(marker)) {
            return;
        }

        try {
            client.options.getGuiScale().setValue(2);
            client.options.getFullscreen().setValue(true);
            if (!client.getWindow().isFullscreen()) {
                client.getWindow().toggleFullscreen();
            }
            client.onResolutionChanged();
            client.options.write();
            Files.createDirectories(stateDir);
            Files.writeString(marker, "guiScale=2\nfullscreen=true\n");
        } catch (Exception exception) {
            System.err.println("[EnoughVisuals] Failed to apply first-launch display defaults: "
                    + exception.getMessage());
        }
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

    private static void tickGate(MinecraftClient client) {

        if (client == null) {

            return;
        }

        boolean gated = RemoteConfig.getInstance().isGated();

        if (client.currentScreen instanceof GateScreen) {

            if (!gated) {

                client.setScreen(new net.minecraft.client.gui.screen.TitleScreen());
            }

            return;
        }

        // Gate only while in menus so we never interrupt an active world session,

        // but the player can never proceed past the menu to join while gated.

        if (gated && client.world == null && client.currentScreen != null) {

            client.setScreen(new GateScreen());
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
}
