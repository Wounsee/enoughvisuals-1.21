package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.friends.FriendRepo;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.ColorSetting;
import fun.enoughvisuals.utils.render.CrystalRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

@Register(
        name = "Friend Marker",
        id = "friend_marker",
        description = "Кристалл над друзьями",
        category = Category.VISUAL
)
public class FriendMarker extends Module {

    private static FriendMarker INSTANCE;

    public final ColorSetting color = add(new ColorSetting(
            "Цвет",
            "color",
            "Цвет маркера друзей",
            0xFF68D8FF
    ));

    public FriendMarker() {
        INSTANCE = this;
    }

    public static void renderInWorld(WorldRenderContext context) {
        if (INSTANCE == null || !INSTANCE.isEnabled()) return;
        if (mc.player == null || mc.world == null) return;
        if (context.matrixStack() == null || context.tickCounter() == null) return;

        float tickDelta = context.tickCounter().getTickDelta(true);
        int colorA = INSTANCE.color.get() & 0x00FFFFFF;
        int colorB = CrystalRenderer.mixColor(colorA, 0xFFFFFF, 0.45f);
        Vec3d camera = context.camera() == null ? mc.player.getPos() : context.camera().getPos();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == null || player == mc.player) continue;
            if (!player.isAlive() || player.isRemoved()) continue;
            if (player.isInvisible() || player.isInvisibleTo(mc.player)) continue;
            if (camera.squaredDistanceTo(player.getPos()) > 96.0 * 96.0) continue;
            Box renderBox = player.getBoundingBox().expand(0.35, 1.2, 0.35);
            if (context.frustum() != null && !context.frustum().isVisible(renderBox)) continue;
            if (!FriendRepo.isFriend(player.getName().getString())) continue;

            CrystalRenderer.renderMarkerAboveHead(
                    context.matrixStack(),
                    player,
                    1.0f,
                    tickDelta,
                    colorA,
                    colorB,
                    0.95f,
                    0.32f
            );
        }
    }
}
