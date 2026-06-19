package fun.enoughvisuals.systems.modules.modules.visual;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.mixins.accessors.PersistentProjectileEntityAccessor;
import fun.enoughvisuals.mixins.accessors.GameRendererAccessor;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.render.ScreenScaleUtil;
import fun.enoughvisuals.utils.render.blur.GlassUtil;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Register(name = "Prediction", id = "prediction", description = "Траектория полета", category = Category.VISUAL)
public class Prediction extends Module {

    private static Prediction INSTANCE;
    private boolean registered = false;
    private final List<PredictionResult> tickPredictions = new ArrayList<>();
    private PredictionResult framePrediction = null;
    private final Map<Integer, ImpactLabel> impactLabels = new HashMap<>();
    private final Map<Integer, Long> projectileSpawnTimesMs = new HashMap<>();
    private final Set<Integer> activeProjectileIds = new HashSet<>();

    private static final int HELD_MAX_TICKS = 320;
    private static final int FLYING_MAX_TICKS = 200;
    private static final long LABEL_TTL_MS = 10_000L;
    private static final double LABEL_HIT_SMOOTH = 0.28;
    private static final float PREDICT_TUBE_RADIUS = 0.035f;
    private static final int PREDICT_TUBE_SIDES = 8;
    private static final float HELD_LINE_WIDTH = 2.0f;

    public Prediction() {
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        if (!registered) {
            WorldRenderEvents.LAST.register(this::onRender3D);
            registered = true;
        }
    }

    @Override
    public void onTick() {
        if (!isInGame()) {
            tickPredictions.clear();
            framePrediction = null;
            impactLabels.clear();
            projectileSpawnTimesMs.clear();
            activeProjectileIds.clear();
        }
    }

    @Override
    protected void onDisable() {
        tickPredictions.clear();
        framePrediction = null;
        impactLabels.clear();
        projectileSpawnTimesMs.clear();
        activeProjectileIds.clear();
    }

