package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import org.joml.Matrix4f;

@Register(name = "MotionBlur", id = "motion_blur", description = "Кинематографичное размытие при движении камеры", category = Category.VISUAL)
public final class MotionBlur extends Module {
    private static MotionBlur INSTANCE;
    private static final Matrix4f LAST_POSITION = new Matrix4f();
    private static final Matrix4f LAST_PROJECTION = new Matrix4f();
    private static boolean matricesReady;
    private static long lastFrameNanos;
    private final fun.enoughvisuals.port.MotionBlur ported = new fun.enoughvisuals.port.MotionBlur();
    public final SliderSetting smoothness = add(new SliderSetting("Плавность", "smoothness", "", 0.68, 0.0, 1.0, 0.01));

    public MotionBlur() { INSTANCE = this; }
    public static MotionBlur active() { return INSTANCE != null && INSTANCE.isEnabled() ? INSTANCE : null; }
    public float smoothness() { return smoothness.get().floatValue(); }

    @Override
    protected void onEnable() {
        ported.plavnost.invoke(smoothness());
        ported.onEnable();
    }

    @Override
    protected void onDisable() {
        ported.onDisable();
        matricesReady = false;
        lastFrameNanos = 0L;
    }

    public static void captureWorldMatrices(WorldRenderContext context) {
        if (active() == null || context == null || context.matrixStack() == null) return;
        LAST_POSITION.set(context.matrixStack().peek().getPositionMatrix());
        LAST_PROJECTION.set(RenderSystem.getProjectionMatrix());
        matricesReady = true;
    }

    public static void captureWorldMatrices(Matrix4f position, Matrix4f projection) {
        if (active() == null || position == null || projection == null) return;
        LAST_POSITION.set(position);
        LAST_PROJECTION.set(projection);
        matricesReady = true;
    }

    public static void renderExact() {
        MotionBlur module = active();
        MinecraftClient client = MinecraftClient.getInstance();
        if (module == null || !matricesReady || client.currentScreen != null || client.world == null
                || client.player == null || client.gameRenderer == null || client.gameRenderer.getCamera() == null) return;
        long now = System.nanoTime();
        float frameMs = lastFrameNanos == 0L ? 16.7F : (now - lastFrameNanos) / 1_000_000.0F;
        lastFrameNanos = now;
        int fps = client.getCurrentFps();
        if (frameMs > 42.0F || fps > 0 && fps < 28) {
            fun.enoughvisuals.port.MotionBlurRenderer.getINSTANCE().invoke3();
            return;
        }
        module.ported.enabled = true;
        module.ported.plavnost.invoke(module.smoothness());
        fun.enoughvisuals.port.MotionBlurRenderer.MotionBlurRendererState2 state = module.ported.resolve();
        float performance = Math.max(0.35F, Math.min(1.0F, (fps - 24.0F) / 50.0F));
        state.floatValue *= performance * 1.32F;
        state.floatValue2 *= 1.15F;
        state.floatValue3 = Math.min(state.floatValue3 * 1.18F, 58.0F * performance + 12.0F);
        state.intValue = fps < 45 ? 4 : fps < 75 ? 6 : 8;
        fun.enoughvisuals.port.MotionBlurRenderer.getINSTANCE().invoke(
                client,
                client.gameRenderer.getCamera(),
                new Matrix4f(LAST_POSITION),
                new Matrix4f(LAST_PROJECTION),
                state);
    }
}
