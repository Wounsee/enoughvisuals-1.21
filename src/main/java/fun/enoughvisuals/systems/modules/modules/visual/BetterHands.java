package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;

@Register(name = "Hold My Items", id = "better_hands", description = "Hold My Items first-person animations", category = Category.VISUAL)
public class BetterHands extends Module {

    public static BetterHands INSTANCE;
    private boolean restoreSwingAnimation;
    private boolean restoreViewModel;

    public BetterHands() {
        INSTANCE = this;
    }

    @Override
    protected void onEnable() {
        restoreSwingAnimation = SwingAnimation.INSTANCE != null && SwingAnimation.INSTANCE.isEnabled();
        restoreViewModel = ViewModel.INSTANCE != null && ViewModel.INSTANCE.isEnabled();

        if (restoreSwingAnimation) {
            SwingAnimation.INSTANCE.disableSilent();
        }
        if (restoreViewModel) {
            ViewModel.INSTANCE.disableSilent();
        }
    }

    @Override
    protected void onDisable() {
        if (restoreSwingAnimation && SwingAnimation.INSTANCE != null) {
            SwingAnimation.INSTANCE.enableSilent();
        }
        if (restoreViewModel && ViewModel.INSTANCE != null) {
            ViewModel.INSTANCE.enableSilent();
        }

        restoreSwingAnimation = false;
        restoreViewModel = false;
    }

    public static boolean isHoldMyItemsEnabled() {
        return INSTANCE != null && INSTANCE.isEnabled();
    }

    public boolean useForwardAttack() {
        return false;
    }

    public boolean useNormalAttack() {
        return false;
    }

    public float spread() {
        return 0.65F;
    }

    public float animationDistance() {
        return 1.0F;
    }

    public float animationSpeed() {
        return 1.0F;
    }
}
