package fun.enoughvisuals.ui.mainmenu;

import fun.enoughvisuals.utils.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public final class AnimatedFlagRenderer {
    private static final int FRAME_SIZE = 96;
    private static final int COLUMNS = 10;
    private static final int FRAME_COUNT = 60;
    private static final int ATLAS_WIDTH = FRAME_SIZE * COLUMNS;
    private static final int ATLAS_HEIGHT = FRAME_SIZE * 6;
    private static final long FRAME_NANOS = 1_000_000_000L / 60L;
    private static final Map<String, Identifier> TEXTURES = new HashMap<>();
    private static final Map<String, Boolean> FILTERED = new HashMap<>();
    private static final Map<String, Long> PLAY_ONCE = new HashMap<>();

    private AnimatedFlagRenderer() {
    }

    public static void playOnce(String asset) {
        if (asset == null || asset.isBlank()) return;
        PLAY_ONCE.put(asset, System.nanoTime());
    }

    public static void draw(DrawContext context, String asset, float x, float y, float size, float alpha) {
        if (asset == null || asset.isBlank()) return;
        Identifier texture = TEXTURES.computeIfAbsent(asset,
                key -> Identifier.of("enough", "textures/language_flags/" + key + ".png"));
        if (FILTERED.putIfAbsent(asset, Boolean.TRUE) == null) {
            MinecraftClient.getInstance().getTextureManager().getTexture(texture).setFilter(true, false);
        }
        int frame = FRAME_COUNT - 1;
        Long startedAt = PLAY_ONCE.get(asset);
        if (startedAt != null) {
            long elapsed = Math.max(0L, System.nanoTime() - startedAt);
            frame = (int) Math.min(FRAME_COUNT - 1L, elapsed / FRAME_NANOS);
            if (frame >= FRAME_COUNT - 1) PLAY_ONCE.remove(asset);
        }
        int sourceX = (frame % COLUMNS) * FRAME_SIZE;
        int sourceY = (frame / COLUMNS) * FRAME_SIZE;
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0f);
        context.drawTexture(RenderLayer::getGuiTextured, texture,
                0, 0, sourceX, sourceY,
                Math.round(size), Math.round(size), FRAME_SIZE, FRAME_SIZE,
                ATLAS_WIDTH, ATLAS_HEIGHT, RenderUtil.withAlpha(0xFFFFFFFF, alpha));
        context.getMatrices().pop();
    }
}
