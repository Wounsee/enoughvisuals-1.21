package fun.enoughvisuals.port;

import java.util.function.Supplier;

public class Setting {
    public String name;
    public Supplier<Boolean> visibilityCondition = () -> false;
}
