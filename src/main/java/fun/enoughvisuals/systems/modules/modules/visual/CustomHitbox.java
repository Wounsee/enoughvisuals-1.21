package fun.enoughvisuals.systems.modules.modules.visual;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.ColorSetting;
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
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

@Register(name = "HitBox Customizer", id = "custom_hitbox", description = "Настраивает отображение хитбоксов сущностей", category = Category.VISUAL)
public class CustomHitbox extends Module {

    private static CustomHitbox INSTANCE;

    public final BooleanSetting playersOnly = add(new BooleanSetting("Только игроки", "players_only", "Рисовать только игроков", false));
    public final ColorSetting hitboxColor = add(new ColorSetting("Цвет хитбокса", "hitbox_color", "Цвет линий хитбокса", 0xFFFFFFFF));
    public final ColorSetting reachColor = add(new ColorSetting("Цвет при наведении", "reach_color", "Цвет, когда цель в радиусе атаки под прицелом", 0xFFFF3030));
    public final SliderSetting outlineThickness = add(new SliderSetting("Толщина обводки", "outline_thickness", "Толщина линий хитбокса", 1.0, 1.0, 6.0, 0.5));
    public final BooleanSetting showLookLine = add(new BooleanSetting("Линия взгляда", "show_look_line", "Показывать направление взгляда существа", true));
    public final BooleanSetting redInReach = add(new BooleanSetting("Подсветка в радиусе", "red_in_reach", "Менять цвет на красный в радиусе удара", false));

    public CustomHitbox() {
        INSTANCE = this;
        reachColor.visibleWhen(redInReach::get);
    }

    public static void renderInWorld(WorldRenderContext context) {
        if (INSTANCE == null || !INSTANCE.isEnabled()) return;
        if (mc.player == null || mc.world == null) return;
        INSTANCE.renderHitboxes(context);
    }

    private void renderHitboxes(WorldRenderContext context) {
        if (context.matrixStack() == null || context.camera() == null) return;

        float tickDelta = context.tickCounter() != null ? context.tickCounter().getTickDelta(true) : 1.0f;
        Vec3d camPos = context.camera().getPos();
        Matrix4f matrix = context.matrixStack().peek().getPositionMatrix();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.enableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        float thickness = outlineThickness.get().floatValue();
        double viewDistanceChunks = mc.options.getViewDistance().getValue();
        double maxDist = Math.max(24.0, viewDistanceChunks * 16.0 + 8.0);
        double maxDistSq = maxDist * maxDist;

        Box searchBox = mc.player.getBoundingBox().expand(maxDist + 2.0);
        for (Entity entity : mc.world.getOtherEntities(mc.player, searchBox, this::canConsiderEntity)) {
            Box box = interpolateBox(entity, tickDelta);
            if (camPos.squaredDistanceTo(box.getCenter()) > maxDistSq) continue;

            int color = hitboxColor.get();
            if (redInReach.get() && isCrosshairTarget(entity)) {
                color = reachColor.get();
            }

            drawOutlineBox(matrix, camPos, box, color, thickness);

            if (showLookLine.get()) {
                Vec3d eyePos = new Vec3d(entity.getX(), entity.getStandingEyeHeight() + entity.getY(), entity.getZ());
                Vec3d lookVec = entity.getRotationVec(tickDelta).multiply(2.0);
                drawVector(matrix, camPos, eyePos, lookVec, color, thickness);
            }
        }

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

        return !entity.isInvisible();
    }

    private boolean isCrosshairTarget(Entity entity) {
        if (mc.crosshairTarget instanceof EntityHitResult ehr && ehr.getEntity() == entity) {
            double reach = 3.0;
            try {
                reach = mc.player.getEntityInteractionRange();
            } catch (Throwable ignored) {}
            return mc.player.getCameraPosVec(1.0f).squaredDistanceTo(ehr.getPos()) <= reach * reach;
        }
        return false;
    }

    private Box interpolateBox(Entity entity, float tickDelta) {
        double x = MathHelper.lerp(tickDelta, entity.prevX, entity.getX());
        double y = MathHelper.lerp(tickDelta, entity.prevY, entity.getY());
        double z = MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ());
        Box box = entity.getBoundingBox();
        double w = box.getLengthX() / 2.0;
        double h = box.getLengthY();
        double d = box.getLengthZ() / 2.0;
        return new Box(x - w, y, z - d, x + w, y + h, z + d);
    }

    private void drawOutlineBox(Matrix4f matrix, Vec3d cam, Box box, int color, float width) {
        float[] c = rgba(color);
        if (c[3] <= 0.002f) return;

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

    private void drawVector(Matrix4f matrix, Vec3d cam, Vec3d start, Vec3d dir, int color, float thickness) {
        float[] c = rgba(color);
        if (c[3] <= 0.002f) return;

        RenderSystem.lineWidth(Math.max(1.0f, thickness));
        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);

        float x1 = (float) (start.x - cam.x);
        float y1 = (float) (start.y - cam.y);
        float z1 = (float) (start.z - cam.z);

        float x2 = (float) (start.x + dir.x - cam.x);
        float y2 = (float) (start.y + dir.y - cam.y);
        float z2 = (float) (start.z + dir.z - cam.z);

        bb.vertex(matrix, x1, y1, z1).color(c[0], c[1], c[2], c[3]);
        bb.vertex(matrix, x2, y2, z2).color(c[0], c[1], c[2], c[3]);

        BufferRenderer.drawWithGlobalProgram(bb.end());
    }

    private void line(BufferBuilder bb, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float[] c) {
        bb.vertex(matrix, x1, y1, z1).color(c[0], c[1], c[2], c[3]);
        bb.vertex(matrix, x2, y2, z2).color(c[0], c[1], c[2], c[3]);
    }

    private float[] rgba(int color) {
        return new float[]{
                ((color >> 16) & 0xFF) / 255f,
                ((color >> 8) & 0xFF) / 255f,
                (color & 0xFF) / 255f,
                ((color >> 24) & 0xFF) / 255f
        };
    }
}
