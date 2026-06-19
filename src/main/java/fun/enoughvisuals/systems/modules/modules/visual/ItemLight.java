package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.mixins.accessors.GameRendererAccessor;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.modules.client.CustomTheme;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.MultiItemSelectSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.render.ScreenScaleUtil;
import fun.enoughvisuals.utils.render.blur.GlassUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Quaternionf;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Register(name="Item Light", id="item_light", description="Подсвечивает выпавшие предметы", category = Category.VISUAL)
public class ItemLight extends Module {

    private static ItemLight INSTANCE;

    public final BooleanSetting showAll = add(new BooleanSetting("Показать все", "show_all", "Показывать все предметы", false));

    public final MultiItemSelectSetting selectedItems = add(new MultiItemSelectSetting("Предметы", "items", "Выбранные предметы", new HashSet<>()) {
        @Override
        public boolean isVisible() {
            return !showAll.get();
        }
    });

    public final SliderSetting size = add(new SliderSetting("Размер", "size", "Размер плашки", 1.0, 0.5, 2.0, 0.05));

    public ItemLight() {
        INSTANCE = this;
    }

    public static void renderOverlay(DrawContext ctx, float tickDelta) {
        if (INSTANCE == null || !INSTANCE.isEnabled()) return;
        if (mc.world == null || mc.player == null) return;

        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();

        List<ItemEntity> validItems = new ArrayList<>();

        Box searchBox = mc.player.getBoundingBox().expand(30.0);
        for (Entity entity : mc.world.getOtherEntities(mc.player, searchBox, candidate -> candidate instanceof ItemEntity)) {
            ItemEntity itemEntity = (ItemEntity) entity;
            ItemStack stack = itemEntity.getStack();
            if (stack.isEmpty()) continue;

            if (camPos.squaredDistanceTo(itemEntity.getPos()) > 30 * 30) continue;

            if (!INSTANCE.showAll.get()) {
                String itemId = Registries.ITEM.getId(stack.getItem()).toString();
                if (!INSTANCE.selectedItems.isSelected(itemId)) continue;
            }

            double x = MathHelper.lerp(tickDelta, itemEntity.prevX, itemEntity.getX());
            double y = MathHelper.lerp(tickDelta, itemEntity.prevY, itemEntity.getY()) + 0.35;
            double z = MathHelper.lerp(tickDelta, itemEntity.prevZ, itemEntity.getZ());
            Vec3d targetPos = new Vec3d(x, y, z);

            RaycastContext raycastCtx = new RaycastContext(
                    camPos, targetPos,
                    RaycastContext.ShapeType.VISUAL,
                    RaycastContext.FluidHandling.NONE,
                    mc.player
            );

            BlockHitResult hit = mc.world.raycast(raycastCtx);
            if (hit.getType() != HitResult.Type.MISS) continue;

            validItems.add(itemEntity);
        }

        List<ItemCluster> clusters = new ArrayList<>();
        for (ItemEntity item : validItems) {
            Vec3d pos = new Vec3d(
                    MathHelper.lerp(tickDelta, item.prevX, item.getX()),
                    MathHelper.lerp(tickDelta, item.prevY, item.getY()) + 0.35,
                    MathHelper.lerp(tickDelta, item.prevZ, item.getZ())
            );

            boolean added = false;
            for (ItemCluster cluster : clusters) {
                if (cluster.center.distanceTo(pos) <= 1.5) {
                    cluster.addOrMerge(item.getStack());
                    added = true;
                    break;
                }
            }
            if (!added) clusters.add(new ItemCluster(pos, item.getStack()));
        }

        float scale = INSTANCE.size.get().floatValue();

        for (ItemCluster cluster : clusters) {
            Vec3d pos2d = projectToScreen(cluster.center.x, cluster.center.y, cluster.center.z, camera, tickDelta);

            if (pos2d != null) {
                float baseRowHeight = 13f;
                float baseGap = 2.5f;

                float scaledRowHeight = baseRowHeight * scale;
                float scaledGap = baseGap * scale;
                float totalHeight = cluster.items.size() * scaledRowHeight + (cluster.items.size() - 1) * scaledGap;

                float currentY = (float) pos2d.y - (totalHeight / 2f) + (scaledRowHeight / 2f);

                for (ItemStack stack : cluster.items) {
                    INSTANCE.drawItemLabel(ctx, stack, (float) pos2d.x, currentY, scale);
                    currentY += scaledRowHeight + scaledGap;
                }
            }
        }
    }

    private void drawItemLabel(DrawContext ctx, ItemStack stack, float x, float y, float scale) {
        String name = stack.getName().getString();
        String count = "x" + stack.getCount();

        float nameSize = 5.8f;
        float countSize = 5.2f;
        float iconSize = 9.5f;
        float gap = 2.5f;
        float height = 13f;
        float paddingX = 5.5f;

        float nameWidth = CustomFontRenderer.getTextWidth(name, nameSize, FontWeight.SEMIBOLD);
        float countWidth = CustomFontRenderer.getTextWidth(count, countSize, FontWeight.SEMIBOLD);
        float totalWidth = iconSize + gap + nameWidth + gap + countWidth;

                                                                                         
        float scaledWidth = totalWidth * scale;
        float scaledHeight = height * scale;
        float scaledPaddingX = paddingX * scale;
        float scaledRadius = 3.5f * scale;

        float absStartX = x - (scaledWidth / 2f);
        float absStartY = y - (scaledHeight / 2f);

                                                                        
        GlassUtil.drawGlassPanel(ctx, absStartX - scaledPaddingX, absStartY, scaledWidth + (scaledPaddingX * 2), scaledHeight, scaledRadius, CustomTheme.itemPanelTint(), CustomTheme.itemPanelTintAlpha(), 1.0f);

                                                                                    
        float startX = -(totalWidth / 2f);
        float startY = -(height / 2f);

        ctx.getMatrices().push();
        ctx.getMatrices().translate(x, y, 0);
        ctx.getMatrices().scale(scale, scale, 1f);

        float textOffsetY = -0.5f;

                 
        float iconY = startY + (height - iconSize) / 2f - 0.5f;
        ctx.getMatrices().push();
        ctx.getMatrices().translate(startX, iconY, 0);
        ctx.getMatrices().scale(iconSize / 16f, iconSize / 16f, 1f);
        ctx.drawItem(stack, 0, 0);
        ctx.getMatrices().pop();

                   
        float textY = startY + (height - nameSize) / 2f + textOffsetY;
        float textX = startX + iconSize + gap;
        CustomFontRenderer.drawText(ctx, name, textX, textY, nameSize, 0xFFFFFFFF, FontWeight.SEMIBOLD);

                     
        float countY = startY + (height - countSize) / 2f + textOffsetY;
        float countX = textX + nameWidth + gap;
        CustomFontRenderer.drawText(ctx, count, countX, countY, countSize, 0xFFAAAAAA, FontWeight.SEMIBOLD);

        ctx.getMatrices().pop();
    }

    private static Vec3d projectToScreen(double x, double y, double z, Camera camera, float tickDelta) {
        Vec3d camPos = camera.getPos();
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

    private static class ItemCluster {
        public Vec3d center;
        public List<ItemStack> items = new ArrayList<>();

        public ItemCluster(Vec3d center, ItemStack initialStack) {
            this.center = center;
            this.items.add(initialStack.copy());
        }

        public void addOrMerge(ItemStack stack) {
            for (ItemStack existing : items) {
                if (ItemStack.areItemsEqual(existing, stack)) {
                    existing.setCount(existing.getCount() + stack.getCount());
                    return;
                }
            }
            items.add(stack.copy());
        }
    }
}
