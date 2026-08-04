package punchy.client.render;

import java.util.EnumMap;
import net.minecraft.class_1268;
import net.minecraft.class_1747;
import net.minecraft.class_1799;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2336;
import net.minecraft.class_2338;
import net.minecraft.class_2343;
import net.minecraft.class_243;
import net.minecraft.class_2531;
import net.minecraft.class_2586;
import net.minecraft.class_2595;
import net.minecraft.class_2611;
import net.minecraft.class_2646;
import net.minecraft.class_2680;
import net.minecraft.class_310;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_4608;
import net.minecraft.class_4739;
import net.minecraft.class_638;
import net.minecraft.class_824;
import net.minecraft.class_827;
import punchy.compat.EmfEtfCemCompat;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/ChestInHandRenderer.class */
public final class ChestInHandRenderer {
    private static final class_2338 ORIGIN = class_2338.field_10980;
    private static final EnumMap<class_1268, Float> LID_OPEN = new EnumMap<>(class_1268.class);
    private static final EnumMap<class_1268, Long> LID_LAST_UPDATE = new EnumMap<>(class_1268.class);

    private ChestInHandRenderer() {
    }

    public static boolean isChestStack(class_1799 stack) {
        return isChest(stack);
    }

    public static boolean tryRender(class_1799 stack, class_1268 hand, class_4587 matrices, class_4597 buffer, int light) {
        class_2680 state;
        class_2586 blockEntity;
        class_310 client;
        class_827<class_2586> renderer;
        float open;
        if (!isChest(stack) || (state = chestState(stack)) == null || (blockEntity = createBlockEntity(state)) == null || (client = class_310.method_1551()) == null) {
            return false;
        }
        class_638 class_638Var = client.field_1687;
        if (class_638Var != null) {
            blockEntity.method_31662(class_638Var);
        }
        class_824 dispatcher = client.method_31975();
        if (dispatcher == null || (renderer = dispatcher.method_3550(blockEntity)) == null) {
            return false;
        }
        EmfEtfCemCompat.ProxyPhysicsTarget proxyTarget = EmfEtfCemCompat.resolveChestTarget(state.method_26204());
        boolean disablePhysics = proxyTarget != null && EmfEtfCemCompat.disableProxyPhysics(proxyTarget);
        if (disablePhysics) {
            open = 0.0f;
        } else {
            float target = InHandLidPhysics.computeFallTarget(client.field_1724);
            open = InHandLidPhysics.updateLid(LID_OPEN, LID_LAST_UPDATE, hand, target);
        }
        if (blockEntity instanceof PunchyChestBlockEntity) {
            PunchyChestBlockEntity chest = (PunchyChestBlockEntity) blockEntity;
            chest.punchy$setOpen(open);
        } else if (blockEntity instanceof PunchyTrappedChestBlockEntity) {
            PunchyTrappedChestBlockEntity trapped = (PunchyTrappedChestBlockEntity) blockEntity;
            trapped.punchy$setOpen(open);
        } else if (blockEntity instanceof PunchyEnderChestBlockEntity) {
            PunchyEnderChestBlockEntity ender = (PunchyEnderChestBlockEntity) blockEntity;
            ender.punchy$setOpen(open);
        }
        ChestInHandContext.push();
        try {
            renderer.method_3569(blockEntity, 0.0f, matrices, buffer, light, class_4608.field_21444, class_243.field_1353);
            return true;
        } finally {
            ChestInHandContext.pop();
        }
    }

    private static boolean isChest(class_1799 stack) {
        if (stack == null || stack.method_7960()) {
            return false;
        }
        class_1747 class_1747VarMethod_7909 = stack.method_7909();
        if (class_1747VarMethod_7909 instanceof class_1747) {
            class_1747 blockItem = class_1747VarMethod_7909;
            class_2248 block = blockItem.method_7711();
            return (block instanceof class_4739) || (block instanceof class_2336) || (block instanceof class_2531);
        }
        return false;
    }

    private static class_2680 chestState(class_1799 stack) {
        class_1747 class_1747VarMethod_7909 = stack.method_7909();
        if (!(class_1747VarMethod_7909 instanceof class_1747)) {
            return null;
        }
        class_1747 blockItem = class_1747VarMethod_7909;
        class_2248 block = blockItem.method_7711();
        return block.method_9564();
    }

    private static class_2586 createBlockEntity(class_2680 state) {
        class_2343 class_2343VarMethod_26204 = state.method_26204();
        if (class_2343VarMethod_26204 == class_2246.field_10443) {
            return new PunchyEnderChestBlockEntity(ORIGIN, state);
        }
        if (class_2343VarMethod_26204 == class_2246.field_10380) {
            return new PunchyTrappedChestBlockEntity(ORIGIN, state);
        }
        if (class_2343VarMethod_26204 == class_2246.field_10034) {
            return new PunchyChestBlockEntity(ORIGIN, state);
        }
        if (class_2343VarMethod_26204 instanceof class_2343) {
            class_2343 entityBlock = class_2343VarMethod_26204;
            return entityBlock.method_10123(ORIGIN, state);
        }
        return null;
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/ChestInHandRenderer$PunchyChestBlockEntity.class */
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

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/ChestInHandRenderer$PunchyTrappedChestBlockEntity.class */
    private static final class PunchyTrappedChestBlockEntity extends class_2646 {
        private float punchy$open;

        private PunchyTrappedChestBlockEntity(class_2338 pos, class_2680 state) {
            super(pos, state);
        }

        private void punchy$setOpen(float open) {
            this.punchy$open = open;
        }

        public float method_11274(float partialTicks) {
            return this.punchy$open;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/ChestInHandRenderer$PunchyEnderChestBlockEntity.class */
    private static final class PunchyEnderChestBlockEntity extends class_2611 {
        private float punchy$open;

        private PunchyEnderChestBlockEntity(class_2338 pos, class_2680 state) {
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
