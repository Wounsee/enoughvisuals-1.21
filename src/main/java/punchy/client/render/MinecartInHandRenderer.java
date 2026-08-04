package punchy.client.render;

import java.util.EnumMap;
import net.minecraft.class_1268;
import net.minecraft.class_1299;
import net.minecraft.class_1688;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1808;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_243;
import net.minecraft.class_2595;
import net.minecraft.class_2680;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3730;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_4608;
import net.minecraft.class_7833;
import net.minecraft.class_7923;
import net.minecraft.class_824;
import net.minecraft.class_827;
import net.minecraft.class_897;
import net.minecraft.class_898;
import punchy.client.render.ChestProxyTuning;
import punchy.compat.EmfEtfCemCompat;
import punchy.mixin.client.accessor.MinecartItemAccessor;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/MinecartInHandRenderer.class */
public final class MinecartInHandRenderer {
    private static final EnumMap<class_1268, MinecartCache> CACHE = new EnumMap<>(class_1268.class);
    private static final EnumMap<class_1268, Float> CHEST_LID_OPEN = new EnumMap<>(class_1268.class);
    private static final EnumMap<class_1268, Long> CHEST_LID_LAST_UPDATE = new EnumMap<>(class_1268.class);
    private static final float SCALE = 0.65f;
    private static final float ROT_Y = 3.1415927f;

    private MinecartInHandRenderer() {
    }

    public static boolean isMinecartStack(class_1799 stack) {
        return (stack == null || stack.method_7960() || !(stack.method_7909() instanceof class_1808)) ? false : true;
    }

    public static boolean tryRender(class_1799 stack, class_1268 hand, class_4587 matrices, class_4597 buffer, int light) {
        MinecartCache cache;
        class_1688 minecart;
        class_1688 renderCart;
        class_897<? super class_1688, ?> renderer;
        class_1808 class_1808VarMethod_7909 = stack.method_7909();
        if (!(class_1808VarMethod_7909 instanceof class_1808)) {
            return false;
        }
        class_1808 minecartItem = class_1808VarMethod_7909;
        class_310 client = class_310.method_1551();
        if (client == null || client.field_1687 == null || (minecart = (cache = (MinecartCache) CACHE.computeIfAbsent(hand, h -> {
            return new MinecartCache();
        })).resolve(minecartItem, stack, client)) == null || (renderCart = cache.getRenderCart()) == null) {
            return false;
        }
        double vy = 0.0d;
        if (client.field_1724 != null) {
            vy = client.field_1724.method_18798().field_1351;
        }
        minecart.method_18800(0.0d, vy, 0.0d);
        renderCart.method_18800(0.0d, vy, 0.0d);
        stabilizePose(minecart);
        stabilizePose(renderCart);
        class_898 dispatcher = client.method_1561();
        if (dispatcher == null || (renderer = dispatcher.method_3953(renderCart)) == null) {
            return false;
        }
        float partialTicks = client.method_61966().method_60637(true);
        MinecartLightState minecartLightStateMethod_55269 = renderer.method_55269();
        renderer.method_62354(renderCart, minecartLightStateMethod_55269, partialTicks);
        if (minecartLightStateMethod_55269 instanceof MinecartLightState) {
            MinecartLightState lightState = minecartLightStateMethod_55269;
            lightState.punchy$setLight(light);
        }
        MinecartInHandContext.push();
        try {
            MinecartInHandContext.setVy(vy);
            MinecartInHandContext.setTntOffset(0.0f);
            MinecartInHandContext.setType(resolveEntityType(minecartItem));
            matrices.method_22903();
            float baseFall = InHandFallPhysics.update(client.field_1724, hand);
            float fallOffset = baseFall * 0.3f;
            MinecartInHandContext.setTntOffset(isTntMinecart(stack) ? fallOffset * 1.7f : 0.0f);
            if (fallOffset != 0.0f) {
                matrices.method_46416(0.0f, fallOffset, 0.0f);
            }
            matrices.method_22905(SCALE, SCALE, SCALE);
            matrices.method_22907(class_7833.field_40716.rotation(ROT_Y));
            dispatcher.method_68833(minecartLightStateMethod_55269, 0.0d, 0.0d, 0.0d, matrices, buffer, light);
            if (isChestMinecart(stack)) {
                renderChestInMinecart(hand, matrices, buffer, client, light);
            } else if (isTntMinecart(stack)) {
                renderTntInMinecart(matrices, buffer, client, light);
            }
            matrices.method_22909();
            return true;
        } finally {
            MinecartInHandContext.pop();
        }
    }

    private static boolean isChestMinecart(class_1799 stack) {
        class_2960 id = class_7923.field_41178.method_10221(stack.method_7909());
        return id != null && "chest_minecart".equals(id.method_12832());
    }

    private static boolean isTntMinecart(class_1799 stack) {
        class_2960 id = class_7923.field_41178.method_10221(stack.method_7909());
        return id != null && "tnt_minecart".equals(id.method_12832());
    }

    private static class_1299<? extends class_1688> resolveEntityType(class_1808 minecartItem) {
        if (minecartItem == null) {
            return null;
        }
        return ((MinecartItemAccessor) minecartItem).punchy$getType();
    }

