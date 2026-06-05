package fun.enoughvisuals.systems.modules.modules.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.mixins.accessors.GameRendererAccessor;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
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
    public final BooleanSetting trapEnabled = add(new BooleanSetting("Трапка", "trap_enabled", true));
    public final BooleanSetting dragonTrapEnabled = add(new BooleanSetting("Драконья трапка", "dragon_trap_enabled", false));
    public final BooleanSetting dustEnabled = add(new BooleanSetting("Явная пыль", "dust_enabled", true));
    public final BooleanSetting disorientEnabled = add(new BooleanSetting("Дезориентация", "disorient_enabled", true));
    public final BooleanSetting godAuraEnabled = add(new BooleanSetting("Божья аура", "god_aura_enabled", true));
    public final BooleanSetting plastEnabled = add(new BooleanSetting("Пласт", "plast_enabled", true));
    public final BooleanSetting freezeEnabled = add(new BooleanSetting("Снежок заморозки", "freeze_enabled", true));

    // --- Логика сканера трапок ---
    private final List<ActiveTrap> activeTraps = new ArrayList<>();
    private int tickCounter = 0;

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
        if (!isInGame()) {
            activeTraps.clear();
            return;
        }

        long now = System.currentTimeMillis();

        // Удаляем протухшие трапки (> 15 сек)
        activeTraps.removeIf(trap -> now > trap.endTime);

        // Парсим мир вокруг игроков каждые 5 тиков
        tickCounter++;
        if (tickCounter % 12 == 0) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player == mc.player || player.squaredDistanceTo(mc.player) > 48.0 * 48.0) {
                    continue;
                }
                // Ищем истинный центр трапки вокруг игрока
                BlockPos trueCenter = findTrapCenter(player.getBlockPos());

                if (trueCenter != null) {
                    // Проверяем, нет ли уже таймера на этом месте (защита от спама 5 плашек)
                    boolean alreadyExists = false;
                    for (ActiveTrap trap : activeTraps) {
                        // Если в радиусе 3 блоков уже есть таймер - это та же самая трапка
                        if (trap.pos.getSquaredDistance(trueCenter) < 9.0) {
                            alreadyExists = true;
                            break;
                        }
                    }

                    if (!alreadyExists) {
                        activeTraps.add(new ActiveTrap(trueCenter, now + 15000L));
                    }
                }
            }
        }
    }

    @Override
    protected void onDisable() {
        activeTraps.clear();
    }

    // === ПАРСЕР СТРУКТУРЫ (ПОИСК ИСТИННОГО ЦЕНТРА) ===
    private BlockPos findTrapCenter(BlockPos playerPos) {
        BlockPos bestCenter = null;
        float bestScore = 0.65f; // Минимум 65% стен должно быть построено

        // Сканируем небольшое смещение от игрока (чтобы найти ровный геометрический центр)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos checkPos = playerPos.add(dx, dy, dz);
                    float score = getTrapScore(checkPos);

                    if (score > bestScore) {
                        bestScore = score;
                        bestCenter = checkPos;
                    }
                }
            }
        }
        return bestCenter;
    }

    private float getTrapScore(BlockPos center) {
        int solidWalls = 0;
        int totalWalls = 0;
        int solidInside = 0;
        int totalInside = 0;

        // Коробка 5x5, высота 4 блока
        for (int y = 0; y <= 3; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    boolean isWall = (Math.abs(x) == 2 || Math.abs(z) == 2);
                    BlockPos checkPos = center.add(x, y, z);

                    // isReplaceable - пропускает воду, траву, воздух (все, сквозь что можно пройти)
                    boolean isSolid = !mc.world.getBlockState(checkPos).isReplaceable();

                    if (isWall) {
                        totalWalls++;
                        if (isSolid) solidWalls++;
                    } else {
                        // Внутренность трапки (3x3)
                        totalInside++;
                        if (isSolid) solidInside++;
                    }
                }
            }
        }

        float wallRatio = (float) solidWalls / totalWalls;
        float insideRatio = (float) solidInside / totalInside;

        // Если внутри больше 15% застроено блоками - это пещера или дом, а не пустая трапка
        if (insideRatio > 0.15f) return 0f;

        return wallRatio;
    }


    // === 2D РЕНДЕР (ИДЕАЛЬНАЯ ПАНЕЛЬКА С ИКОНКОЙ) ===
    private void onRender2D(DrawContext ctx, net.minecraft.client.render.RenderTickCounter tickCounter) {
        if (!isEnabled() || !isInGame()) return;

        Camera camera = mc.gameRenderer.getCamera();
        float tickDelta = tickCounter.getTickDelta(true);
        long now = System.currentTimeMillis();

        for (ActiveTrap trap : activeTraps) {
            long timeLeft = trap.endTime - now;
            if (timeLeft <= 0) continue;

            // Теперь pos.x/z + 0.5 - это 100% идеальный центр коробки!
            Vec3d pos2d = projectToScreen(trap.pos.getX() + 0.5, trap.pos.getY() + 2.0, trap.pos.getZ() + 0.5, camera, tickDelta);

            if (pos2d != null) {
                String text = String.format(ClientLanguage.text("ft_helper.trap.timer", "Трапка: %.1fс"), timeLeft / 1000.0f);
                drawTrapLabel(ctx, text, (float) pos2d.x, (float) pos2d.y);
            }
        }
    }

    private void drawTrapLabel(DrawContext ctx, String text, float x, float y) {
        float textSize = 5.8f;
        float iconSize = 9.5f;
        float gap = 2.5f;
        float height = 13f;
        float paddingX = 5.5f;

        float textWidth = CustomFontRenderer.getTextWidth(text, textSize, FontWeight.SEMIBOLD);
        float totalWidth = iconSize + gap + textWidth;

        float absStartX = x - (totalWidth / 2f);
        float absStartY = y - (height / 2f);

        // 1. Фон (GlassPanel)
        GlassUtil.drawGlassPanel(ctx, absStartX - paddingX, absStartY, totalWidth + (paddingX * 2), height, 3.5f, 0x000000, 0.6f, 1.0f);

        float startX = -(totalWidth / 2f);
        float startY = -(height / 2f);

        ctx.getMatrices().push();
        // Поднимаем Z-index (400), чтобы иконка лома и текст гарантированно рисовались ПОВЕРХ стекла и блоков
        ctx.getMatrices().translate(x, y, 400);

        float textOffsetY = -0.5f;

        // 2. Иконка незеритового лома (как в ItemLight)
        float iconY = startY + (height - iconSize) / 2f - 0.5f;
        ctx.getMatrices().push();
        ctx.getMatrices().translate(startX, iconY, 0);
        ctx.getMatrices().scale(iconSize / 16f, iconSize / 16f, 1f);
        ctx.drawItem(new ItemStack(Items.NETHERITE_SCRAP), 0, 0);
        ctx.getMatrices().pop();

        // 3. Текст
        int color = text.contains(" 3.") || text.contains(" 2.") || text.contains(" 1.") || text.contains(" 0.") ? 0xFF5555 : 0xFFFFFFFF;
        float textY = startY + (height - textSize) / 2f + textOffsetY;
        float textX = startX + iconSize + gap;
        CustomFontRenderer.drawText(ctx, text, textX, textY, textSize, color, FontWeight.SEMIBOLD);

        ctx.getMatrices().pop();
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

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.lineWidth(2.0f);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        stack.push();
        stack.translate(-camPos.x, -camPos.y, -camPos.z);

        // 1. Превью Трапки (в руках)
        if (trapEnabled.get() && heldItem == Items.NETHERITE_SCRAP) {
            drawWireBox(stack, playerPos.add(-2, 0, -2), playerPos.add(3, 4, 3), 0xFFFF5555);
            if (dragonTrapEnabled.get()) {
                drawWireBox(stack, playerPos.add(-4, 0, -4), playerPos.add(4, 4, 4), 0xFFFFAA00);
            }
        }

        // 2. Превью Пласта
        if (plastEnabled.get() && heldItem == Items.DRIED_KELP) {
            drawPlast(stack, playerPos);
        }

        // 3. Статичные Круги
        if (dustEnabled.get() && heldItem == Items.SUGAR && itemName.contains("пыль")) {
            drawWireCircle(stack, mc.player.getPos(), 10, 0xFF55FFFF);
        }

        if (disorientEnabled.get() && heldItem == Items.ENDER_EYE && itemName.contains("дезориентация")) {
            drawWireCircle(stack, mc.player.getPos(), 10, 0xFFAA00AA);
        }

        if (godAuraEnabled.get() && heldItem == Items.PHANTOM_MEMBRANE && itemName.contains("божья аура")) {
            drawWireCircle(stack, mc.player.getPos(), 2, 0xFFFFFF55);
        }

        // 4. Снежок заморозки (Только точка падения!)
        if (freezeEnabled.get() && heldItem == Items.SNOWBALL && itemName.contains("заморозк")) {
            Vec3d hitPos = calculateSnowballHitPos(mc.player);
            if (hitPos != null) {
                drawWireCircle(stack, hitPos, 7.0, 0xFF55FFFF);
            }
        }

        stack.pop();

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
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
    private void drawWireBox(MatrixStack stack, BlockPos min, BlockPos max, int color) {
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        Matrix4f m = stack.peek().getPositionMatrix();

        float a = ((color >> 24) & 0xFF) / 255f;
        if (a == 0) a = 1.0f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

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

    private void addLine(BufferBuilder buf, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a) {
        buf.vertex(m, x1, y1, z1).color(r, g, b, a);
        buf.vertex(m, x2, y2, z2).color(r, g, b, a);
    }

    private void drawPlast(MatrixStack stack, BlockPos playerPos) {
        Direction dir = mc.player.getHorizontalFacing();
        BlockPos min = null, max = null;

        switch (dir) {
            case NORTH -> { min = playerPos.add(-2, 0, -4); max = playerPos.add(3, 4, -2); }
            case SOUTH -> { min = playerPos.add(-2, 0, 2); max = playerPos.add(3, 4, 4); }
            case WEST  -> { min = playerPos.add(-4, 0, -2); max = playerPos.add(-2, 4, 3); }
            case EAST  -> { min = playerPos.add(2, 0, -2); max = playerPos.add(4, 4, 3); }
        }

        if (min != null) {
            drawWireBox(stack, min, max, 0xFFFFFFFF);
        }
    }

    private void drawWireCircle(MatrixStack stack, Vec3d center, double radius, int color) {
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        Matrix4f m = stack.peek().getPositionMatrix();

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        for (int i = 0; i <= 40; i++) {
            double angle = (i * Math.PI * 2) / 40;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            buf.vertex(m, (float)x, (float)center.y + 0.1f, (float)z).color(r, g, b, 1f);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
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

    private static class ActiveTrap {
        public final BlockPos pos;
        public final long endTime;

        public ActiveTrap(BlockPos pos, long endTime) {
            this.pos = pos;
            this.endTime = endTime;
        }
    }
}
