package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BindSetting;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

@Register(name = "Zoom", id = "zoom", description = "Плавное приближение камеры", category = Category.VISUAL)
public class Zoom extends Module {

    private static Zoom INSTANCE;

    public final BindSetting holdKey = add(new BindSetting(
            "Кнопка удержания",
            "hold_key",
            "Кнопка для зума при зажатии",
            GLFW.GLFW_KEY_C
    ));

    private float zoomFactor = 4.0F;

    private float currentMult = 1.0F;
    private float lastMult = 1.0F;

    private static final double ZOOM_SENSITIVITY_MULTIPLIER = 0.7;

    private double originalSensitivity;
    private boolean sensitivityApplied;

    public Zoom() {
        INSTANCE = this;
    }

    public static Zoom getInstance() {
        return INSTANCE;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.getWindow() == null) return;

        lastMult = currentMult;

        boolean holdingKey = isZoomKeyHeld();

        float target = holdingKey ? 1.0F / zoomFactor : 1.0F;

        currentMult += (target - currentMult) * 0.35F;

        updateSensitivity(holdingKey);
    }
    public boolean isZoomKeyHeld() {
        if (!isEnabled() || mc.player == null || mc.getWindow() == null) return false;
        if (mc.currentScreen != null) return false;

        return holdKey.isBound()
                && GLFW.glfwGetKey(mc.getWindow().getHandle(), holdKey.get()) == GLFW.GLFW_PRESS;
    }
    @Override
    protected void onDisable() {
        currentMult = 1.0F;
        lastMult = 1.0F;

        updateSensitivity(false);
    }

    public float getFovMultiplier(float tickDelta) {
        return MathHelper.lerp(tickDelta, lastMult, currentMult);
    }

    public void onMouseScroll(double amount) {
        if (!isEnabled()) return;

        zoomFactor += amount > 0 ? 0.25F : -0.25F;
        zoomFactor = MathHelper.clamp(zoomFactor, 1.5F, 10.0F);
    }

    private void updateSensitivity(boolean holdingKey) {
        if (holdingKey && !sensitivityApplied) {
            originalSensitivity = mc.options.getMouseSensitivity().getValue();
            mc.options.getMouseSensitivity().setValue(originalSensitivity * ZOOM_SENSITIVITY_MULTIPLIER);
            sensitivityApplied = true;
        } else if (!holdingKey && sensitivityApplied) {
            mc.options.getMouseSensitivity().setValue(originalSensitivity);
            sensitivityApplied = false;
        }
    }
}
