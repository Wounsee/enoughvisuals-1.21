package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.modules.client.CustomTheme;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.ColorSetting;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.TriState;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

@Register(name = "Trails", id = "trails", description = "Красивый эффект сзади игрока при движении", category = Category.VISUAL)
public class Trails extends Module {

    private static Trails INSTANCE;

    public final ColorSetting customColor = add(new ColorSetting("Цвет", "color", "Цвет шлейфа", 0xFF5E7CFF));
    public final ModeSetting rotateMode = add(new ModeSetting("Вращение", "rotate_mode", "Ориентация кубиков", "Движение", "Движение", "Камера"));
    public final SliderSetting dashLength = add(new SliderSetting("Длина шлейфа", "dash_length", "Длина и время жизни шлейфа", 0.75, 0.5, 1.5, 0.01));
    public final SliderSetting moveLerp = add(new SliderSetting("Move Lerp", "move_lerp", "Плавность следования за игроком", 0.2, 0.1, 0.5, 0.01));
    public final BooleanSetting motionSmooth = add(new BooleanSetting("Сглаживание", "motion_smooth", "Сглаживание движения кубиков", false));
    public final BooleanSetting lighting = add(new BooleanSetting("Свечение", "lighting", "Дополнительное мягкое свечение", true));

    private static final int DASH_TEX_COUNT = 21;
    private static final long DASH_FRAME_TIME_MS = 80L;
    private static final int[] GROUP_SIZES = {11, 23, 32, 16, 32};

    private static final List<Identifier> DASH_TEXTURES = new ArrayList<>();
    private static final List<List<Identifier>> DASH_ANIM_GROUPS = new ArrayList<>();
    private static final Identifier BLOOM_TEX = Identifier.of("enough", "textures/particles/glow.png");

    static {
        for (int i = 1; i <= DASH_TEX_COUNT; i++) {
            DASH_TEXTURES.add(Identifier.of("enough", "textures/dash_cubes/dashcubic" + i + ".png"));
        }
        for (int g = 1; g <= GROUP_SIZES.length; g++) {
            List<Identifier> grp = new ArrayList<>();
            for (int f = 1; f <= GROUP_SIZES[g - 1]; f++) {
                grp.add(Identifier.of("enough", "textures/dash_cubes/group_dashs/group" + g + "/dashcubic" + f + ".png"));
            }
            DASH_ANIM_GROUPS.add(grp);
        }
    }

    private static final Function<Identifier, RenderLayer> CUBIC_LAYER = Util.memoize(tex ->
            RenderLayer.of("trails_cubic_" + tex,
                    VertexFormats.POSITION_TEXTURE_COLOR,
                    VertexFormat.DrawMode.QUADS,
                    1536,
                    false,
                    true,
                    RenderLayer.MultiPhaseParameters.builder()
                            .texture(new RenderPhase.Texture(tex, TriState.FALSE, false))
                            .program(RenderPhase.POSITION_TEXTURE_COLOR_PROGRAM)
                            .transparency(RenderPhase.LIGHTNING_TRANSPARENCY)
                            .cull(RenderPhase.DISABLE_CULLING)
                            .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
                            .writeMaskState(RenderPhase.COLOR_MASK)
                            .build(false)));

    private final List<DashCubic> cubics = new ArrayList<>();
    private final List<DashCubic> filteredCache = new ArrayList<>();
    private final Random rng = new Random(1234567891L);
    private final BufferAllocator allocator = new BufferAllocator(1 << 18);

    private final Vector3f renderRight = new Vector3f();
    private final Vector3f renderUp = new Vector3f();
    private final Quaternionf bloomRotCache = new Quaternionf();

    private float stateAnim = 0f;
    private float lightAnim = 0f;
    private double prevX, prevY, prevZ;
    private boolean prevInit = false;

    public Trails() {
        INSTANCE = this;
    }

    public static Trails active() {
        return INSTANCE != null && INSTANCE.isEnabled() ? INSTANCE : null;
    }

    @Override
    protected void onEnable() {
        cubics.clear();
        filteredCache.clear();
        stateAnim = 0f;
        prevInit = false;
    }

    @Override
    protected void onDisable() {
        cubics.clear();
        filteredCache.clear();
    }

