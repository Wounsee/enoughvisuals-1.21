package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BindSetting;
import fun.enoughvisuals.utils.system.BindKeyUtil;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

@Register(
        name = "FreeLook",
        id = "free_look",
        description = "Свободный обзор от третьего лица",
        category = Category.VISUAL
)
public final class FreeLook extends Module {
    private static FreeLook instance;

    private final BindSetting activationBind = add(new BindSetting(
            "Клавиша", "activation_bind", "", GLFW.GLFW_KEY_LEFT_ALT));

    private boolean active;
    private float cameraYaw;
    private float cameraPitch;
    private Perspective previousPerspective = Perspective.FIRST_PERSON;

    public FreeLook() {
        instance = this;
    }

    public static boolean isActive() {
        return instance != null && instance.isEnabled() && instance.active;
    }

    public static float cameraYaw() {
        return instance == null ? 0f : instance.cameraYaw;
    }

    public static float cameraPitch() {
        return instance == null ? 0f : instance.cameraPitch;
    }

    public static boolean handleLookDelta(double yawDelta, double pitchDelta) {
        if (!isActive()) return false;
        instance.cameraYaw += (float) (yawDelta * 0.15);
        instance.cameraPitch = MathHelper.clamp(
                instance.cameraPitch + (float) (pitchDelta * 0.15), -90f, 90f);
        return true;
    }

    @Override
    public void onTick() {
        boolean shouldBeActive = isInGame()
                && mc.currentScreen == null
                && activationBind.isBound()
                && isHeld(activationBind.get());

        if (shouldBeActive && !active) activateCamera();
        else if (!shouldBeActive && active) deactivateCamera();

        if (active && mc.options.getPerspective() != Perspective.THIRD_PERSON_BACK) {
            mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        }
    }

    @Override
    protected void onDisable() {
        deactivateCamera();
    }

    private void activateCamera() {
        if (mc.player == null || mc.options == null) return;
        previousPerspective = mc.options.getPerspective();
        cameraYaw = mc.player.getYaw();
        cameraPitch = mc.player.getPitch();
        active = true;
        mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
    }

    private void deactivateCamera() {
        if (!active) return;
        active = false;
        if (mc.options != null) mc.options.setPerspective(previousPerspective);
    }

    private static boolean isHeld(int bind) {
        if (mc.getWindow() == null || bind == BindKeyUtil.NONE) return false;
        long window = mc.getWindow().getHandle();
        if (bind >= 1000) {
            return GLFW.glfwGetMouseButton(window, bind - 1000) == GLFW.GLFW_PRESS;
        }
        return GLFW.glfwGetKey(window, bind) == GLFW.GLFW_PRESS;
    }
}
