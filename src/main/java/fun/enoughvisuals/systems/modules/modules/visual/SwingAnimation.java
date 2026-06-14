package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.math.MathHelper;

@Register(name = "Swing Animation", id = "swing_animation", description = "Анимация руки", category = Category.VISUAL)
public class SwingAnimation extends Module {

    public static SwingAnimation INSTANCE;

    public final ModeSetting mode = add(new ModeSetting(
            "Вид",
            "mode",
            "Тип анимации",
            "Block Hit",
            "Block Hit",
            "Rotate 360"
    ));

    public final SliderSetting speed = add(new SliderSetting(
            "Скорость",
            "speed",
            "Скорость взмаха",
            2.0,
            1.0,
            5.0,
            0.1
    ));

    public SwingAnimation() {
        INSTANCE = this;
    }

    @Override
    protected boolean canEnable() {
        return BetterHands.INSTANCE == null || !BetterHands.INSTANCE.isEnabled();
    }

    public boolean shouldApplyAnimation(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) return false;
        Item item = itemStack.getItem();

        if (item == Items.FILLED_MAP || item == Items.CROSSBOW || item == Items.BOW || item == Items.TRIDENT) {
            return false;
        }

        UseAction action = item.getUseAction(itemStack);
        return action != UseAction.DRINK && action != UseAction.EAT;
    }

    public float getSpeedMultiplier() {
        return speed.get().floatValue();
    }

    public SwingTransformations getTransform(float swingProgress) {
        Preset preset = getPreset(mode.get());

        float progress = MathHelper.clamp(swingProgress, 0.0f, 1.0f);
        if (preset.swingBack) {
            progress = MathHelper.sin(MathHelper.sqrt(progress) * (float) Math.PI);
        }

        return new SwingTransformations(
                MathHelper.lerp(progress, preset.from.anchorX, preset.to.anchorX),
                MathHelper.lerp(progress, preset.from.anchorY, preset.to.anchorY),
                MathHelper.lerp(progress, preset.from.anchorZ, preset.to.anchorZ),
                MathHelper.lerp(progress, preset.from.moveX, preset.to.moveX),
                MathHelper.lerp(progress, preset.from.moveY, preset.to.moveY),
                MathHelper.lerp(progress, preset.from.moveZ, preset.to.moveZ),
                MathHelper.lerp(progress, preset.from.rotateX, preset.to.rotateX),
                MathHelper.lerp(progress, preset.from.rotateY, preset.to.rotateY),
                MathHelper.lerp(progress, preset.from.rotateZ, preset.to.rotateZ)
        );
    }

    private Preset getPreset(String name) {
        return switch (name) {
            case "Bonk" -> BONK;
            case "Rotate 360" -> ROTATE_360;
            case "From Me" -> FROM_ME;
            default -> BLOCK_HIT;
        };
    }

    private static final Preset BLOCK_HIT = new Preset(
            true,
            new SwingTransformations(0.0f, -0.05f, -0.7f, 1.0500001f, -0.7f, -1.1f, -120.0f, -135.0f, -60.0f),
            new SwingTransformations(0.0f, -0.05f, -0.7f, 1.0500001f, -0.7f, -1.1f, -120.0f, -180.0f, -60.0f)
    );

    private static final Preset BONK = new Preset(
            true,
            new SwingTransformations(0.0f, -0.4f, -0.65000004f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f),
            new SwingTransformations(0.0f, -0.4f, -0.65000004f, 0.0f, 0.0f, 0.0f, -45.0f, 0.0f, 0.0f)
    );

    private static final Preset ROTATE_360 = new Preset(
            false,
            new SwingTransformations(0.0f, -0.4f, -0.65000004f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f),
            new SwingTransformations(0.0f, -0.4f, -0.65000004f, 0.0f, 0.0f, 0.0f, -360.0f, 0.0f, 0.0f)
    );

    private static final Preset FROM_ME = new Preset(
            true,
            new SwingTransformations(0.0f, 0.0f, -1.1f, 0.2f, 0.0f, -0.1f, -135.0f, 45.0f, 60.0f),
            new SwingTransformations(0.0f, 0.0f, -1.1f, 0.2f, 0.0f, -0.3f, -180.0f, 45.0f, 60.0f)
    );


    private record Preset(boolean swingBack, SwingTransformations from, SwingTransformations to) {}

    public record SwingTransformations(
            float anchorX, float anchorY, float anchorZ,
            float moveX, float moveY, float moveZ,
            float rotateX, float rotateY, float rotateZ
    ) {}
}