    public void onTick() {
        if (mc.player == null) return;

        stateAnim += (1f - stateAnim) * 0.08f;
        lightAnim += ((lighting.get() ? 1f : 0f) - lightAnim) * 0.08f;

        if (!prevInit) {
            prevX = mc.player.getX();
            prevY = mc.player.getY();
            prevZ = mc.player.getZ();
            prevInit = true;
            return;
        }

        long nowMs = System.currentTimeMillis();

        int size = cubics.size();
        for (int i = 0; i < size; i++) {
            DashCubic c = cubics.get(i);
            c.cachedTimePC = Math.min(1f, (nowMs - c.startTime) / (float) c.base.rMTime);
            if (c.cachedTimePC >= 1f && c.alphaTarget != 0f) c.alphaTarget = 0f;
            c.alphaAnim += (c.alphaTarget - c.alphaAnim) * 0.08f;
        }

        for (int i = cubics.size() - 1; i >= 0; i--) {
            DashCubic c = cubics.get(i);
            if (c.cachedTimePC >= 1f && c.alphaTarget == 0f && c.alphaAnim < 0.02f)
                cubics.remove(i);
        }

        filteredCache.clear();
        for (int i = 0, n = cubics.size(); i < n; i++) {
            DashCubic c = cubics.get(i);
            if (c.alphaAnim > 0.05f) filteredCache.add(c);
        }

        float cameraPitch = mc.gameRenderer.getCamera().getPitch();
        boolean smooth = motionSmooth.get();
        int fsz = filteredCache.size();
        for (int i = 0; i < fsz; i++) {
            DashCubic cur = filteredCache.get(i);
            DashCubic next = (smooth && i + 1 < fsz) ? filteredCache.get(i + 1) : null;
            if (next != null && next.base.entityId != cur.base.entityId) next = null;
            cur.motionProcess(next, nowMs, cameraPitch);
        }

        double lx = prevX, ly = prevY, lz = prevZ;
        prevX = mc.player.getX();
        prevY = mc.player.getY();
        prevZ = mc.player.getZ();
        spawnFor(mc.player, lx, ly, lz);
    }

    private void spawnFor(LivingEntity entity, double lx, double ly, double lz) {
        if (entity == null) return;
        double dx = entity.getX() - lx, dy = entity.getY() - ly, dz = entity.getZ() - lz;

        if (dx * dx + dz * dz < 0.0025) return;
        double speed = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int count = (int) MathHelper.clamp((float) (speed / 0.3F), 1, 6);
        for (int i = 0; i < count; i++) {
            int rMTime = (int) ((550 + rng.nextInt(300)) * dashLength.get().doubleValue());
            cubics.add(new DashCubic(new DashBase(entity, 0.04f, (float) i / count, rMTime)));
        }
    }

    public static void renderInWorld(WorldRenderContext context) {
        Trails module = active();
        if (module == null || mc.player == null || module.stateAnim < 0.05f) return;
        if (module.filteredCache.isEmpty()) return;

        float ticks = context.tickCounter() != null ? context.tickCounter().getTickDelta(true) : 1.0f;
        float alphaPC = module.stateAnim;
        float lightPC = module.lightAnim;
        int fsz = module.filteredCache.size();
        long nowMs = System.currentTimeMillis();

        MatrixStack matrix = context.matrixStack();
        Vec3d cam = context.camera().getPos();
        double camX = cam.x, camY = cam.y, camZ = cam.z;

        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(module.allocator);

        float fCamYaw = context.camera().getYaw();
        float fCamPitch = context.camera().getPitch();
        Quaternionf camRot = context.camera().getRotation();
        camRot.transform(module.renderRight.set(1, 0, 0));
        camRot.transform(module.renderUp.set(0, 1, 0));
        module.bloomRotCache.identity()
                .rotateY((float) Math.toRadians(-fCamYaw))
                .rotateX((float) Math.toRadians(fCamPitch));

        for (int i = 0; i < fsz; i++) {
            DashCubic cubic = module.filteredCache.get(i);
            Identifier tex = cubic.dashTexture.getCurrent(nowMs);
            if (tex == null) continue;
            float aPC = cubic.alphaAnim * alphaPC;
            float ext = 128f * 0.033f * aPC * 0.3f;
            int col = cubic.color;
            matrix.push();
            matrix.translate(cubic.getRenderX(ticks) - camX,
                    cubic.getRenderY(ticks) - camY,
                    cubic.getRenderZ(ticks) - camZ);
            module.applyBodyRotation(matrix, cubic.rotate, fCamYaw, fCamPitch);
            matrix.scale(-0.1f, -0.1f, 0.1f);
            drawTexQuad(immediate.getBuffer(CUBIC_LAYER.apply(tex)),
                    matrix.peek().getPositionMatrix(),
                    -ext, -ext, ext, ext,
                    overCol(col, 0xFFFFFFFF, 0.4f), aPC);
            matrix.pop();
        }

        VertexConsumer bloomBuf = immediate.getBuffer(CUBIC_LAYER.apply(BLOOM_TEX));
        for (int i = 0; i < fsz; i++) {
            DashCubic cubic = module.filteredCache.get(i);
            float aPC = cubic.alphaAnim * alphaPC;
            float base = 128f * 0.033f * aPC * 0.3f;

            float timePcOf = 1f - cubic.cachedTimePC;
            int col = cubic.color;
            int baseCol = overCol(col, 0xFFFFFFFF, 0.18f);
            matrix.push();
            matrix.translate(cubic.getRenderX(ticks) - camX,
                    cubic.getRenderY(ticks) - camY,
                    cubic.getRenderZ(ticks) - camZ);
            matrix.multiply(module.bloomRotCache);
            matrix.scale(-0.1f, -0.1f, 0.1f);
            Matrix4f mat = matrix.peek().getPositionMatrix();
            drawTexQuad(bloomBuf, mat, -base * 6f, -base * 6f, base * 6f, base * 6f, swapAlpha(baseCol, 15f * aPC));
            drawTexQuad(bloomBuf, mat, -base * 2.2f, -base * 2.2f, base * 2.2f, base * 2.2f, swapAlpha(baseCol, 70f * aPC));
            drawTexQuad(bloomBuf, mat, -base * 1.1f, -base * 1.1f, base * 1.1f, base * 1.1f, swapAlpha(baseCol, 140f * aPC));
            if (lightPC > 0f) {
                float aMul = aPC * lightPC;
                float bigExt = base * (1f + 8f * timePcOf * aMul);
                drawTexQuad(bloomBuf, mat, -bigExt, -bigExt, bigExt, bigExt,
                        swapAlpha(multDark(baseCol, aMul / 4f), 95f * aMul));
            }
            matrix.pop();
        }

        immediate.draw();
    }

