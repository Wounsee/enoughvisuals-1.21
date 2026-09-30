package fun.enoughvisuals.utils.font;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public class TextGradientUtil {

    public static MutableText gradient(String text, int hexStart, int hexEnd) {
        MutableText result = Text.empty();
        int len = text.length();

        int r1 = (hexStart >> 16) & 0xFF;
        int g1 = (hexStart >> 8) & 0xFF;
        int b1 = hexStart & 0xFF;

        int r2 = (hexEnd >> 16) & 0xFF;
        int g2 = (hexEnd >> 8) & 0xFF;
        int b2 = hexEnd & 0xFF;

        for (int i = 0; i < len; i++) {

            float ratio = len > 1 ? (float) i / (len - 1) : 0;

            int r = (int) (r1 + (r2 - r1) * ratio);
            int g = (int) (g1 + (g2 - g1) * ratio);
            int b = (int) (b1 + (b2 - b1) * ratio);

            int stepColor = (r << 16) | (g << 8) | b;

            result.append(Text.literal(String.valueOf(text.charAt(i)))
                    .setStyle(Style.EMPTY.withColor(stepColor)));
        }
        return result;
    }
}
