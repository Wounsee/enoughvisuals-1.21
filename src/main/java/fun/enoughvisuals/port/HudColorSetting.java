package fun.enoughvisuals.port;

import java.awt.Color;
import java.util.function.Supplier;

public class HudColorSetting extends ColorSetting {
    public HudColorSetting(String name, Color color) {
        super(name, 0.0F);
        invoke(color);
    }

    public HudColorSetting resolve(Supplier<Boolean> condition) {
        setVisibilityCondition(condition);
        return this;
    }
}
