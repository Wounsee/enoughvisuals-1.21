package fun.enoughvisuals.systems.modules.modules.visual;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.ColorSetting;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Register(name = "HitBox Customizer", id = "custom_hitbox", description = "Настраивает отображение хитбоксов сущностей", category = Category.VISUAL)
public class CustomHitbox extends Module {

    private static final long VISIBILITY_CACHE_MS = 70L;
    private static final float MIN_ALPHA = 0.002f;
    private static final int FIXED_FILL_ALPHA = 0x80; // ~50%

    private static CustomHitbox INSTANCE;

    private final Map<Integer, Float> hoverProgress = new HashMap<>();
    private final Map<Integer, VisibilitySnapshot> visibilityCache = new HashMap<>();

    public final ModeSetting style = add(new ModeSetting(
            "Стиль",
            "style",
            "Вид хитбокса",
            "Бокс + Заливка",
            "Бокс + Заливка",
            "Бокс",
            "Заливка"
    ));

    public final SliderSetting lineWidth = add(new SliderSetting(
            "Толщина",
            "line_width",
            "Толщина линий",
            2.4,
            1.0,
            8.0,
            0.1
    ));
    public final SliderSetting cornerScale = add(new SliderSetting(
            "Размер уголков",
            "corner_scale",
            "Длина сегментов уголков",
            0.28,
            0.05,
            0.8,
            0.01
    ));

    public final BooleanSetting playersOnly = add(new BooleanSetting(
            "Только игроки",
            "players_only",
            "Рисовать только игроков",
            false
    ));

    public final BooleanSetting hoverHighlight = add(new BooleanSetting(
            "Подсветка при наведении",
            "hover_highlight",
            "Подсвечивать цель под прицелом",
            true
    ));
    public final SliderSetting hoverSmooth = add(new SliderSetting(
            "Плавность наведения",
            "hover_smooth",
            "Скорость анимации подсветки",
            0.28,
            0.05,
            0.7,
            0.01
    ));

    public final ColorSetting outlineColor = add(new ColorSetting("Обводка", "outline_color", "Цвет обводки", 0xFF68D8FF));
    public final ColorSetting fillColor = add(new ColorSetting("Заливка", "fill_color", "Цвет заливки", 0x3568D8FF));
    public final ColorSetting cornerColor = add(new ColorSetting("Уголки", "corner_color", "Цвет уголков", 0xFFBCEAFF));
    public final ColorSetting hoverOutlineColor = add(new ColorSetting("Наводка обводка", "hover_outline_color", "Обводка при наведении", 0xFFFF3B30));
    public final ColorSetting hoverFillColor = add(new ColorSetting("Наводка заливка", "hover_fill_color", "Заливка при наведении", 0x55FF3B30));

    public CustomHitbox() {
        INSTANCE = this;

        lineWidth.visibleWhen(this::usesLineDrawing);
        cornerScale.visibleWhen(this::drawsCorners);
        fillColor.visibleWhen(this::usesFill);
        outlineColor.visibleWhen(this::drawsOutline);
        cornerColor.visibleWhen(this::drawsCorners);
        hoverSmooth.visibleWhen(hoverHighlight::get);
        hoverOutlineColor.visibleWhen(() -> hoverHighlight.get() && usesLineDrawing());
        hoverFillColor.visibleWhen(() -> hoverHighlight.get() && usesFill());
    }

    public static void renderInWorld(WorldRenderContext context) {
        if (INSTANCE == null || !INSTANCE.isEnabled()) return;
        if (mc.player == null || mc.world == null) return;
        INSTANCE.renderHitboxes(context);
    }

    @Override
    protected void onDisable() {
        hoverProgress.clear();
        visibilityCache.clear();
    }

