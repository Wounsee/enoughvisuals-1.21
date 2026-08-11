package fun.enoughvisuals.systems.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudDesignSlotStyleTest {
    @Test
    void baseSlotKeepsInventoryGeometryAndDarkLayerOrder() {
        HudDesign.ItemSlotStyle style = HudDesign.itemSlotStyle(18f, 1f);

        assertEquals(18f, style.size(), 1.0e-6f);
        assertEquals(6.25f, style.radius(), 1.0e-6f);
        assertEquals(0.65f, style.inset(), 1.0e-6f);
        assertEquals(16.7f, style.insetSize(), 1.0e-5f);
        assertEquals(5.6f, style.insetRadius(), 1.0e-5f);
        assertEquals(0.72f, style.blurAlpha(), 1.0e-6f);
        assertEquals(0.34f, style.glassTintAlpha(), 1.0e-6f);
        assertEquals(0.90f, style.insetAlpha(), 1.0e-6f);
        float combinedDarkOpacity = 1f
                - (1f - style.glassTintAlpha()) * (1f - style.insetAlpha());
        assertEquals(0.934f, combinedDarkOpacity, 1.0e-6f);
        assertEquals(0.55f, style.outlineWidth(), 1.0e-6f);
        assertEquals(0.78f, style.outlineAlpha(), 1.0e-6f);
        assertEquals(0xFF121214, HudDesign.ITEM_SLOT_COLOR);
        assertTrue(style.radius() <= style.size() * 0.5f);
        assertTrue(style.insetRadius() <= style.insetSize() * 0.5f);
    }

    @Test
    void slotGeometryScalesUniformlyAndAlphaIsClamped() {
        HudDesign.ItemSlotStyle style = HudDesign.itemSlotStyle(36f, 2f);

        assertEquals(12.5f, style.radius(), 1.0e-6f);
        assertEquals(1.3f, style.inset(), 1.0e-6f);
        assertEquals(33.4f, style.insetSize(), 1.0e-5f);
        assertEquals(11.2f, style.insetRadius(), 1.0e-5f);
        assertEquals(1f, style.alpha(), 1.0e-6f);
        assertEquals(0.72f, style.blurAlpha(), 1.0e-6f);
    }
}
