package fun.enoughvisuals.systems.modules.modules.utils;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.utils.player.ClientActionUtil;
import net.minecraft.util.PlayerInput;

@Register(name = "Shift Tap", id = "shift_tap", description = "Briefly taps sneak when you hit", category = Category.UTILS)
public class ShiftTap extends Module {
    private static ShiftTap INSTANCE;
    private static int tapTicks = 0;
    private static boolean restorePressed;

    public ShiftTap() {
        INSTANCE = this;
    }

    public static void onAttack() {
        if (INSTANCE == null || !INSTANCE.isEnabled() || !ClientActionUtil.inGame()) return;
        restorePressed = mc.options != null && mc.options.sneakKey != null && mc.options.sneakKey.isPressed();
        tapTicks = 3;
        applySneak(true);
    }

    @Override
    public void onTick() {
        if (tapTicks > 0) {
            applySneak(true);
            tapTicks--;
            if (tapTicks == 0) {
                applySneak(restorePressed);
                restorePressed = false;
            }
        }
    }

    @Override
    protected void onDisable() {
        if (tapTicks > 0) {
            applySneak(restorePressed);
        }
        tapTicks = 0;
        restorePressed = false;
    }

    private static void applySneak(boolean pressed) {
        if (mc.options != null && mc.options.sneakKey != null) {
            mc.options.sneakKey.setPressed(pressed);
        }
        if (mc.player != null) {
            mc.player.setSneaking(pressed);
            if (mc.player.input != null && mc.player.input.playerInput != null) {
                PlayerInput input = mc.player.input.playerInput;
                mc.player.input.playerInput = new PlayerInput(
                        input.forward(),
                        input.backward(),
                        input.left(),
                        input.right(),
                        input.jump(),
                        pressed,
                        input.sprint()
                );
            }
        }
    }
}