    private static void renderChestInMinecart(class_1268 hand, class_4587 matrices, class_4597 buffer, class_310 client, int light) {
        class_824 dispatcher;
        ChestProxyTuning.Profile profile;
        float open;
        if (client == null || client.field_1687 == null || (dispatcher = client.method_31975()) == null) {
            return;
        }
        class_2680 state = class_2246.field_10034.method_9564();
        PunchyChestBlockEntity chest = new PunchyChestBlockEntity(class_2338.field_10980, state);
        chest.method_31662(client.field_1687);
        class_827<PunchyChestBlockEntity> renderer = dispatcher.method_3550(chest);
        if (renderer == null || (profile = ChestProxyTuning.get(ChestProxyTuning.Kind.MINECART)) == null) {
            return;
        }
        boolean disablePhysics = EmfEtfCemCompat.disableProxyPhysics(EmfEtfCemCompat.ProxyPhysicsTarget.CHEST);
        if (disablePhysics) {
            open = 0.0f;
        } else {
            float target = InHandLidPhysics.computeFallTarget(client.field_1724);
            open = InHandLidPhysics.updateLid(CHEST_LID_OPEN, CHEST_LID_LAST_UPDATE, hand, target);
        }
        chest.punchy$setOpen(open);
        matrices.method_22903();
        matrices.method_46416(profile.offX, profile.offY, profile.offZ);
        matrices.method_22907(class_7833.field_40714.rotationDegrees(profile.rotX));
        matrices.method_22907(class_7833.field_40716.rotationDegrees(profile.rotY));
        matrices.method_22907(class_7833.field_40718.rotationDegrees(profile.rotZ));
        matrices.method_22905(profile.scale, profile.scale, profile.scale);
        renderer.method_3569(chest, 0.0f, matrices, buffer, light, class_4608.field_21444, class_243.field_1353);
        matrices.method_22909();
    }

    private static void renderTntInMinecart(class_4587 matrices, class_4597 buffer, class_310 client, int light) {
        ChestProxyTuning.Profile profile;
        if (client == null || (profile = ChestProxyTuning.get(ChestProxyTuning.Kind.MINECART)) == null) {
            return;
        }
        float offset = MinecartInHandContext.getTntOffset();
        matrices.method_22903();
        matrices.method_46416(profile.offX, profile.offY + offset, profile.offZ);
        matrices.method_22907(class_7833.field_40714.rotationDegrees(profile.rotX));
        matrices.method_22907(class_7833.field_40716.rotationDegrees(profile.rotY));
        matrices.method_22907(class_7833.field_40718.rotationDegrees(profile.rotZ));
        matrices.method_22905(profile.scale, profile.scale, profile.scale);
        client.method_1541().method_3353(class_2246.field_10375.method_9564(), matrices, buffer, light, class_4608.field_21444);
        matrices.method_22909();
    }

    private static void stabilizePose(class_1688 minecart) {
        if (minecart == null) {
            return;
        }
        minecart.method_5814(0.0d, 0.0d, 0.0d);
        minecart.method_36456(180.0f);
        minecart.method_36457(0.0f);
        minecart.field_5982 = minecart.method_36454();
        minecart.field_6004 = minecart.method_36455();
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/MinecartInHandRenderer$MinecartCache.class */
    private static final class MinecartCache {
        private class_1792 lastItem;
        private class_1688 minecart;
        private class_1688 renderCart;

        private MinecartCache() {
        }

        class_1688 resolve(class_1808 minecartItem, class_1799 stack, class_310 client) {
            class_2960 id;
            class_1792 item = stack.method_7909();
            if (this.minecart == null || this.lastItem != item) {
                this.lastItem = item;
                class_1299<? extends class_1688> type = MinecartInHandRenderer.resolveEntityType(minecartItem);
                if (type == null && (id = class_7923.field_41178.method_10221(item)) != null) {
                    class_1299<? extends class_1688> resolved = (class_1299) class_7923.field_41177.method_63535(id);
                    type = resolved;
                }
                if (type == null) {
                    type = class_1299.field_6096;
                }
                this.minecart = type.method_5883(client.field_1687, class_3730.field_16462);
                if (this.minecart != null) {
                    this.minecart.method_5814(0.0d, 0.0d, 0.0d);
                    this.minecart.method_36456(180.0f);
                }
                this.renderCart = resolveRenderCart(client, stack, this.minecart);
            }
            return this.minecart;
        }

        class_1688 getRenderCart() {
            return this.renderCart != null ? this.renderCart : this.minecart;
        }

        private class_1688 resolveRenderCart(class_310 client, class_1799 stack, class_1688 source) {
            if (MinecartInHandRenderer.isChestMinecart(stack) || MinecartInHandRenderer.isTntMinecart(stack)) {
                class_1688 base = class_1299.field_6096.method_5883(client.field_1687, class_3730.field_16462);
                if (base != null) {
                    base.method_5814(0.0d, 0.0d, 0.0d);
                    base.method_36456(180.0f);
                }
                return base != null ? base : source;
            }
            return source;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/MinecartInHandRenderer$PunchyChestBlockEntity.class */
    private static final class PunchyChestBlockEntity extends class_2595 {
        private float punchy$open;

        private PunchyChestBlockEntity(class_2338 pos, class_2680 state) {
            super(pos, state);
        }

        private void punchy$setOpen(float open) {
            this.punchy$open = open;
        }

        public float method_11274(float partialTicks) {
            return this.punchy$open;
        }
    }
}
