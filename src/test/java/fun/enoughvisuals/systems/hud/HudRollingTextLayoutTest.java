package fun.enoughvisuals.systems.hud;

import org.junit.jupiter.api.Test;
import java.util.function.ToDoubleFunction;
import static org.junit.jupiter.api.Assertions.*;

class HudRollingTextLayoutTest {
    private static final ToDoubleFunction<String> WIDTH = text -> text.chars()
            .mapToDouble(c -> c == '1' ? 0.4 : c == '-' ? 0.3 : 0.7).sum();

    @Test
    void glyphsMatchNormalTextExactlyAtBothEnds() {
        for (String[] pair : new String[][]{{"99", "100"}, {"100", "99"},
                {"111", "888"}, {"888", "111"}, {"-99999", "1"}, {"9.9", "10.0"}}) {
            for (int end = 0; end <= 1; end++) {
                var layout = HudRollingText.layout(pair[0], pair[1], end, WIDTH);
                String text = pair[end];
                int padding = layout.positions().length - text.length();
                assertEquals(WIDTH.applyAsDouble(text), layout.width(), 0.00001);
                for (int i = 0; i < text.length(); i++) {
                    assertEquals(WIDTH.applyAsDouble(text.substring(0, i)),
                            layout.positions()[padding + i], 0.00001);
                }
            }
        }
    }

    @Test
    void lastDigitAndSuffixStayTogetherWhileDigitCountChanges() {
        for (float p : new float[]{0, 0.1f, 0.5f, 0.9f, 1}) {
            var layout = HudRollingText.layout("99", "100", p, WIDTH);
            assertEquals(layout.width(), layout.positions()[2] + 0.7f, 0.00001);
            assertEquals(0, layout.positions()[0], 0.00001);
            assertTrue(layout.positions()[1] >= 0);
        }
    }
}
