package punchy.client.render;

import net.minecraft.class_1268;
import net.minecraft.class_1306;
import net.minecraft.class_1819;
import net.minecraft.class_310;
import net.minecraft.class_4587;
import net.minecraft.class_746;
import net.minecraft.class_7833;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/ShieldImpactPhysics.class */
public class ShieldImpactPhysics {
    private static final ShieldImpactPhysics INSTANCE = new ShieldImpactPhysics();
    private static final int IMPACT_TICKS = 5;
    private static final float IMPACT_ROT_X = -18.0f;
    private static final float IMPACT_ROT_Z = 4.0f;
    private static final float IMPACT_OFF_Z = -0.12f;
    private static final float IMPACT_OFF_Y = -0.02f;
    private long startTick = -1;
    private class_1306 impactArm = class_1306.field_6183;
    private float jitterPhase = 0.0f;

    public static ShieldImpactPhysics get() {
        return INSTANCE;
    }

    public void trigger(class_746 player) {
        class_1306 arm = resolveImpactArm(player);
        if (arm == null) {
            return;
        }
        trigger(arm);
    }

    public void trigger(class_1306 arm) {
        class_310 mc = class_310.method_1551();
        if (mc.field_1687 == null) {
            return;
        }
        this.startTick = mc.field_1687.method_8510();
        this.impactArm = arm;
        this.jitterPhase = (float) (Math.random() * 3.141592653589793d * 2.0d);
    }

    public static class_1306 resolveImpactArm(class_746 player) {
        class_1306 arm;
        if (player == null) {
            return null;
        }
        class_1306 arm2 = player.method_6068();
        if (player.method_6079().method_7909() instanceof class_1819) {
            arm = arm2 == class_1306.field_6183 ? class_1306.field_6182 : class_1306.field_6183;
        } else if (player.method_6047().method_7909() instanceof class_1819) {
            arm = player.method_6068();
        } else if (player.method_6115() && (player.method_6030().method_7909() instanceof class_1819)) {
            class_1268 hand = player.method_6058();
            boolean right = player.method_6068() == class_1306.field_6183;
            if (hand == class_1268.field_5808) {
                arm = right ? class_1306.field_6183 : class_1306.field_6182;
            } else {
                arm = right ? class_1306.field_6182 : class_1306.field_6183;
            }
        } else {
            return null;
        }
        return arm;
    }

    public void applyTo(class_4587 poseStack, class_1306 arm, float partialTicks) {
        float impact;
        class_310 mc = class_310.method_1551();
        if (mc.field_1687 == null || this.startTick < 0 || arm != this.impactArm) {
            return;
        }
        long now = mc.field_1687.method_8510();
        long elapsed = now - this.startTick;
        if (elapsed < 0) {
            elapsed = 0;
        }
        if (elapsed >= 5) {
            return;
        }
        float t = (elapsed + partialTicks) / 5.0f;
        if (t > 1.0f) {
            t = 1.0f;
        }
        if (t < 0.4f) {
            impact = t / 0.4f;
        } else {
            impact = 1.0f - ((t - 0.4f) / 0.6f);
        }
        float wobble = 1.0f + (0.05f * ((float) Math.sin(this.jitterPhase + (t * 3.1415927f * 2.0f))));
        float strength = impact * impact * (3.0f - (2.0f * impact)) * wobble;
        float sign = arm == class_1306.field_6182 ? -1.0f : 1.0f;
        float rotX = IMPACT_ROT_X * strength;
        float rotZ = IMPACT_ROT_Z * strength * sign;
        float offZ = IMPACT_OFF_Z * strength;
        float offY = IMPACT_OFF_Y * strength;
        poseStack.method_46416(0.0f, offY, offZ);
        poseStack.method_22907(class_7833.field_40714.rotationDegrees(rotX));
        poseStack.method_22907(class_7833.field_40718.rotationDegrees(rotZ));
    }
}
