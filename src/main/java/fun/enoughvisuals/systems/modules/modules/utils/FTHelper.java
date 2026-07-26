package fun.enoughvisuals.systems.modules.modules.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.mixins.accessors.GameRendererAccessor;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.ColorSetting;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.render.blur.GlassUtil;
import fun.enoughvisuals.utils.system.ClientLanguage;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

@Register(name = "FTHelper", id = "ft_helper", description = "Хелпер для сервера FunTime", category = Category.UTILS)
public class FTHelper extends Module {

    private static FTHelper INSTANCE;
    private boolean registered = false;

    // --- Настройки ---
    public final BooleanSetting dustEnabled = add(new BooleanSetting("Явная пыль", "dust_enabled", true));
    public final ColorSetting dustColor = (ColorSetting) add(new ColorSetting("Цвет пыли", "dust_color", "Цвет радиуса явной пыли", 0xFF55FFFF))
            .visibleWhen(dustEnabled::get);
    public final BooleanSetting disorientEnabled = add(new BooleanSetting("Дезориентация", "disorient_enabled", true));
    public final ColorSetting disorientColor = (ColorSetting) add(new ColorSetting("Цвет дезориентации", "disorient_color", "Цвет радиуса дезориентации", 0xFFAA00AA))
            .visibleWhen(disorientEnabled::get);
    public final BooleanSetting godAuraEnabled = add(new BooleanSetting("Божья аура", "god_aura_enabled", true));
    public final ColorSetting godAuraColor = (ColorSetting) add(new ColorSetting("Цвет божьей ауры", "god_aura_color", "Цвет радиуса божьей ауры", 0xFFFFFF55))
            .visibleWhen(godAuraEnabled::get);
    public final BooleanSetting plastEnabled = add(new BooleanSetting("Пласт", "plast_enabled", true));
    public final ColorSetting plastColor = (ColorSetting) add(new ColorSetting("Цвет пласта", "plast_color", "Цвет контура и заливки пласта", 0xFFFFFFFF))
            .visibleWhen(plastEnabled::get);
    public final BooleanSetting freezeEnabled = add(new BooleanSetting("Снежок заморозки", "freeze_enabled", true));
    public final ColorSetting freezeColor = (ColorSetting) add(new ColorSetting("Цвет заморозки", "freeze_color", "Цвет радиуса снежка заморозки", 0xFF55FFFF))
            .visibleWhen(freezeEnabled::get);
    public final ColorSetting playerInsideColor = add(new ColorSetting("Игрок в радиусе", "player_inside_color", "Цвет, когда в зоне есть игрок", 0xFFFF3030));



    public FTHelper() {
        INSTANCE = this;
    }

    @Override
    protected void onEnable() {
        if (!registered) {
            WorldRenderEvents.LAST.register(this::onRender3D);
            HudRenderCallback.EVENT.register(this::onRender2D);
            registered = true;
        }
    }

    @Override
    public void onTick() {
    }

    @Override
    protected void onDisable() {
    }




    // === 2D РЕНДЕР ===
    private void onRender2D(DrawContext ctx, net.minecraft.client.render.RenderTickCounter tickCounter) {
    }

