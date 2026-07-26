package fun.enoughvisuals.systems.modules.modules.utils;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;

@Register(
        name = "Auto Sprint",
        id = "auto_sprint",
        description = "Автоматически зажимает клавишу бега при движении вперед",
        category = Category.UTILS
)
public final class AutoSprint extends Module {

    private static AutoSprint INSTANCE;

    public AutoSprint() {
        INSTANCE = this;
    }

    public static AutoSprint getInstance() {
        return INSTANCE;
    }

    @Override
    public void onTick() {
        if (!isInGame() || mc.options == null) {
            return;
        }

        if (!isEnabled()) {
            // Release the sprint key when module is disabled
            if (mc.options.sprintKey.isPressed()) {
                mc.options.sprintKey.setPressed(false);
            }
            return;
        }

        // Hold the sprint key pressed while enabled
        mc.options.sprintKey.setPressed(true);

        // Force player sprint state if moving forward, not sneaking/using item, and has food
        if (mc.player.input != null
                && mc.player.input.movementForward > 0.8F
                && !mc.player.isSprinting()
                && !mc.player.isSneaking()
                && !mc.player.isUsingItem()
                && mc.player.getHungerManager().getFoodLevel() > 6) {
            mc.player.setSprinting(true);
        }
    }

    @Override
    protected void onDisable() {
        if (mc.options != null && mc.options.sprintKey.isPressed()) {
            mc.options.sprintKey.setPressed(false);
        }
    }
}