    private void renderHitboxes(WorldRenderContext context) {
        if (context.matrixStack() == null || context.camera() == null) return;

        float tickDelta = context.tickCounter() != null ? context.tickCounter().getTickDelta(true) : 1.0f;
        long now = System.currentTimeMillis();
        Vec3d camPos = context.camera().getPos();
        Matrix4f matrix = context.matrixStack().peek().getPositionMatrix();

        boolean drawCorners = drawsCorners();
        boolean drawOutline = drawsOutline();
        boolean drawFill = drawsFill();

        double maxVisibleDistance = getMaxVisibleDistance();
        double maxVisibleDistanceSq = maxVisibleDistance * maxVisibleDistance;
        int hoveredEntityId = resolveHoveredEntityId(tickDelta, now);
        Set<Integer> activeEntityIds = new HashSet<>();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.enableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        float width = lineWidth.get().floatValue();
        float corner = cornerScale.get().floatValue();

        Box searchBox = mc.player.getBoundingBox().expand(maxVisibleDistance + 2.0);
        for (Entity entity : mc.world.getOtherEntities(mc.player, searchBox, this::canConsiderEntity)) {
            int entityId = entity.getId();
            activeEntityIds.add(entityId);

            Box box = interpolateBox(entity, tickDelta);
            if (camPos.squaredDistanceTo(box.getCenter()) > maxVisibleDistanceSq) continue;
            if (!hasLineOfSightCached(entity, box, now)) continue;

            float hover = updateHoverProgress(entityId, hoverHighlight.get() && entityId == hoveredEntityId);
            boolean forceHover = hover > 0.01f;

            int useOutline = blendColor(outlineColor.get(), hoverOutlineColor.get(), hover);
            int useFill = withFixedFillAlpha(blendColor(fillColor.get(), hoverFillColor.get(), hover));
            int useCorner = blendColor(cornerColor.get(), hoverOutlineColor.get(), hover);

            if (drawFill || forceHover) {
                drawFilledBox(matrix, camPos, box, useFill);
            }
            if (drawOutline || forceHover) {
                drawOutlineBox(matrix, camPos, box, useOutline, width);
            }
            if (drawCorners) {
                drawCornerBox(matrix, camPos, box, useCorner, width, corner);
            }
        }

        cleanupState(activeEntityIds);

        RenderSystem.lineWidth(1.0f);
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private boolean canConsiderEntity(Entity entity) {
        if (entity == null || entity.isRemoved()) return false;
        if (entity == mc.player) return false;
        if (playersOnly.get() && !(entity instanceof PlayerEntity)) return false;

        if (entity instanceof LivingEntity livingEntity && !livingEntity.isAlive()) {
            return false;
        }

        if (entity.isInvisible()) {
            if (!(entity instanceof LivingEntity living)) return false;
            return isWearingFullHighTierArmor(living);
        }

        return true;
    }

    private int resolveHoveredEntityId(float tickDelta, long now) {
        if (!hoverHighlight.get()) return -1;
        if (!(mc.crosshairTarget instanceof EntityHitResult entityHitResult)) return -1;

        Entity entity = entityHitResult.getEntity();
        if (!canConsiderEntity(entity)) return -1;

        double reach = getHoverReachDistance();
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        if (cameraPos.squaredDistanceTo(entityHitResult.getPos()) > reach * reach) return -1;

        if (!hasLineOfSightCached(entity, interpolateBox(entity, tickDelta), now)) return -1;
        return entity.getId();
    }

    private double getMaxVisibleDistance() {
        int viewDistanceChunks = mc.options.getViewDistance().getValue();
        return Math.max(24.0, viewDistanceChunks * 16.0 + 8.0);
    }

    private double getHoverReachDistance() {
        try {
            return mc.player.getEntityInteractionRange();
        } catch (Throwable ignored) {
            return 3.0;
        }
    }

    private float updateHoverProgress(int entityId, boolean hovered) {
        float current = hoverProgress.getOrDefault(entityId, 0.0f);
        float target = hovered ? 1.0f : 0.0f;
        float speed = MathHelper.clamp(hoverSmooth.get().floatValue(), 0.03f, 0.95f);
        float next = current + (target - current) * speed;

        if (next < 0.001f && !hovered) {
            hoverProgress.remove(entityId);
            return 0.0f;
        }

        hoverProgress.put(entityId, next);
        return next;
    }

    private void cleanupState(Set<Integer> activeEntityIds) {
        hoverProgress.entrySet().removeIf(entry -> !activeEntityIds.contains(entry.getKey()) && entry.getValue() < 0.01f);
        visibilityCache.entrySet().removeIf(entry -> !activeEntityIds.contains(entry.getKey()));
    }

    private boolean hasLineOfSightCached(Entity entity, Box box, long now) {
        VisibilitySnapshot snapshot = visibilityCache.get(entity.getId());
        if (snapshot != null && now - snapshot.checkedAtMs <= VISIBILITY_CACHE_MS) {
            return snapshot.visible;
        }

        boolean visible = hasLineOfSight(box);
        visibilityCache.put(entity.getId(), new VisibilitySnapshot(visible, now));
        return visible;
    }

    private boolean hasLineOfSight(Box box) {
        Vec3d eye = mc.player.getCameraPosVec(1.0f);
        Vec3d center = box.getCenter();
        if (isPointVisible(eye, center)) return true;

        Vec3d top = new Vec3d(center.x, box.maxY - 0.05, center.z);
        if (isPointVisible(eye, top)) return true;

        Vec3d bottom = new Vec3d(center.x, box.minY + 0.05, center.z);
        if (isPointVisible(eye, bottom)) return true;

        Vec3d nearX = new Vec3d(box.minX + 0.08, center.y, center.z);
        if (isPointVisible(eye, nearX)) return true;

        Vec3d farX = new Vec3d(box.maxX - 0.08, center.y, center.z);
        if (isPointVisible(eye, farX)) return true;

        Vec3d nearZ = new Vec3d(center.x, center.y, box.minZ + 0.08);
        if (isPointVisible(eye, nearZ)) return true;

        Vec3d farZ = new Vec3d(center.x, center.y, box.maxZ - 0.08);
        return isPointVisible(eye, farZ);
    }

    private boolean isPointVisible(Vec3d eye, Vec3d point) {
        RaycastContext context = new RaycastContext(
                eye,
                point,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        );
        BlockHitResult hitResult = mc.world.raycast(context);
        if (hitResult.getType() == HitResult.Type.MISS) return true;

        double hitDistanceSq = eye.squaredDistanceTo(hitResult.getPos());
        double pointDistanceSq = eye.squaredDistanceTo(point);
        return hitDistanceSq + 1.0E-4 >= pointDistanceSq;
    }

    private Box interpolateBox(Entity entity, float tickDelta) {
        Box base = entity.getBoundingBox();
        double x = MathHelper.lerp(tickDelta, entity.prevX, entity.getX());
        double y = MathHelper.lerp(tickDelta, entity.prevY, entity.getY());
        double z = MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ());
        return base.offset(x - entity.getX(), y - entity.getY(), z - entity.getZ());
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

    private boolean drawsCorners() {
        return style.is("Уголки") || style.is("Уголки + Заливка");
    }

    private boolean drawsOutline() {
        return style.is("Бокс") || style.is("Бокс + Заливка");
    }

    private boolean drawsFill() {
        return style.is("Заливка") || style.is("Бокс + Заливка") || style.is("Уголки + Заливка");
    }

    private boolean usesLineDrawing() {
        return drawsOutline() || drawsCorners() || hoverHighlight.get();
    }

    private boolean usesFill() {
        return drawsFill() || hoverHighlight.get();
    }

    private void drawFilledBox(Matrix4f matrix, Vec3d cam, Box box, int color) {
        float[] c = rgba(color);
        if (c[3] <= MIN_ALPHA) return;

        float x1 = (float) (box.minX - cam.x);
        float y1 = (float) (box.minY - cam.y);
        float z1 = (float) (box.minZ - cam.z);
        float x2 = (float) (box.maxX - cam.x);
        float y2 = (float) (box.maxY - cam.y);
        float z2 = (float) (box.maxZ - cam.z);

        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        quad(bb, matrix, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, c);
        quad(bb, matrix, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, c);
        quad(bb, matrix, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, c);
        quad(bb, matrix, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, c);
        quad(bb, matrix, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, c);
        quad(bb, matrix, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, c);

        BufferRenderer.drawWithGlobalProgram(bb.end());
    }

    private void drawOutlineBox(Matrix4f matrix, Vec3d cam, Box box, int color, float width) {
        float[] c = rgba(color);
        if (c[3] <= MIN_ALPHA) return;

        float x1 = (float) (box.minX - cam.x);
        float y1 = (float) (box.minY - cam.y);
        float z1 = (float) (box.minZ - cam.z);
        float x2 = (float) (box.maxX - cam.x);
        float y2 = (float) (box.maxY - cam.y);
        float z2 = (float) (box.maxZ - cam.z);

        RenderSystem.lineWidth(Math.max(1.0f, width));
        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);

        line(bb, matrix, x1, y1, z1, x2, y1, z1, c);
        line(bb, matrix, x2, y1, z1, x2, y1, z2, c);
        line(bb, matrix, x2, y1, z2, x1, y1, z2, c);
        line(bb, matrix, x1, y1, z2, x1, y1, z1, c);

        line(bb, matrix, x1, y2, z1, x2, y2, z1, c);
        line(bb, matrix, x2, y2, z1, x2, y2, z2, c);
        line(bb, matrix, x2, y2, z2, x1, y2, z2, c);
        line(bb, matrix, x1, y2, z2, x1, y2, z1, c);

        line(bb, matrix, x1, y1, z1, x1, y2, z1, c);
        line(bb, matrix, x2, y1, z1, x2, y2, z1, c);
        line(bb, matrix, x2, y1, z2, x2, y2, z2, c);
        line(bb, matrix, x1, y1, z2, x1, y2, z2, c);

        BufferRenderer.drawWithGlobalProgram(bb.end());
    }

