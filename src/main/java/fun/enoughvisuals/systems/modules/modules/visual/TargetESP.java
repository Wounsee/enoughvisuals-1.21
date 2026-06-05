package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.ColorSetting;
import fun.enoughvisuals.utils.render.CrystalRenderer;
import fun.enoughvisuals.utils.render.RenderUtil;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

@Register(
        name        = "Target ESP",
        id          = "target_esp",
        description = "Кристалики",
        category    = Category.VISUAL
)
public class TargetESP extends Module {

    private static TargetESP INSTANCE;

    // Для синхронизации с другими модулями (если нужно)
    public static LivingEntity globalTarget = null;

    public final ColorSetting colorA =
            add(new ColorSetting("Цвет 1", "color_a", "Первый цвет", 0xFF8A5CFF));
    public final ColorSetting colorB =
            add(new ColorSetting("Цвет 2", "color_b", "Второй цвет", 0xFF5CA0FF));

    private LivingEntity displayTarget  = null;
    private float        displayAlpha   = 0f;

    private LivingEntity prevTarget     = null;
    private float        prevAlpha      = 0f;

    private LivingEntity logicTarget    = null;
    private long         lastTargetTime = 0L; // Для сохранения таргета на 2 секунды

    public TargetESP() {
        INSTANCE = this;
    }

    @Override
    public void onDisable() {
        displayTarget  = null;
        prevTarget     = null;
        displayAlpha   = 0f;
        prevAlpha      = 0f;
        logicTarget    = null;
        globalTarget   = null;
        lastTargetTime = 0L;
    }

    @Override
    public void onTick() {
        LivingEntity found = findTarget();
        long now = System.currentTimeMillis();

        if (found != null) {

            // новая цель
            if (found != logicTarget) {

                // старая цель уходит в fade-out
                if (displayTarget != null && displayAlpha > 0.01f) {
                    prevTarget = displayTarget;
                    prevAlpha  = displayAlpha;
                }

                logicTarget   = found;
                displayTarget = found;

                // ВАЖНО: новая цель начинает с 0 прозрачности
                displayAlpha = 0f;
            }

            lastTargetTime = now;
        }
        else {
            // задержка 2 сек перед исчезновением
            if (now - lastTargetTime > 2000L) {

                if (logicTarget != null && displayTarget != null) {
                    prevTarget = displayTarget;
                    prevAlpha  = displayAlpha;
                }

                logicTarget   = null;
                displayTarget = null;
                displayAlpha  = 0f;
            }
        }

        globalTarget = (displayAlpha > 0f) ? displayTarget : null;
    }

    public static void renderInWorld(WorldRenderContext ctx) {
        if (INSTANCE == null || !INSTANCE.isEnabled()) return;
        if (mc.world == null || mc.player == null)     return;
        INSTANCE.doRender(ctx);
    }

    private void doRender(WorldRenderContext ctx) {
        MatrixStack ms = ctx.matrixStack();
        if (ms == null) return;
        float frameDelta = RenderUtil.getFrameDelta();

        // Плавная анимация прозрачности КАЖДЫЙ КАДР (идеальная плавность)
        float targetAlpha = (logicTarget != null) ? 1f : 0f;
        displayAlpha = RenderUtil.animate(displayAlpha, targetAlpha, 0.15f, frameDelta);

        if (prevTarget != null) {
            prevAlpha = RenderUtil.animate(prevAlpha, 0f, 0.15f, frameDelta);
            if (prevAlpha <= 0.01f) {
                prevTarget = null;
            }
        }

        float tickDelta = mc.getRenderTickCounter().getTickDelta(true);
        int   cA = colorA.get() & 0x00FFFFFF;
        int   cB = colorB.get() & 0x00FFFFFF;

        if (prevTarget != null && prevAlpha > 0.01f && prevTarget.isAlive() && !prevTarget.isRemoved()) {
            CrystalRenderer.render(ms, prevTarget, prevAlpha, tickDelta, cA, cB, 0.85f, 4);
        }

        if (displayTarget != null && displayAlpha > 0.01f && displayTarget.isAlive() && !displayTarget.isRemoved()) {
            CrystalRenderer.render(ms, displayTarget, displayAlpha, tickDelta, cA, cB, 0.85f, 4);
        }
    }

    private LivingEntity findTarget() {
        if (mc.world == null || mc.player == null) return null;

        // Получаем реальную дистанцию игрока
        double maxDist = 3.0;
        try {
            maxDist = mc.player.getEntityInteractionRange(); // 1.21.4
        } catch (Throwable t) {
            if (mc.interactionManager != null) {
                maxDist = mc.player.getEntityInteractionRange();
            }
        }

        return raycastTarget(maxDist);
    }

    private LivingEntity raycastTarget(double maxDist) {
        if (mc.player == null || mc.world == null) return null;

        Vec3d eyes = mc.player.getCameraPosVec(1.0f);
        Vec3d look = mc.player.getRotationVec(1.0f);
        Vec3d end  = eyes.add(look.multiply(maxDist));

        LivingEntity best     = null;
        double       bestDist = maxDist * maxDist;

        Box searchBox = mc.player.getBoundingBox().stretch(look.multiply(maxDist)).expand(1.2);
        for (Entity e : mc.world.getOtherEntities(mc.player, searchBox, entity -> entity instanceof LivingEntity)) {
            LivingEntity targetEntity = (LivingEntity) e;
            if (!targetEntity.isAlive())    continue;
            if (targetEntity.isRemoved())   continue;

            if (targetEntity.isInvisible() && !isWearingFullHighTierArmor(targetEntity)) {
                continue;
            }

            Box box = targetEntity.getBoundingBox().expand(0.6);
            Optional<Vec3d> hit = box.raycast(eyes, end);

            if (hit.isPresent()) {
                double d = eyes.squaredDistanceTo(hit.get());
                if (d < bestDist) {
                    bestDist = d;
                    best     = targetEntity;
                }
            }
        }
        return best;
    }

    private boolean isWearingFullHighTierArmor(LivingEntity entity) {
        int count = 0;
        for (ItemStack stack : entity.getArmorItems()) {
            if (stack == null || stack.isEmpty()) return false;

            String name = stack.getItem().getTranslationKey().toLowerCase();
            if (!name.contains("diamond") && !name.contains("netherite")) {
                return false;
            }
            count++;
        }
        return count == 4;
    }
}