    private void applyBodyRotation(MatrixStack m, float[] rotate, float camYaw, float camPitch) {
        if (rotateMode.is("Движение")) {
            m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-rotate[0]));
            m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotate[1]));
        } else {
            m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camYaw));
            m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camPitch));
        }
    }

    private static void drawTexQuad(VertexConsumer buf, Matrix4f mat,
                                    float x1, float y1, float x2, float y2, int color, float alpha) {
        int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF,
                b = color & 0xFF, a = (int) MathHelper.clamp(((color >> 24) & 0xFF) * alpha, 0, 255);
        buf.vertex(mat, x1, y1, 0).texture(0, 1).color(r, g, b, a);
        buf.vertex(mat, x2, y1, 0).texture(1, 1).color(r, g, b, a);
        buf.vertex(mat, x2, y2, 0).texture(1, 0).color(r, g, b, a);
        buf.vertex(mat, x1, y2, 0).texture(0, 0).color(r, g, b, a);
    }

    private static void drawTexQuad(VertexConsumer buf, Matrix4f mat,
                                    float x1, float y1, float x2, float y2, int color) {
        int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF,
                b = color & 0xFF, a = (color >> 24) & 0xFF;
        buf.vertex(mat, x1, y1, 0).texture(0, 1).color(r, g, b, a);
        buf.vertex(mat, x2, y1, 0).texture(1, 1).color(r, g, b, a);
        buf.vertex(mat, x2, y2, 0).texture(1, 0).color(r, g, b, a);
        buf.vertex(mat, x1, y2, 0).texture(0, 0).color(r, g, b, a);
    }

    private int getDashColor() {
        return customColor.get();
    }

    private static int swapAlpha(int color, float alpha) {
        int a = (int) MathHelper.clamp(alpha, 0, 255);
        return (color & 0x00FFFFFF) | (a << 24);
    }

    private static int multDark(int color, float factor) {
        int r = (int) (((color >> 16) & 0xFF) * factor);
        int g = (int) (((color >> 8) & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);
        int a = (color >> 24) & 0xFF;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int overCol(int col1, int col2, float ratio) {
        int r1 = (col1 >> 16) & 0xFF, g1 = (col1 >> 8) & 0xFF, b1 = col1 & 0xFF;
        int r2 = (col2 >> 16) & 0xFF, g2 = (col2 >> 8) & 0xFF, b2 = col2 & 0xFF;
        int r = (int) (r1 + (r2 - r1) * ratio);
        int g = (int) (g1 + (g2 - g1) * ratio);
        int b = (int) (b1 + (b2 - b1) * ratio);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static float computeMotionYaw(double mx, double mz) {
        int y = (int) (Math.toDegrees(Math.atan2(mz, mx)) - 90);
        return y < 0 ? y + 360 : y;
    }

    private class DashCubic {
        float alphaAnim = 0f;
        float alphaTarget = 1f;
        float cachedTimePC = 0f;
        final long startTime = System.currentTimeMillis();
        final DashBase base;
        final int color;
        final float[] rotate = new float[2];
        final DashTexture dashTexture;

        DashCubic(DashBase base) {
            this.base = base;
            this.color = getDashColor();
            this.dashTexture = new DashTexture();
            recomputeRotate(mc.gameRenderer.getCamera().getPitch());
        }

        void recomputeRotate(float cameraPitch) {
            double spd = Math.sqrt(base.motionX * base.motionX + base.motionZ * base.motionZ);
            if (spd < 5e-4) {
                rotate[0] = (float) (360 * Math.random());
                rotate[1] = cameraPitch;
            } else {
                float motYaw = computeMotionYaw(base.motionX, base.motionZ);
                rotate[0] = motYaw - 60f - (base.prevYaw - base.yaw) * 3f;
                float diff = Math.abs(motYaw + 26.3f - base.yaw) % 360f;
                if (diff > 180f) diff = 360f - diff;
                rotate[1] = (diff < 10f || diff > 160f) ? -90f : cameraPitch;
            }
        }

        double getRenderX(float pt) { return base.prevPosX + (base.posX - base.prevPosX) * pt; }
        double getRenderY(float pt) { return base.prevPosY + (base.posY - base.prevPosY) * pt; }
        double getRenderZ(float pt) { return base.prevPosZ + (base.posZ - base.prevPosZ) * pt; }

        void motionProcess(DashCubic next, long nowMs, float cameraPitch) {
            base.prevPosX = base.posX;
            base.prevPosY = base.posY;
            base.prevPosZ = base.posZ;

            base.motionX = (next != null ? next.base.motionX : base.motionX) / 1.05;
            base.posX += 5.0 * base.motionX;
            base.motionY = (next != null ? next.base.motionY : base.motionY) / 1.05;
            base.posY += 5.0 * base.motionY / (base.motionY < 0 ? 1.0 : 3.5);
            base.motionZ = (next != null ? next.base.motionZ : base.motionZ) / 1.05;
            base.posZ += 5.0 * base.motionZ;
            recomputeRotate(cameraPitch);
        }
    }

    private static class DashBase {
        final int entityId;
        double motionX, motionY, motionZ;
        double posX, posY, posZ;
        double prevPosX, prevPosY, prevPosZ;
        final int rMTime;
        final float yaw, prevYaw;

        DashBase(LivingEntity entity, float speed, float offsetPC, int rmTime) {
            entityId = entity.getId();
            rMTime = rmTime;
            yaw = entity.getYaw();
            prevYaw = entity.prevYaw;

            double mx = -(entity.prevX - entity.getX());
            double my = -(entity.prevY - entity.getY());
            double mz = -(entity.prevZ - entity.getZ());
            motionX = mx; motionY = my; motionZ = mz;

            float h = entity.getHeight();
            float swimDiv = entity.isSubmergedInWater() ? 2.4f : 1f;
            posX = entity.prevX - mx * offsetPC + (-0.0875 + 0.175 * Math.random());
            posY = entity.prevY - my * offsetPC
                    + (h / swimDiv / 3.0 + h / swimDiv / 4.0 * Math.random() * 2.7);
            posZ = entity.prevZ - mz * offsetPC + (-0.0875 + 0.175 * Math.random());
            prevPosX = posX; prevPosY = posY; prevPosZ = posZ;
            motionX *= speed; motionY *= speed; motionZ *= speed;
        }
    }

    private class DashTexture {
        private final List<Identifier> textures;
        private final boolean animated;
        private final long spawnTime = System.currentTimeMillis();
        private final long animDur;

        DashTexture() {
            boolean doAnim = rng.nextInt(100) > 40 && !DASH_ANIM_GROUPS.isEmpty();
            animated = doAnim;
            if (doAnim) {
                textures = DASH_ANIM_GROUPS.get(rng.nextInt(DASH_ANIM_GROUPS.size()));
                animDur = (long) ((550 + rng.nextInt(300)) * dashLength.get().doubleValue());
            } else {
                textures = DASH_TEXTURES.isEmpty()
                        ? List.of()
                        : List.of(DASH_TEXTURES.get(rng.nextInt(DASH_TEXTURES.size())));
                animDur = 0;
            }
        }

        Identifier getCurrent(long nowMs) {
            if (textures.isEmpty()) return null;
            if (animated && textures.size() > 1 && animDur > 0) {
                int idx = (int) (((nowMs - spawnTime) / DASH_FRAME_TIME_MS) % textures.size());
                return textures.get(idx);
            }
            return textures.get(0);
        }
    }
}
