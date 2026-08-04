package punchy.client.render;

import java.util.EnumMap;
import net.minecraft.class_10004;
import net.minecraft.class_10017;
import net.minecraft.class_10255;
import net.minecraft.class_10256;
import net.minecraft.class_1268;
import net.minecraft.class_1299;
import net.minecraft.class_1749;
import net.minecraft.class_1799;
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
import punchy.mixin.client.accessor.BoatItemAccessor;
import punchy.platform.Services;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/BoatInHandRenderer.class */
public final class BoatInHandRenderer {
    private static final String BERYL_MOD_ID = "beryl";
    private static final EnumMap<class_1268, BoatCache> CACHE = new EnumMap<>(class_1268.class);
    private static final EnumMap<class_1268, Float> CHEST_LID_OPEN = new EnumMap<>(class_1268.class);
    private static final EnumMap<class_1268, Long> CHEST_LID_LAST_UPDATE = new EnumMap<>(class_1268.class);
    private static final float SCALE = 0.6f;
    private static final float ROT_Y = 3.1415927f;

    private BoatInHandRenderer() {
    }

    public static boolean isBoatStack(class_1799 stack) {
        return (stack == null || stack.method_7960() || !(stack.method_7909() instanceof class_1749)) ? false : true;
    }

    public static boolean tryRender(class_1799 stack, class_1268 hand, class_4587 matrices, class_4597 buffer, int light) {
        class_310 client;
        BoatCache cache;
        class_10255 boat;
        class_10255 renderBoat;
        class_897<? super class_10255, ?> renderer;
        class_1749 class_1749VarMethod_7909 = stack.method_7909();
        if (!(class_1749VarMethod_7909 instanceof class_1749)) {
            return false;
        }
        class_1749 boatItem = class_1749VarMethod_7909;
        if (shouldUseVanillaBoatRender(stack) || (client = class_310.method_1551()) == null || client.field_1687 == null || (boat = (cache = (BoatCache) CACHE.computeIfAbsent(hand, h -> {
            return new BoatCache();
        })).resolve(boatItem, stack, client)) == null || (renderBoat = cache.getRenderBoat()) == null) {
            return false;
        }
        if (client.field_1724 != null) {
            double vy = client.field_1724.method_18798().field_1351;
            boat.method_18800(0.0d, vy, 0.0d);
            renderBoat.method_18800(0.0d, vy, 0.0d);
        }
        stabilizePose(boat);
        stabilizePose(renderBoat);
        class_898 dispatcher = client.method_1561();
        if (dispatcher == null || (renderer = dispatcher.method_3953(renderBoat)) == null) {
            return false;
        }
        BoatInHandContext.push();
        try {
            float partialTicks = client.method_61966().method_60637(true);
            BoatLidState boatLidStateMethod_55269 = renderer.method_55269();
            renderer.method_62354(renderBoat, boatLidStateMethod_55269, partialTicks);
            if (boatLidStateMethod_55269 instanceof BoatLidState) {
                BoatLidState lidState = boatLidStateMethod_55269;
                lidState.punchy$setLight(light);
            }
            matrices.method_22903();
            float fallOffset = InHandFallPhysics.update(client.field_1724, hand) * 0.3f;
            if (fallOffset != 0.0f) {
                matrices.method_46416(0.0f, fallOffset, 0.0f);
            }
            matrices.method_22905(SCALE, SCALE, SCALE);
            matrices.method_22907(class_7833.field_40716.rotation(ROT_Y));
            dispatcher.method_68833(boatLidStateMethod_55269, 0.0d, 0.0d, 0.0d, matrices, buffer, light);
            if (boat instanceof class_10256) {
                ChestProxyTuning.Kind chestKind = resolveChestProxyKind(stack);
                renderChestInBoat(hand, boatLidStateMethod_55269, matrices, buffer, client, chestKind, light);
            }
            matrices.method_22909();
            return true;
        } finally {
            BoatInHandContext.pop();
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/BoatInHandRenderer$BoatCache.class */
    private static final class BoatCache {
        private class_1799 lastStack;
        private class_10255 boat;
        private class_10255 renderBoat;

        private BoatCache() {
        }

        class_10255 resolve(class_1749 boatItem, class_1799 stack, class_310 client) {
            if (this.boat == null || this.lastStack == null || this.lastStack.method_7909() != stack.method_7909()) {
                this.lastStack = stack;
                this.boat = BoatInHandRenderer.createBoat(boatItem, client);
                this.renderBoat = resolveRenderBoat(client, boatItem, this.boat);
            }
            return this.boat;
        }

        class_10255 getRenderBoat() {
            return this.renderBoat != null ? this.renderBoat : this.boat;
        }

        private class_10255 resolveRenderBoat(class_310 client, class_1749 boatItem, class_10255 source) {
            class_1299<?> baseType;
            class_10255 class_10255Var;
            if ((source instanceof class_10256) && (baseType = BoatInHandRenderer.resolveBaseBoatType(BoatInHandRenderer.resolveBoatEntityType(boatItem))) != null) {
                class_10255 class_10255VarMethod_5883 = baseType.method_5883(client.field_1687, class_3730.field_16462);
                if (class_10255VarMethod_5883 instanceof class_10255) {
                    class_10255 created = class_10255VarMethod_5883;
                    class_10255Var = created;
                } else {
                    class_10255Var = null;
                }
                class_10255 baseBoat = class_10255Var;
                if (baseBoat != null) {
                    baseBoat.method_5814(0.0d, 0.0d, 0.0d);
                    baseBoat.method_36456(180.0f);
                    return baseBoat;
                }
            }
            return source;
        }
    }

    private static class_10255 createBoat(class_1749 boatItem, class_310 client) {
        class_10255 boat;
        if (boatItem == null || client == null || client.field_1687 == null) {
            return null;
        }
        BoatItemAccessor accessor = (BoatItemAccessor) boatItem;
        class_1299<? extends class_10255> type = accessor.punchy$getEntityType();
        if (type == null || (boat = type.method_5883(client.field_1687, class_3730.field_16462)) == null) {
            return null;
        }
        boat.method_5814(0.0d, 0.0d, 0.0d);
        boat.method_36456(180.0f);
        return boat;
    }

    private static class_1299<?> resolveBoatEntityType(class_1749 boatItem) {
        if (boatItem == null) {
            return null;
        }
        return ((BoatItemAccessor) boatItem).punchy$getEntityType();
    }

    private static class_1299<?> resolveBaseBoatType(class_1299<?> type) {
        class_2960 id;
        if (type == null || (id = class_7923.field_41177.method_10221(type)) == null) {
            return null;
        }
        String path = id.method_12832();
        String basePath = null;
        if (path.endsWith("_chest_boat")) {
            basePath = path.replace("_chest_boat", "_boat");
        } else if (path.endsWith("_chest_raft")) {
            basePath = path.replace("_chest_raft", "_raft");
        }
        if (basePath == null) {
            return null;
        }
        class_2960 baseId = class_2960.method_60655(id.method_12836(), basePath);
        class_1299<?> baseType = (class_1299) class_7923.field_41177.method_63535(baseId);
        if (baseType == null) {
            return null;
        }
        class_2960 resolved = class_7923.field_41177.method_10221(baseType);
        if (baseId.equals(resolved)) {
            return baseType;
        }
        return null;
    }

    private static void renderChestInBoat(class_1268 hand, class_10017 boatRenderState, class_4587 matrices, class_4597 buffer, class_310 client, ChestProxyTuning.Kind chestKind, int light) {
        class_824 dispatcher;
        float open;
        if (client == null || client.field_1687 == null || (dispatcher = client.method_31975()) == null) {
            return;
        }
        class_2680 state = class_2246.field_10034.method_9564();
        PunchyChestBlockEntity chest = new PunchyChestBlockEntity(class_2338.field_10980, state);
        chest.method_31662(client.field_1687);
        class_827<PunchyChestBlockEntity> renderer = dispatcher.method_3550(chest);
        if (renderer == null) {
            return;
        }
        ChestProxyTuning.Profile profile = ChestProxyTuning.get(chestKind != null ? chestKind : ChestProxyTuning.Kind.BOAT);
        if (profile == null) {
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
        applyBoatTransforms(boatRenderState, matrices, false);
        applyChestPose(boatRenderState, matrices);
        matrices.method_46416(profile.offX, profile.offY, profile.offZ);
        matrices.method_22907(class_7833.field_40714.rotationDegrees(profile.rotX));
        matrices.method_22907(class_7833.field_40716.rotationDegrees(profile.rotY));
        matrices.method_22907(class_7833.field_40718.rotationDegrees(profile.rotZ));
        matrices.method_22905(profile.scale, profile.scale, profile.scale);
        renderer.method_3569(chest, 0.0f, matrices, buffer, light, class_4608.field_21444, class_243.field_1353);
        matrices.method_22909();
    }

    private static ChestProxyTuning.Kind resolveChestProxyKind(class_1799 stack) {
        if (stack == null || stack.method_7960()) {
            return ChestProxyTuning.Kind.BOAT;
        }
        class_2960 id = class_7923.field_41178.method_10221(stack.method_7909());
        if (id == null) {
            return ChestProxyTuning.Kind.BOAT;
        }
        String path = id.method_12832();
        if (path.endsWith("_chest_raft")) {
            return ChestProxyTuning.Kind.RAFT;
        }
        return ChestProxyTuning.Kind.BOAT;
    }

    private static void applyBoatTransforms(class_10017 renderState, class_4587 matrices, boolean flip) {
        float f;
        if (renderState instanceof class_10004) {
            class_10004 boatState = (class_10004) renderState;
            f = boatState.field_53274;
        } else {
            f = 0.0f;
        }
        float yRot = f;
        matrices.method_46416(0.0f, 0.375f, 0.0f);
        matrices.method_22907(class_7833.field_40716.rotationDegrees(180.0f - yRot));
        if (flip) {
            matrices.method_22905(-1.0f, -1.0f, 1.0f);
        }
        matrices.method_22907(class_7833.field_40716.rotationDegrees(90.0f));
    }

    private static void applyChestPose(class_10017 renderState, class_4587 matrices) {
        if (renderState instanceof BoatLidState) {
            BoatLidState lidState = (BoatLidState) renderState;
            if (!lidState.punchy$isChestPoseValid()) {
                return;
            }
            float x = lidState.punchy$getChestX() / 16.0f;
            float y = lidState.punchy$getChestY() / 16.0f;
            float z = lidState.punchy$getChestZ() / 16.0f;
            matrices.method_46416(x, y, z);
            matrices.method_22907(class_7833.field_40714.rotation(lidState.punchy$getChestRotX()));
            matrices.method_22907(class_7833.field_40716.rotation(lidState.punchy$getChestRotY()));
            matrices.method_22907(class_7833.field_40718.rotation(lidState.punchy$getChestRotZ()));
        }
    }

    private static void stabilizePose(class_10255 boat) {
        if (boat == null) {
            return;
        }
        boat.method_5814(0.0d, 0.0d, 0.0d);
        boat.method_36456(180.0f);
        boat.method_36457(0.0f);
        boat.field_5982 = boat.method_36454();
        boat.field_6004 = boat.method_36455();
    }

    private static boolean shouldUseVanillaBoatRender(class_1799 stack) {
        class_2960 id;
        if (stack == null || stack.method_7960() || Services.PLATFORM == null || !Services.PLATFORM.isModLoaded(BERYL_MOD_ID) || (id = class_7923.field_41178.method_10221(stack.method_7909())) == null) {
            return false;
        }
        String path = id.method_12832();
        return path.endsWith("_boat") || path.endsWith("_chest_boat");
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/BoatInHandRenderer$PunchyChestBlockEntity.class */
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