    private void onRender3D(WorldRenderContext context) {
        if (!isEnabled() || !isInGame()) return;
        framePrediction = null;
        tickPredictions.clear();
        activeProjectileIds.clear();
        long now = System.currentTimeMillis();

        float tickDelta = context.tickCounter().getTickDelta(true);
        Camera camera = context.camera();
        if (camera == null) return;
        Vec3d camPos = stableCameraPos(tickDelta);

        Box projectileSearchBox = mc.player.getBoundingBox().expand(96.0);
        for (Entity entity : mc.world.getOtherEntities(mc.player, projectileSearchBox, candidate -> candidate instanceof ProjectileEntity)) {
            ProjectileEntity proj = (ProjectileEntity) entity;
            int projectileId = proj.getId();
            if (isProjectileFinished(proj)) {
                removeProjectileState(projectileId);
                continue;
            }
            activeProjectileIds.add(projectileId);
            projectileSpawnTimesMs.putIfAbsent(projectileId, now);
            if (camPos.squaredDistanceTo(proj.getPos()) > 96.0 * 96.0) continue;
            ItemStack stack = getProjectileStack(proj);
            if (!stack.isEmpty()) {
                PredictionResult prediction = calculatePath(proj.getPos(), proj.getVelocity(), stack.copy(), true, FLYING_MAX_TICKS, projectileId);
                tickPredictions.add(prediction);
            }
        }

        pruneProjectileState();

        ItemStack mainHand = mc.player.getMainHandStack();
        ItemStack offHand = mc.player.getOffHandStack();
        ItemStack held = isThrowable(mainHand) ? mainHand : (isThrowable(offHand) ? offHand : ItemStack.EMPTY);

        if (!held.isEmpty()) {
            PredictionStart start = getHeldPredictionStart(held, camPos, tickDelta);
            if (start != null) {
                framePrediction = calculatePath(start.pos, start.vel, held, false, HELD_MAX_TICKS, -1);
            }
        }

        updateImpactLabels();

        List<PredictionResult> allToRender = new ArrayList<>(tickPredictions);
        if (framePrediction != null) allToRender.add(framePrediction);

        if (allToRender.isEmpty()) return;

        MatrixStack matrices = context.matrixStack();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.depthMask(false);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        drawHeldPredictionLine(matrices, matrix, camPos);
        drawFlyingPredictions(matrices, matrix, camPos, allToRender);

        if (framePrediction != null && framePrediction.hit) {
            Vec3d normal = framePrediction.hitNormal;
            if (normal == null || normal.lengthSquared() < 1.0E-6) {
                normal = new Vec3d(0.0, 1.0, 0.0);
            }
            drawLandingCircle(context.matrixStack(), camPos, framePrediction.hitPos, normal.normalize());
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void drawFlyingPredictions(MatrixStack matrices, Matrix4f matrix, Vec3d camPos, List<PredictionResult> allToRender) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        boolean wroteAny = false;

        for (PredictionResult res : allToRender) {
            if (!res.isFlying) continue;

            float r = 1.0f;
            float g = res.hitEntity ? 0.3f : 1.0f;
            float b = res.hitEntity ? 0.3f : 1.0f;
            float a = 0.85f;

            wroteAny |= drawPredictionTube(buffer, matrix, camPos, res, r, g, b, a);
        }
        var builtBuffer = buffer.endNullable();
        if (wroteAny && builtBuffer != null) {
            BufferRenderer.drawWithGlobalProgram(builtBuffer);
        }
    }

    private void drawHeldPredictionLine(MatrixStack matrices, Matrix4f matrix, Vec3d camPos) {
        if (framePrediction == null || framePrediction.path.size() < 2) return;

        RenderSystem.lineWidth(HELD_LINE_WIDTH);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < framePrediction.path.size() - 1; i++) {
            Vec3d p1 = framePrediction.path.get(i);
            Vec3d p2 = framePrediction.path.get(i + 1);
            buffer.vertex(matrix, (float) (p1.x - camPos.x), (float) (p1.y - camPos.y), (float) (p1.z - camPos.z)).color(1.0f, 1.0f, 1.0f, 0.48f);
            buffer.vertex(matrix, (float) (p2.x - camPos.x), (float) (p2.y - camPos.y), (float) (p2.z - camPos.z)).color(1.0f, 1.0f, 1.0f, 0.48f);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.lineWidth(1.0f);
    }

    public static void renderOverlay2D(DrawContext ctx, float tickDelta) {
        if (INSTANCE == null || !INSTANCE.isEnabled() || !INSTANCE.isInGame()) return;
        INSTANCE.render2D(ctx, tickDelta);
    }

    private void render2D(DrawContext ctx, float tickDelta) {
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = stableCameraPos(tickDelta);
        long now = System.currentTimeMillis();

        Iterator<Map.Entry<Integer, ImpactLabel>> it = impactLabels.entrySet().iterator();
        while (it.hasNext()) {
            ImpactLabel label = it.next().getValue();
            if (!activeProjectileIds.contains(label.sourceId)) {
                it.remove();
                continue;
            }
            if (now - label.thrownAtMs > LABEL_TTL_MS) {
                it.remove();
                continue;
            }

            Vec3d pos2d = projectToScreen(label.hitPos.x, label.hitPos.y + 0.3, label.hitPos.z, camera, camPos, tickDelta);
            if (pos2d != null) {
                drawItemLabel(ctx, label.stack, snapHalf((float) pos2d.x), snapHalf((float) pos2d.y));
            }
        }
    }

    private PredictionResult calculatePath(Vec3d startPos, Vec3d startVel, ItemStack stack, boolean isFlying, int maxTicks, int sourceId) {
        List<Vec3d> path = new ArrayList<>();
        Vec3d pos = startPos;
        Vec3d vel = startVel;
        path.add(pos);

        float gravity = getGravity(stack.getItem());
        float drag = getDrag(stack.getItem());
        boolean hit = false;
        boolean hitEntity = false;
        Vec3d hitPos = pos;
        Vec3d hitNormal = new Vec3d(0.0, 1.0, 0.0);

        for (int tick = 0; tick < maxTicks; tick++) {
            Vec3d nextPos = pos.add(vel);

            RaycastContext context = new RaycastContext(pos, nextPos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
            BlockHitResult blockHit = mc.world.raycast(context);
            if (blockHit.getType() != HitResult.Type.MISS) {
                hitPos = blockHit.getPos();
                hitNormal = Vec3d.of(blockHit.getSide().getVector());
                path.add(hitPos);
                hit = true;
                break;
            }

            Box box = new Box(pos, nextPos).expand(0.5);
            Vec3d closestEntityHit = null;
            double closestDistSq = Double.MAX_VALUE;

            for (Entity e : mc.world.getOtherEntities(mc.player, box, entity -> entity.getId() != sourceId && !entity.isSpectator() && entity.canHit())) {
                var entityHit = e.getBoundingBox().expand(0.3).raycast(pos, nextPos);
                if (entityHit.isEmpty()) continue;
                Vec3d hitVec = entityHit.get();
                double distSq = pos.squaredDistanceTo(hitVec);
                if (distSq < closestDistSq) {
                    closestDistSq = distSq;
                    closestEntityHit = hitVec;
                }
            }

            if (closestEntityHit != null) {
                hitPos = closestEntityHit;
                path.add(hitPos);
                hit = true;
                hitEntity = true;
                break;
            }

            pos = nextPos;
            path.add(pos);
            vel = vel.multiply(drag);
            vel = new Vec3d(vel.x, vel.y - gravity, vel.z);
        }

        return new PredictionResult(path, hitPos, hitNormal, stack, hit, hitEntity, isFlying, sourceId);
    }

    private void drawItemLabel(DrawContext ctx, ItemStack stack, float x, float y) {
        x = snapHalf(x);
        y = snapHalf(y);
        String name = stack.getName().getString();

        float nameSize = 5.8f;
        float iconSize = 9.5f;
        float gap = 2.5f;
        float height = 13f;
        float paddingX = 5.5f;

        float nameWidth = CustomFontRenderer.getTextWidth(name, nameSize, FontWeight.SEMIBOLD);
        float totalWidth = iconSize + gap + nameWidth;

        float absStartX = x - (totalWidth / 2f);
        float absStartY = y - (height / 2f);

        float panelX = snapHalf(absStartX - paddingX);
        float panelY = snapHalf(absStartY);
        float panelW = totalWidth + (paddingX * 2);
        RenderUtil.drawShadow(ctx, panelX, panelY, panelW, height, 3.5f, 7.0f, RenderUtil.withAlpha(0x000000, 0.24f));
        GlassUtil.drawGlassPanel(ctx, panelX, panelY, panelW, height, 3.5f, 0x000000, 0.6f, 1.0f);

        float startX = -(totalWidth / 2f);
        float startY = -(height / 2f);

        ctx.getMatrices().push();
        ctx.getMatrices().translate(x, y, 0);

        float iconY = startY + (height - iconSize) / 2f - 0.5f;
        ctx.getMatrices().push();
        ctx.getMatrices().translate(startX, iconY, 0);
        ctx.getMatrices().scale(iconSize / 16f, iconSize / 16f, 1f);
        ctx.drawItem(stack, 0, 0);
        ctx.getMatrices().pop();

        float textY = startY + (height - nameSize) / 2f - 0.5f;
        float textX = startX + iconSize + gap;
        CustomFontRenderer.drawText(ctx, name, textX, textY, nameSize, 0xFFFFFFFF, FontWeight.SEMIBOLD);

        ctx.getMatrices().pop();
    }

    private boolean drawPredictionTube(BufferBuilder buffer, Matrix4f matrix, Vec3d camPos,
                                       PredictionResult result, float r, float g, float b, float a) {
        float radius = result.isFlying ? PREDICT_TUBE_RADIUS * 0.82f : PREDICT_TUBE_RADIUS;
        boolean wroteAny = false;
        for (int i = 0; i < result.path.size() - 1; i++) {
            wroteAny |= addTubeSegment(buffer, matrix, camPos, result.path.get(i), result.path.get(i + 1), radius, r, g, b, a);
        }
        return wroteAny;
    }

    private boolean addTubeSegment(BufferBuilder buffer, Matrix4f matrix, Vec3d camPos, Vec3d p1, Vec3d p2,
                                   float radius, float r, float g, float b, float a) {
        Vec3d dir = p2.subtract(p1);
        if (dir.lengthSquared() < 1.0E-7) return false;

        dir = dir.normalize();
        Vec3d basis = Math.abs(dir.y) > 0.88 ? new Vec3d(1.0, 0.0, 0.0) : new Vec3d(0.0, 1.0, 0.0);
        Vec3d right = dir.crossProduct(basis);
        if (right.lengthSquared() < 1.0E-7) {
            right = new Vec3d(1.0, 0.0, 0.0);
        } else {
            right = right.normalize();
        }
        Vec3d up = right.crossProduct(dir).normalize();

        for (int side = 0; side < PREDICT_TUBE_SIDES; side++) {
            double a1 = (Math.PI * 2.0 * side) / PREDICT_TUBE_SIDES;
            double a2 = (Math.PI * 2.0 * (side + 1)) / PREDICT_TUBE_SIDES;
            Vec3d o1 = right.multiply(Math.cos(a1) * radius).add(up.multiply(Math.sin(a1) * radius));
            Vec3d o2 = right.multiply(Math.cos(a2) * radius).add(up.multiply(Math.sin(a2) * radius));

            Vec3d v1 = p1.add(o1);
            Vec3d v2 = p2.add(o1);
            Vec3d v3 = p2.add(o2);
            Vec3d v4 = p1.add(o2);

            float shade = 0.70f + 0.30f * (float) ((Math.sin((a1 + a2) * 0.5) + 1.0) * 0.5);
            tubeVertex(buffer, matrix, camPos, v1, r * shade, g * shade, b * shade, a);
            tubeVertex(buffer, matrix, camPos, v2, r * shade, g * shade, b * shade, a);
            tubeVertex(buffer, matrix, camPos, v3, r * shade, g * shade, b * shade, a);
            tubeVertex(buffer, matrix, camPos, v1, r * shade, g * shade, b * shade, a);
            tubeVertex(buffer, matrix, camPos, v3, r * shade, g * shade, b * shade, a);
            tubeVertex(buffer, matrix, camPos, v4, r * shade, g * shade, b * shade, a);
        }
        return true;
    }

    private void tubeVertex(BufferBuilder buffer, Matrix4f matrix, Vec3d camPos, Vec3d pos,
                            float r, float g, float b, float a) {
        buffer.vertex(matrix,
                (float) (pos.x - camPos.x),
                (float) (pos.y - camPos.y),
                (float) (pos.z - camPos.z)
        ).color(r, g, b, a);
    }

    private boolean isThrowable(ItemStack stack) {
        Item i = stack.getItem();
        return i instanceof EnderPearlItem || i instanceof SnowballItem || i instanceof EggItem
                || i instanceof SplashPotionItem || i instanceof LingeringPotionItem || i instanceof ExperienceBottleItem
                || i instanceof BowItem || i instanceof CrossbowItem || i instanceof TridentItem;
    }

    private float getThrowVelocity(ItemStack stack) {
        Item item = stack.getItem();

        if (item instanceof BowItem) {
            if (!mc.player.isUsingItem() || !(mc.player.getActiveItem().getItem() instanceof BowItem)) return 0f;
            int useTicks = mc.player.getItemUseTime();
            if (useTicks <= 0) return 0f;
            float pull = useTicks / 20.0F;
            pull = (pull * pull + pull * 2.0F) / 3.0F;
            if (pull > 1.0F) pull = 1.0F;
            return pull * 3.0F;
        }

        if (item instanceof CrossbowItem) {
            return CrossbowItem.isCharged(stack) ? 3.15f : 0f;
        }

        if (item instanceof TridentItem) {
            return mc.player.isUsingItem() && (mc.player.getActiveItem().getItem() instanceof TridentItem) ? 2.5f : 0f;
        }

        if (item instanceof SplashPotionItem || item instanceof LingeringPotionItem) return 0.5f;
        if (item instanceof ExperienceBottleItem) return 0.7f;
        return 1.5f;
    }

    private float getGravity(Item item) {
        if (item instanceof BowItem || item instanceof CrossbowItem || item instanceof TridentItem || item instanceof SplashPotionItem || item instanceof LingeringPotionItem)
            return 0.05f;
        if (item instanceof ExperienceBottleItem) return 0.07f;
        return 0.03f;
    }

    private float getDrag(Item item) {
        if (item instanceof TridentItem) return 0.99f;
        return 0.99f;
    }

    private PredictionStart getHeldPredictionStart(ItemStack stack, Vec3d cameraPos, float tickDelta) {
        float velocity = getThrowVelocity(stack);
        if (velocity <= 0f) return null;

        float yaw = MathHelper.lerpAngleDegrees(tickDelta, mc.player.prevYaw, mc.player.getYaw());
        float pitch = MathHelper.lerp(tickDelta, mc.player.prevPitch, mc.player.getPitch());
        Vec3d direction = getShootDirection(pitch, yaw, 0.0f);
        Vec3d startPos = cameraPos.add(direction.multiply(0.16));
        Vec3d shotVelocity = direction.multiply(velocity);

        return new PredictionStart(startPos, shotVelocity);
    }

    private void drawLandingCircle(MatrixStack matrices, Vec3d camPos, Vec3d hitPos, Vec3d normal) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder circle = tessellator.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        int segments = 28;
        float radius = 0.18f;
        Vec3d n = normal.lengthSquared() < 1.0E-6 ? new Vec3d(0.0, 1.0, 0.0) : normal.normalize();
        Vec3d origin = hitPos.subtract(camPos).add(n.multiply(0.02));

        Vec3d tangent = Math.abs(n.y) > 0.9 ? new Vec3d(1.0, 0.0, 0.0) : new Vec3d(0.0, 1.0, 0.0);
        Vec3d u = n.crossProduct(tangent).normalize();
        Vec3d v = n.crossProduct(u).normalize();

        float cx = (float) origin.x;
        float cy = (float) origin.y;
        float cz = (float) origin.z;

        for (int i = 0; i < segments; i++) {
            double a1 = (Math.PI * 2.0 * i) / segments;
            double a2 = (Math.PI * 2.0 * (i + 1)) / segments;
            Vec3d p1 = origin.add(u.multiply(Math.cos(a1) * radius)).add(v.multiply(Math.sin(a1) * radius));
            Vec3d p2 = origin.add(u.multiply(Math.cos(a2) * radius)).add(v.multiply(Math.sin(a2) * radius));

            circle.vertex(matrices.peek().getPositionMatrix(), cx, cy, cz).color(1.0f, 0.1f, 0.1f, 0.52f);
            circle.vertex(matrices.peek().getPositionMatrix(), (float) p1.x, (float) p1.y, (float) p1.z).color(1.0f, 0.1f, 0.1f, 0.52f);
            circle.vertex(matrices.peek().getPositionMatrix(), (float) p2.x, (float) p2.y, (float) p2.z).color(1.0f, 0.1f, 0.1f, 0.52f);
        }

        BufferRenderer.drawWithGlobalProgram(circle.end());
    }

    private void updateImpactLabels() {
        long now = System.currentTimeMillis();

        for (PredictionResult res : tickPredictions) {
            if (!res.isFlying || !res.hit || res.sourceId < 0) continue;

            long thrownAtMs = projectileSpawnTimesMs.getOrDefault(res.sourceId, now);
            ImpactLabel label = impactLabels.get(res.sourceId);
            if (label == null) {
                impactLabels.put(res.sourceId, new ImpactLabel(res.sourceId, res.stack.copy(), res.hitPos, thrownAtMs));
                continue;
            }

            if (label.hitPos.squaredDistanceTo(res.hitPos) > 9.0) {
                label.hitPos = res.hitPos;
            } else {
                label.hitPos = label.hitPos.lerp(res.hitPos, LABEL_HIT_SMOOTH);
            }
            label.stack = res.stack.copy();
        }
    }

    private void pruneProjectileState() {
        projectileSpawnTimesMs.keySet().removeIf(id -> !activeProjectileIds.contains(id));
        impactLabels.keySet().removeIf(id -> !activeProjectileIds.contains(id));
    }

    private void removeProjectileState(int projectileId) {
        projectileSpawnTimesMs.remove(projectileId);
        impactLabels.remove(projectileId);
    }

    private boolean isProjectileFinished(ProjectileEntity projectile) {
        if (projectile.isRemoved() || !projectile.isAlive()) return true;
        if (projectile.age <= 2) return false;
        if (projectile.horizontalCollision || projectile.verticalCollision || projectile.groundCollision) {
            return true;
        }
        if (projectile instanceof PersistentProjectileEntity persistent
                && ((PersistentProjectileEntityAccessor) persistent).enough$isInGround()) {
            return true;
        }

        double speedSq = projectile.getVelocity().lengthSquared();
        if (projectile.isOnGround() && speedSq < 0.01) return true;
        return projectile.age > 10 && speedSq < 0.0004;
    }

    private Vec3d getShootDirection(float pitch, float yaw, float roll) {
        float rad = (float) (Math.PI / 180.0);
        float x = -MathHelper.sin(yaw * rad) * MathHelper.cos(pitch * rad);
        float y = -MathHelper.sin((pitch + roll) * rad);
        float z = MathHelper.cos(yaw * rad) * MathHelper.cos(pitch * rad);
        return new Vec3d(x, y, z).normalize();
    }

    private ItemStack getProjectileStack(ProjectileEntity proj) {
        if (proj instanceof PotionEntity potion) return potion.getStack();
        if (proj instanceof ExperienceBottleEntity) return new ItemStack(Items.EXPERIENCE_BOTTLE);
        if (proj.getClass().getSimpleName().contains("EnderPearl")) return new ItemStack(Items.ENDER_PEARL);
        if (proj.getClass().getSimpleName().contains("Snowball")) return new ItemStack(Items.SNOWBALL);
        if (proj.getClass().getSimpleName().contains("Egg")) return new ItemStack(Items.EGG);
        if (proj.getClass().getSimpleName().contains("Arrow")) return new ItemStack(Items.ARROW);
        if (proj.getClass().getSimpleName().contains("Trident")) return new ItemStack(Items.TRIDENT);
        return ItemStack.EMPTY;
    }

    private static Vec3d projectToScreen(double x, double y, double z, Camera camera, Vec3d camPos, float tickDelta) {
        Vector4f pos = new Vector4f((float) (x - camPos.x), (float) (y - camPos.y), (float) (z - camPos.z), 1.0f);

        Quaternionf rotation = new Quaternionf(camera.getRotation()).conjugate();
        pos.rotate(rotation);

        if (pos.z >= 0) return null;

        GameRendererAccessor gameRendererAccessor = (GameRendererAccessor) mc.gameRenderer;
        double fov = gameRendererAccessor.invokeGetFov(camera, tickDelta, true);
        float halfFovTan = (float) Math.tan(Math.toRadians(fov) / 2.0);

        float screenWidth = ScreenScaleUtil.virtualWidth();
        float screenHeight = ScreenScaleUtil.virtualHeight();

        float scale = (screenHeight / 2.0f) / halfFovTan;

        float screenX = screenWidth / 2.0f + (pos.x / -pos.z) * scale;
        float screenY = screenHeight / 2.0f - (pos.y / -pos.z) * scale;

        if (screenX < -500 || screenX > screenWidth + 500 || screenY < -500 || screenY > screenHeight + 500) return null;

        return new Vec3d(screenX, screenY, 0);
    }

    private static Vec3d stableCameraPos(float tickDelta) {
        if (mc.player != null) {
            return mc.player.getCameraPosVec(tickDelta);
        }
        Camera camera = mc.gameRenderer == null ? null : mc.gameRenderer.getCamera();
        return camera == null ? Vec3d.ZERO : camera.getPos();
    }

    private static float snapHalf(float value) {
        return Math.round(value * 2.0f) * 0.5f;
    }

    private static class ImpactLabel {
        private final int sourceId;
        private ItemStack stack;
        private Vec3d hitPos;
        private final long thrownAtMs;

        private ImpactLabel(int sourceId, ItemStack stack, Vec3d hitPos, long thrownAtMs) {
            this.sourceId = sourceId;
            this.stack = stack;
            this.hitPos = hitPos;
            this.thrownAtMs = thrownAtMs;
        }
    }

    private record PredictionStart(Vec3d pos, Vec3d vel) {
    }

    private static class PredictionResult {
        final List<Vec3d> path;
        final Vec3d hitPos;
        final Vec3d hitNormal;
        final ItemStack stack;
        final boolean hit;
        final boolean hitEntity;
        final boolean isFlying;
        final int sourceId;

        PredictionResult(List<Vec3d> path, Vec3d hitPos, Vec3d hitNormal, ItemStack stack, boolean hit, boolean hitEntity,
                         boolean isFlying, int sourceId) {
            this.path = path;
            this.hitPos = hitPos;
            this.hitNormal = hitNormal;
            this.stack = stack;
            this.hit = hit;
            this.hitEntity = hitEntity;
            this.isFlying = isFlying;
            this.sourceId = sourceId;
        }
    }
}
