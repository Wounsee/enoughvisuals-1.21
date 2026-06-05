package fun.enoughvisuals.utils.render;

import fun.enoughvisuals.systems.friends.FriendRepo;
import fun.enoughvisuals.systems.modules.modules.visual.Pet;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class PetRenderer {

    private static final Map<Integer, Vec3d> SMOOTH_OFFSETS = new HashMap<>();
    private static long lastCleanupMs = 0L;

    private PetRenderer() {
    }

    public static void renderWorldPet(int stateId, EntityPose pose, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        Pet module = Pet.active();
        MinecraftClient client = MinecraftClient.getInstance();
        if (module == null || client.player == null || client.world == null) {
            SMOOTH_OFFSETS.clear();
            return;
        }

        if (!module.creature.is("Allay")) {
            return;
        }

        Entity entity = client.world.getEntityById(stateId);
        if (!(entity instanceof PlayerEntity player)) {
            return;
        }
        if (player != client.player && player.squaredDistanceTo(client.player) > 64.0 * 64.0) {
            return;
        }
        if (!shouldRenderFor(player, client.player)) {
            return;
        }

        float tickDelta = client.getRenderTickCounter().getTickDelta(true);
        float age = client.player.age + tickDelta;
        if (pose == null) {
            pose = EntityPose.STANDING;
        }

        float bodyYaw = MathHelper.lerp(tickDelta, player.prevBodyYaw, player.bodyYaw);
        float yaw = (float) Math.toRadians(bodyYaw);
        float hover = MathHelper.sin(age * 0.22f) * 0.11f;
        float orbit = MathHelper.sin(age * 0.08f) * 0.10f;
        float height = pose == EntityPose.CROUCHING ? 1.18f : 1.52f;
        float leftDistance = 0.92f + orbit;
        float backDistance = 0.30f;
        float offsetX = -MathHelper.cos(yaw) * leftDistance + MathHelper.sin(yaw) * backDistance;
        float offsetZ = -MathHelper.sin(yaw) * leftDistance - MathHelper.cos(yaw) * backDistance;
        Vec3d targetOffset = new Vec3d(offsetX, height + hover, offsetZ);

        Vec3d smoothOffset = SMOOTH_OFFSETS.get(stateId);
        smoothOffset = smoothOffset == null ? targetOffset : smoothOffset.lerp(targetOffset, 0.18D);
        SMOOTH_OFFSETS.put(stateId, smoothOffset);
        cleanupOffsets(client);

        float sway = MathHelper.sin(age * 0.16f) * 9.0f;
        float roll = MathHelper.cos(age * 0.18f) * 5.5f;
        float scale = 0.35f * module.size.get().floatValue();

        matrices.push();
        matrices.translate(smoothOffset.x, smoothOffset.y, smoothOffset.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-bodyYaw + sway));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(roll));
        matrices.scale(scale, scale, scale);
        CustomModelRenderer.renderModel(
                module.modelPath(),
                new Matrix4f(matrices.peek().getPositionMatrix()),
                vertexConsumers,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                age
        );
        matrices.pop();

        if (vertexConsumers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw();
        }
    }

    private static boolean shouldRenderFor(PlayerEntity player, PlayerEntity self) {
        if (player == null || self == null) {
            return false;
        }

        return player == self || FriendRepo.isFriend(player.getName().getString());
    }

    private static void cleanupOffsets(MinecraftClient client) {
        long now = System.currentTimeMillis();
        if (client.world == null || SMOOTH_OFFSETS.size() < 2 || now - lastCleanupMs < 1000L) {
            return;
        }
        lastCleanupMs = now;

        Iterator<Integer> iterator = SMOOTH_OFFSETS.keySet().iterator();
        while (iterator.hasNext()) {
            int id = iterator.next();
            Entity entity = client.world.getEntityById(id);
            if (!(entity instanceof PlayerEntity player) || !shouldRenderFor(player, client.player)) {
                iterator.remove();
            }
        }
    }

}