    // === 3D РЕНДЕР (ФОРМЫ И СНЕЖОК) ===
    private void onRender3D(WorldRenderContext context) {
        if (!isEnabled() || !isInGame()) return;

        MatrixStack stack = context.matrixStack();
        Camera camera = context.camera();
        Vec3d camPos = camera.getPos();

        ItemStack handStack = mc.player.getMainHandStack();
        Item heldItem = handStack.getItem();
        String itemName = handStack.getName().getString().toLowerCase();
        BlockPos playerPos = mc.player.getBlockPos();
        float tickDelta = context.tickCounter() == null ? 1.0f : context.tickCounter().getTickDelta(true);
        Vec3d playerRenderPos = interpolatedPos(mc.player, tickDelta);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.lineWidth(2.0f);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        stack.push();
        stack.translate(-camPos.x, -camPos.y, -camPos.z);



        // 2. Превью Пласта
        if (plastEnabled.get() && heldItem == Items.DRIED_KELP) {
            drawPlast(stack, playerPos, tickDelta);
        }

        // 3. Статичные Круги
        if (dustEnabled.get() && heldItem == Items.SUGAR && itemName.contains("пыль")) {
            Vec3d center = playerRenderPos;
            drawCircle(stack, center, 10, chooseAreaColor(dustColor.get(), hasPlayerInRadius(center, 10)));
        }

        if (disorientEnabled.get() && heldItem == Items.ENDER_EYE && itemName.contains("дезориентация")) {
            Vec3d center = playerRenderPos;
            drawCircle(stack, center, 10, chooseAreaColor(disorientColor.get(), hasPlayerInRadius(center, 10)));
        }

        if (godAuraEnabled.get() && heldItem == Items.PHANTOM_MEMBRANE && itemName.contains("божья аура")) {
            Vec3d center = playerRenderPos;
            drawCircle(stack, center, 2, chooseAreaColor(godAuraColor.get(), hasPlayerInRadius(center, 2)));
        }

        // 4. Снежок заморозки (Только точка падения!)
        if (freezeEnabled.get() && heldItem == Items.SNOWBALL && itemName.contains("заморозк")) {
            Vec3d hitPos = calculateSnowballHitPos(mc.player);
            if (hitPos != null) {
                drawCircle(stack, hitPos, 7.0, chooseAreaColor(freezeColor.get(), hasPlayerInRadius(hitPos, 7.0)));
            }
        }

        stack.pop();

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    // === ПРЕДИКТ ПОЛЕТА СНЕЖКА ===
    private Vec3d calculateSnowballHitPos(PlayerEntity player) {
        double x = player.getX();
        double y = player.getEyeY() - 0.1;
        double z = player.getZ();

        float yaw = player.getYaw();
        float pitch = player.getPitch();

        double vx = -MathHelper.sin(yaw * 0.017453292F) * MathHelper.cos(pitch * 0.017453292F);
        double vy = -MathHelper.sin(pitch * 0.017453292F);
        double vz = MathHelper.cos(yaw * 0.017453292F) * MathHelper.cos(pitch * 0.017453292F);

        Vec3d velocity = new Vec3d(vx, vy, vz).normalize().multiply(1.5);

        for (int i = 0; i < 200; i++) {
            Vec3d currentPos = new Vec3d(x, y, z);
            Vec3d nextPos = currentPos.add(velocity);

            RaycastContext context = new RaycastContext(currentPos, nextPos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player);
            HitResult hit = mc.world.raycast(context);

            if (hit.getType() != HitResult.Type.MISS) {
                return hit.getPos();
            }

            x = nextPos.x;
            y = nextPos.y;
            z = nextPos.z;

            velocity = velocity.multiply(0.99);
            velocity = velocity.subtract(0, 0.03, 0);
        }
        return null;
    }

    // === МЕТОДЫ ОТРИСОВКИ ФОРМ ===
    private void drawBox(MatrixStack stack, BlockPos min, BlockPos max, int color) {
        drawFilledBox(stack, min, max, toFillColor(color));
        drawWireBox(stack, min, max, color);
    }

    private void drawFilledBox(MatrixStack stack, BlockPos min, BlockPos max, int color) {
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        Matrix4f m = stack.peek().getPositionMatrix();

        float a = alpha(color);
        float r = red(color);
        float g = green(color);
        float b = blue(color);

        float x1 = min.getX(); float y1 = min.getY(); float z1 = min.getZ();
        float x2 = max.getX(); float y2 = max.getY(); float z2 = max.getZ();

        addQuad(buf, m, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, r, g, b, a);
        addQuad(buf, m, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, r, g, b, a);
        addQuad(buf, m, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, r, g, b, a);
        addQuad(buf, m, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, r, g, b, a);
        addQuad(buf, m, x2, y1, z2, x2, y2, z2, x1, y2, z2, x1, y1, z2, r, g, b, a);
        addQuad(buf, m, x1, y1, z2, x1, y2, z2, x1, y2, z1, x1, y1, z1, r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private void drawWireBox(MatrixStack stack, BlockPos min, BlockPos max, int color) {
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        Matrix4f m = stack.peek().getPositionMatrix();

        float a = alpha(color);
        float r = red(color);
        float g = green(color);
        float b = blue(color);

        float x1 = min.getX(); float y1 = min.getY(); float z1 = min.getZ();
        float x2 = max.getX(); float y2 = max.getY(); float z2 = max.getZ();

        addLine(buf, m, x1, y1, z1, x2, y1, z1, r, g, b, a);
        addLine(buf, m, x2, y1, z1, x2, y1, z2, r, g, b, a);
        addLine(buf, m, x2, y1, z2, x1, y1, z2, r, g, b, a);
        addLine(buf, m, x1, y1, z2, x1, y1, z1, r, g, b, a);

        addLine(buf, m, x1, y2, z1, x2, y2, z1, r, g, b, a);
        addLine(buf, m, x2, y2, z1, x2, y2, z2, r, g, b, a);
        addLine(buf, m, x2, y2, z2, x1, y2, z2, r, g, b, a);
        addLine(buf, m, x1, y2, z2, x1, y2, z1, r, g, b, a);

        addLine(buf, m, x1, y1, z1, x1, y2, z1, r, g, b, a);
        addLine(buf, m, x2, y1, z1, x2, y2, z1, r, g, b, a);
        addLine(buf, m, x2, y1, z2, x2, y2, z2, r, g, b, a);
        addLine(buf, m, x1, y1, z2, x1, y2, z2, r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private void addQuad(BufferBuilder buf, Matrix4f m,
                         float x1, float y1, float z1,
                         float x2, float y2, float z2,
                         float x3, float y3, float z3,
                         float x4, float y4, float z4,
                         float r, float g, float b, float a) {
        buf.vertex(m, x1, y1, z1).color(r, g, b, a);
        buf.vertex(m, x2, y2, z2).color(r, g, b, a);
        buf.vertex(m, x3, y3, z3).color(r, g, b, a);
        buf.vertex(m, x4, y4, z4).color(r, g, b, a);
    }

    private void addLine(BufferBuilder buf, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a) {
        buf.vertex(m, x1, y1, z1).color(r, g, b, a);
        buf.vertex(m, x2, y2, z2).color(r, g, b, a);
    }

    private void drawPlast(MatrixStack stack, BlockPos playerPos, float tickDelta) {
        if (isDiagonalPlastYaw(tickDelta)) {
            Vec3d look = interpolatedLookVector(tickDelta);
            drawDiagonalPlast(stack, playerPos, look.x >= 0.0 ? 1 : -1, look.z >= 0.0 ? 1 : -1);
            return;
        }

        Direction dir = resolvePlastDirection(tickDelta);
        BlockPos origin = playerPos;
        BlockPos min = null, max = null;

        switch (dir) {
            case NORTH -> { min = origin.add(-2, 0, -4); max = origin.add(3, 4, -2); }
            case SOUTH -> { min = origin.add(-2, 0, 2); max = origin.add(3, 4, 4); }
            case WEST  -> { min = origin.add(-4, 0, -2); max = origin.add(-2, 4, 3); }
            case EAST  -> { min = origin.add(2, 0, -2); max = origin.add(4, 4, 3); }
            case UP    -> { min = origin.add(-2, 3, -2); max = origin.add(3, 5, 3); }
            case DOWN  -> { min = origin.add(-2, -5, -2); max = origin.add(3, -3, 3); }
        }

        if (min != null) {
            drawBox(stack, min, max, chooseAreaColor(plastColor.get(), hasPlayerInBox(min, max)));
        }
    }

    private void drawDiagonalPlast(MatrixStack stack, BlockPos origin, int sx, int sz) {
        List<BlockPos> cells = new ArrayList<>();
        double invSqrt2 = 1.0 / Math.sqrt(2.0);

        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                double cx = dx + 0.5;
                double cz = dz + 0.5;
                double forward = (cx * sx + cz * sz) * invSqrt2;
                double lateral = (cx * -sz + cz * sx) * invSqrt2;

                if (forward >= 2.0 && forward <= 4.0 && lateral >= -2.0 && lateral <= 2.0) {
                    cells.add(origin.add(dx, 0, dz));
                }
            }
        }

        boolean hasPlayerInside = false;
        for (BlockPos cell : cells) {
            if (hasPlayerInBox(cell, cell.add(1, 4, 1))) {
                hasPlayerInside = true;
                break;
            }
        }

        int color = chooseAreaColor(plastColor.get(), hasPlayerInside);
        for (BlockPos cell : cells) {
            drawBox(stack, cell, cell.add(1, 4, 1), color);
        }
    }

    private void drawCircle(MatrixStack stack, Vec3d center, double radius, int color) {
        drawFilledCircle(stack, center, radius, toFillColor(color));
        drawWireCircle(stack, center, radius, color);
    }

    private void drawFilledCircle(MatrixStack stack, Vec3d center, double radius, int color) {
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        Matrix4f m = stack.peek().getPositionMatrix();

        float r = red(color);
        float g = green(color);
        float b = blue(color);
        float a = alpha(color);
        float y = (float) center.y + 0.1f;

        for (int i = 0; i < 48; i++) {
            double angle1 = (i * Math.PI * 2) / 48;
            double angle2 = ((i + 1) * Math.PI * 2) / 48;
            float x1 = (float) (center.x + Math.cos(angle1) * radius);
            float z1 = (float) (center.z + Math.sin(angle1) * radius);
            float x2 = (float) (center.x + Math.cos(angle2) * radius);
            float z2 = (float) (center.z + Math.sin(angle2) * radius);

            buf.vertex(m, (float) center.x, y, (float) center.z).color(r, g, b, a);
            buf.vertex(m, x1, y, z1).color(r, g, b, a);
            buf.vertex(m, x2, y, z2).color(r, g, b, a);
        }

        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private void drawWireCircle(MatrixStack stack, Vec3d center, double radius, int color) {
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        Matrix4f m = stack.peek().getPositionMatrix();

        float r = red(color);
        float g = green(color);
        float b = blue(color);
        float a = alpha(color);

        for (int i = 0; i <= 48; i++) {
            double angle = (i * Math.PI * 2) / 48;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            buf.vertex(m, (float)x, (float)center.y + 0.1f, (float)z).color(r, g, b, a);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private int chooseAreaColor(int normalColor, boolean hasPlayerInside) {
        return hasPlayerInside ? playerInsideColor.get() : normalColor;
    }

    private boolean hasPlayerInRadius(Vec3d center, double radius) {
        double radiusSq = radius * radius;
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (player.squaredDistanceTo(center) <= radiusSq) {
                return true;
            }
        }
        return false;
    }

    private Vec3d interpolatedPos(PlayerEntity player, float tickDelta) {
        return new Vec3d(
                MathHelper.lerp(tickDelta, player.prevX, player.getX()),
                MathHelper.lerp(tickDelta, player.prevY, player.getY()),
                MathHelper.lerp(tickDelta, player.prevZ, player.getZ())
        );
    }

    private Direction resolvePlastDirection(float tickDelta) {
        Vec3d look = interpolatedLookVector(tickDelta);
        double ax = Math.abs(look.x);
        double az = Math.abs(look.z);
        float pitch = MathHelper.lerp(tickDelta, mc.player.prevPitch, mc.player.getPitch());

        if (pitch <= -60.0f) {
            return Direction.UP;
        }
        if (pitch >= 60.0f) {
            return Direction.DOWN;
        }
        if (ax >= az) {
            return look.x >= 0.0 ? Direction.EAST : Direction.WEST;
        }
        return look.z >= 0.0 ? Direction.SOUTH : Direction.NORTH;
    }

    private boolean isDiagonalPlastYaw(float tickDelta) {
        float pitch = MathHelper.lerp(tickDelta, mc.player.prevPitch, mc.player.getPitch());
        if (Math.abs(pitch) >= 60.0f) {
            return false;
        }

        float yaw = MathHelper.wrapDegrees(MathHelper.lerp(tickDelta, mc.player.prevYaw, mc.player.getYaw()));
        return isYawNearDiagonal(yaw, 45.0f)
                || isYawNearDiagonal(yaw, -45.0f)
                || isYawNearDiagonal(yaw, 135.0f)
                || isYawNearDiagonal(yaw, -135.0f);
    }

    private boolean isYawNearDiagonal(float yaw, float target) {
        float delta = Math.abs(MathHelper.wrapDegrees(yaw - target));
        return delta <= 20.0f;
    }

    private Vec3d interpolatedLookVector(float tickDelta) {
        float yaw = MathHelper.lerp(tickDelta, mc.player.prevYaw, mc.player.getYaw());
        float pitch = MathHelper.lerp(tickDelta, mc.player.prevPitch, mc.player.getPitch());
        float yawRad = yaw * 0.017453292F;
        float pitchRad = pitch * 0.017453292F;
        double x = -MathHelper.sin(yawRad) * MathHelper.cos(pitchRad);
        double y = -MathHelper.sin(pitchRad);
        double z = MathHelper.cos(yawRad) * MathHelper.cos(pitchRad);
        return new Vec3d(x, y, z);
    }

    private boolean hasPlayerInBox(BlockPos min, BlockPos max) {
        double x1 = Math.min(min.getX(), max.getX());
        double y1 = Math.min(min.getY(), max.getY());
        double z1 = Math.min(min.getZ(), max.getZ());
        double x2 = Math.max(min.getX(), max.getX());
        double y2 = Math.max(min.getY(), max.getY());
        double z2 = Math.max(min.getZ(), max.getZ());

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            double x = player.getX();
            double y = player.getY();
            double z = player.getZ();
            if (x >= x1 && x <= x2 && y >= y1 && y <= y2 && z >= z1 && z <= z2) {
                return true;
            }
        }
        return false;
    }

    private int toFillColor(int color) {
        int alpha = (color >>> 24) & 0xFF;
        if (alpha == 0) alpha = 255;
        return (color & 0x00FFFFFF) | (Math.min(alpha, 70) << 24);
    }

    private float red(int color) {
        return ((color >> 16) & 0xFF) / 255f;
    }

    private float green(int color) {
        return ((color >> 8) & 0xFF) / 255f;
    }

    private float blue(int color) {
        return (color & 0xFF) / 255f;
    }

    private float alpha(int color) {
        int alpha = (color >>> 24) & 0xFF;
        return (alpha == 0 ? 255 : alpha) / 255f;
    }

    // Проекция 3D -> 2D
    private static Vec3d projectToScreen(double x, double y, double z, Camera camera, float tickDelta) {
        Vec3d camPos = camera.getPos();
        Vector4f pos = new Vector4f((float) (x - camPos.x), (float) (y - camPos.y), (float) (z - camPos.z), 1.0f);

        Quaternionf rotation = new Quaternionf(camera.getRotation()).conjugate();
        pos.rotate(rotation);

        if (pos.z >= 0) return null;

        GameRendererAccessor gameRendererAccessor = (GameRendererAccessor) mc.gameRenderer;
        double fov = gameRendererAccessor.invokeGetFov(camera, tickDelta, true);
        float halfFovTan = (float) Math.tan(Math.toRadians(fov) / 2.0);

        float screenWidth = mc.getWindow().getScaledWidth();
        float screenHeight = mc.getWindow().getScaledHeight();

        float scale = (screenHeight / 2.0f) / halfFovTan;

        float screenX = screenWidth / 2.0f + (pos.x / -pos.z) * scale;
        float screenY = screenHeight / 2.0f - (pos.y / -pos.z) * scale;

        if (screenX < -500 || screenX > screenWidth + 500 || screenY < -500 || screenY > screenHeight + 500) return null;

        return new Vec3d(screenX, screenY, 0);
    }


}
