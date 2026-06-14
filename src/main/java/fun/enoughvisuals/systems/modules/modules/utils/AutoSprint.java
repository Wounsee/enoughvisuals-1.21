package fun.enoughvisuals.systems.modules.modules.utils;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;

@Register(
        name = "Auto Sprint",
        id = "auto_sprint",
        description = "Автоматически бегает",
        category = Category.UTILS
)
public final class AutoSprint extends Module {

    @Override
    public void onTick() {
        if (!isInGame()) return;

        if (mc.currentScreen != null) {
            stopControlledSprint();
            return;
        }

        boolean canSprint = mc.player.input != null
                && mc.player.input.movementForward > 0.8F
                && !mc.player.isSneaking()
                && !mc.player.isUsingItem()
                && !mc.player.horizontalCollision
                && (mc.player.getHungerManager().getFoodLevel() > 6
                || mc.player.getAbilities().allowFlying);

        if (canSprint && !mc.player.isSprinting()) {
            mc.player.setSprinting(true);
        } else if (!canSprint) {
            stopControlledSprint();
        }
    }

    private void stopControlledSprint() {
        if (mc.player != null && mc.player.isSprinting() && !mc.options.sprintKey.isPressed()) {
            mc.player.setSprinting(false);
        }
    }
}
