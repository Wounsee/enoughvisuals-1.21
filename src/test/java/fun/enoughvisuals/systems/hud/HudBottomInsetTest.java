package fun.enoughvisuals.systems.hud;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudBottomInsetTest {

    @AfterEach
    void reset() {
        HudBottomInset.resetForTest();
    }

    @Test
    void leavesTopElementsAtTheirSavedPosition() {
        assertEquals(0f, HudBottomInset.offsetFor(24f, 40f, 480f, 20f), 0.0001f);
    }

    @Test
    void appliesFullInsetToBottomAnchoredElements() {
        assertEquals(20f, HudBottomInset.offsetFor(440f, 20f, 480f, 20f), 0.0001f);
    }

    @Test
    void lowerScreenInfluenceIsContinuous() {
        float upper = HudBottomInset.offsetFor(310f, 20f, 480f, 20f);
        float middle = HudBottomInset.offsetFor(360f, 20f, 480f, 20f);
        float lower = HudBottomInset.offsetFor(420f, 20f, 480f, 20f);
        assertTrue(upper <= middle);
        assertTrue(middle <= lower);
        assertTrue(lower <= 20f);
    }

    @Test
    void anchorStrengthDependsOnBottomGapNotResolution() {
        float smallScreen = HudBottomInset.offsetFor(172f, 20f, 240f, 20f);
        float largeScreen = HudBottomInset.offsetFor(412f, 20f, 480f, 20f);
        assertEquals(smallScreen, largeScreen, 0.0001f);
    }

    @Test
    void openingAndClosingApproachTheirTargetsWithoutOvershoot() {
        float opened = HudBottomInset.advance(0f, 20f, 1f, 0.20f);
        float closing = HudBottomInset.advance(opened, 0f, 1f, 0.16f);
        assertTrue(opened > 0f && opened < 20f);
        assertTrue(closing >= 0f && closing < opened);
    }

    @Test
    void dragConversionRoundTripsVisualPosition() {
        HudBottomInset.beginFrame(true, 5f);
        float layoutY = 440f;
        float visualY = layoutY - HudBottomInset.offsetFor(layoutY, 20f, 480f);
        float restored = HudBottomInset.layoutYForVisual(visualY, 20f, 480f);
        assertEquals(layoutY, restored, 0.02f);
    }
}