    private void drawCornerBox(Matrix4f matrix, Vec3d cam, Box box, int color, float width, float cornerScaleValue) {
        float[] c = rgba(color);
        if (c[3] <= MIN_ALPHA) return;

        double dx = box.getLengthX();
        double dy = box.getLengthY();
        double dz = box.getLengthZ();

        float len = (float) (Math.min(Math.min(dx, dy), dz) * cornerScaleValue);
        len = Math.max(0.01f, len);
        len = Math.min(len, (float) (dx * 0.5));
        len = Math.min(len, (float) (dy * 0.5));
        len = Math.min(len, (float) (dz * 0.5));

        float x1 = (float) (box.minX - cam.x);
        float y1 = (float) (box.minY - cam.y);
        float z1 = (float) (box.minZ - cam.z);
        float x2 = (float) (box.maxX - cam.x);
        float y2 = (float) (box.maxY - cam.y);
        float z2 = (float) (box.maxZ - cam.z);

        RenderSystem.lineWidth(Math.max(1.0f, width));
        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);

        corner(bb, matrix, x1, y1, z1, 1, 1, 1, len, c);
        corner(bb, matrix, x2, y1, z1, -1, 1, 1, len, c);
        corner(bb, matrix, x2, y1, z2, -1, 1, -1, len, c);
        corner(bb, matrix, x1, y1, z2, 1, 1, -1, len, c);

