package fun.enoughvisuals.systems.cosmetic.pulse;

import fun.enoughvisuals.systems.cosmetic.CosmeticCatalog;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;

import java.util.Locale;
import java.util.Set;

/**
 * Draws the equipped Pulse pet beside its owner.
 *
 * <p>The bundled Pulse renderer anchors pets to the player's eye height and offsets them by the
 * authored model origin, which leaves most of them buried in the player or floating far away. This
 * replacement measures the model instead and stands it on the ground next to the owner.
 */
public final class PulsePetWorldRenderer {
    /** Sideways and backwards placement of the pet's footprint, in blocks. */
    private static final float SIDE_OFFSET = -0.75f;
    private static final float BACK_OFFSET = -0.35f;
    private static final float HOVER = 0.1f;
    private static final float BOB_AMPLITUDE = 0.045f;
    private static final float BOB_SPEED = 1.6f;
    private static final double MOVING_SPEED_SQ = 4.0e-4;
    private static final long START_NANOS = System.nanoTime();
    /** Pets that hover next to the owner's shoulder instead of standing on the ground. */
    private static final Set<String> SHOULDER_PETS = Set.of("angel", "bee", "demon", "diamond");
    private static final float SHOULDER_HEIGHT = 1.35f;
    private static final float SHOULDER_SIDE_OFFSET = -0.62f;
    private static final float SHOULDER_BACK_OFFSET = -0.18f;
    /** Catch-up rates, per second, of the pet chasing its anchor point. */
    private static final float FOLLOW_SPEED = 6.5f;
    private static final float TURN_SPEED = 5.0f;

    private static Vec3d smoothedPos;
    private static float smoothedYaw;
    private static long lastFrameNanos;

    private PulsePetWorldRenderer() {}

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(PulsePetWorldRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null || context.matrixStack() == null) return;
        if (player.isInvisible() || player.isSpectator()) return;

        CosmeticCatalog.Entry pet = CosmeticCatalog.equipped(CosmeticCatalog.Kind.PET);
        if (pet == null || !pet.isPulse()) {
            smoothedPos = null;
            return;
        }
        PulseModelGeometry.Bounds bounds = PulseModelGeometry.bounds(pet.pulseId(), true);
        boolean shoulder = SHOULDER_PETS.contains(
                pet.name() == null ? "" : pet.name().trim().toLowerCase(Locale.ROOT));

        float tickDelta = context.tickCounter() == null ? 1f : context.tickCounter().getTickDelta(false);
        Vec3d position = player.getLerpedPos(tickDelta);
        Vec3d camera = client.gameRenderer.getCamera().getPos();
        float seconds = (float) ((System.nanoTime() - START_NANOS) / 1.0e9);
        float bob = BOB_AMPLITUDE * (1f + MathHelper.sin(seconds * BOB_SPEED));
        // Pulse models carry a baked displacement from their root pivot, so the measured
        // box is what has to be placed rather than the model origin.
        float x = (shoulder ? SHOULDER_SIDE_OFFSET : SIDE_OFFSET) - (bounds == null ? 0f : bounds.centerX());
        float y = shoulder
                ? SHOULDER_HEIGHT + bob - (bounds == null ? 0f : bounds.centerY())
                : HOVER + bob - (bounds == null ? 0f : bounds.minY());
        float z = (shoulder ? SHOULDER_BACK_OFFSET : BACK_OFFSET) - (bounds == null ? 0f : bounds.centerZ());

        // Chase the anchor instead of being welded to the player: a small delay reads far
        // more alive than a rigid attachment.
        float targetYaw = player.getLerpedYaw(tickDelta);
        double radians = Math.toRadians(-targetYaw);
        double sin = Math.sin(radians);
        double cos = Math.cos(radians);
        Vec3d anchor = position.add(x * cos + z * sin, y, -x * sin + z * cos);
        float dt = frameSeconds();
        if (smoothedPos == null || smoothedPos.squaredDistanceTo(anchor) > 64.0) {
            smoothedPos = anchor;
            smoothedYaw = targetYaw;
        } else {
            smoothedPos = smoothedPos.lerp(anchor, catchUp(FOLLOW_SPEED, dt));
            smoothedYaw = MathHelper.lerpAngleDegrees(catchUp(TURN_SPEED, dt), smoothedYaw, targetYaw);
        }
        Vec3d render = smoothedPos;

        MatrixStack matrices = context.matrixStack();
        matrices.push();
        try {
            matrices.translate(render.x - camera.x, render.y - camera.y, render.z - camera.z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-smoothedYaw));
            VertexConsumerProvider consumers = context.consumers();
            if (consumers == null) return;
            PulseCosmeticBridge.renderPet(pet.pulseId(), matrices, consumers,
                    light(client, position), player.isGliding(), player.isSwimming(),
                    player.isSneaking(), isMoving(player), seconds);
        } catch (RuntimeException ignored) {
        } finally {
            matrices.pop();
        }
    }

    /** Frame-rate independent catch-up factor for an exponential follow. */
    private static float catchUp(float speed, float dt) {
        return MathHelper.clamp(1f - (float) Math.exp(-speed * dt), 0f, 1f);
    }

    private static float frameSeconds() {
        long now = System.nanoTime();
        long previous = lastFrameNanos;
        lastFrameNanos = now;
        if (previous == 0L) return 1f / 60f;
        return MathHelper.clamp((float) ((now - previous) / 1.0e9), 0f, 0.25f);
    }

    private static boolean isMoving(ClientPlayerEntity player) {
        double dx = player.getX() - player.prevX;
        double dz = player.getZ() - player.prevZ;
        return dx * dx + dz * dz >= MOVING_SPEED_SQ;
    }

    private static int light(MinecraftClient client, Vec3d position) {
        if (client.world == null) return LightmapTextureManager.MAX_LIGHT_COORDINATE;
        BlockPos pos = BlockPos.ofFloored(position);
        int block = client.world.getLightLevel(LightType.BLOCK, pos);
        int sky = client.world.getLightLevel(LightType.SKY, pos);
        return LightmapTextureManager.pack(block, sky);
    }
}
