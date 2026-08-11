package fun.enoughvisuals.systems.hud;

/**
 * Tracks how far down the vanilla HUD occupies the top-centre strip.
 *
 * <p>Boss bars and the player list both live there, and so do our own widgets.
 * Vanilla draws them during {@code InGameHud.render}, while client widgets draw
 * at its tail, so a value reported this frame is always ready before anything
 * needs to read it.
 */
public final class HudTopInset {

    private static float bossBarBottom;
    private static float playerListBottom;
    private static float playerListOriginY;
    private static float playerListScale = 1f;

    private HudTopInset() {}

    public static void beginFrame() {
        bossBarBottom = 0f;
        playerListBottom = 0f;
    }

    public static void reportBossBarBottom(float bottom) {
        bossBarBottom = Math.max(bossBarBottom, bottom);
    }

    /**
     * Declares the transform applied around the player list, so its own
     * reported bounds can be mapped onto the screen. Must be paired with
     * {@link #clearPlayerListTransform()}.
     */
    public static void setPlayerListTransform(float originY, float scale) {
        playerListOriginY = originY;
        playerListScale = scale;
    }

    public static void clearPlayerListTransform() {
        playerListOriginY = 0f;
        playerListScale = 1f;
    }

    /** Takes the player list bottom in its own coordinate space. */
    public static void reportPlayerListBottom(float bottomInListSpace) {
        float screenBottom = playerListOriginY + bottomInListSpace * playerListScale;
        playerListBottom = Math.max(playerListBottom, screenBottom);
    }

    /** Lowest point the vanilla top-centre HUD reached this frame. */
    public static float occupiedBottom() {
        return Math.max(bossBarBottom, playerListBottom);
    }
}