        corner(bb, matrix, x1, y2, z1, 1, -1, 1, len, c);
        corner(bb, matrix, x2, y2, z1, -1, -1, 1, len, c);
        corner(bb, matrix, x2, y2, z2, -1, -1, -1, len, c);
        corner(bb, matrix, x1, y2, z2, 1, -1, -1, len, c);

        BufferRenderer.drawWithGlobalProgram(bb.end());
    }

    private void corner(BufferBuilder bb, Matrix4f matrix, float x, float y, float z, int sx, int sy, int sz, float len, float[] color) {
        line(bb, matrix, x, y, z, x + len * sx, y, z, color);
        line(bb, matrix, x, y, z, x, y + len * sy, z, color);
        line(bb, matrix, x, y, z, x, y, z + len * sz, color);
    }

    private void quad(BufferBuilder bb, Matrix4f matrix,
                      float x1, float y1, float z1,
                      float x2, float y2, float z2,
                      float x3, float y3, float z3,
                      float x4, float y4, float z4,
                      float[] c) {
        bb.vertex(matrix, x1, y1, z1).color(c[0], c[1], c[2], c[3]);
        bb.vertex(matrix, x2, y2, z2).color(c[0], c[1], c[2], c[3]);
        bb.vertex(matrix, x3, y3, z3).color(c[0], c[1], c[2], c[3]);
        bb.vertex(matrix, x4, y4, z4).color(c[0], c[1], c[2], c[3]);
    }

    private void line(BufferBuilder bb, Matrix4f matrix,
                      float x1, float y1, float z1,
                      float x2, float y2, float z2,
                      float[] c) {
        bb.vertex(matrix, x1, y1, z1).color(c[0], c[1], c[2], c[3]);
        bb.vertex(matrix, x2, y2, z2).color(c[0], c[1], c[2], c[3]);
    }

    private int blendColor(int base, int target, float mix) {
        float t = MathHelper.clamp(mix, 0.0f, 1.0f);

        int a1 = (base >> 24) & 0xFF;
        int r1 = (base >> 16) & 0xFF;
        int g1 = (base >> 8) & 0xFF;
        int b1 = base & 0xFF;

        int a2 = (target >> 24) & 0xFF;
        int r2 = (target >> 16) & 0xFF;
        int g2 = (target >> 8) & 0xFF;
        int b2 = target & 0xFF;

        int a = (int) (a1 + (a2 - a1) * t);
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private int withFixedFillAlpha(int color) {
        return (FIXED_FILL_ALPHA << 24) | (color & 0x00FFFFFF);
    }

    private float[] rgba(int color) {
        return new float[]{
                ((color >> 16) & 0xFF) / 255f,
                ((color >> 8) & 0xFF) / 255f,
                (color & 0xFF) / 255f,
                ((color >> 24) & 0xFF) / 255f
        };
    }

    private record VisibilitySnapshot(boolean visible, long checkedAtMs) {
    }
}
