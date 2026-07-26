package fun.enoughvisuals.port;

import java.awt.Color;

public enum Theme {
    PORTED, CUSTOM, ASTOLFO_RAINBOW, LAGUNE_RAINBOW, HALF_RAINBOW,
    AURORA_RAINBOW, NEON_RAINBOW, BLOSSOM_RAINBOW, ABYSS_RAINBOW,
    SUNSET_RAINBOW, GLACIER_RAINBOW, CHROMA_RAINBOW, DREAM_RAINBOW,
    TOXIC_RAINBOW;

    public Color getColor() {
        return new Color(fun.enoughvisuals.systems.modules.modules.client.CustomTheme.hudAccent(), true);
    }
}
