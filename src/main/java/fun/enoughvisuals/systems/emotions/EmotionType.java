package fun.enoughvisuals.systems.emotions;

import net.minecraft.util.Identifier;

public enum EmotionType {
    DEB("deb", "Дэб", 2500L),
    FLOSS("floss", "Флосс", 4500L),
    MASTURBATE("masturbate", "Мастурбация", 3500L),
    HELLO("hello", "Приветствие", 2500L),
    GET_GRIDDY("get_griddy", "Get Griddy", 5500L),
    HAPPY("happy", "Радость", 2500L);

    private final String name;
    private final String title;
    private final long durationMs;
    private final Identifier texture;

    EmotionType(String name, String title, long durationMs) {
        this.name = name;
        this.title = title;
        this.durationMs = durationMs;
        this.texture = Identifier.of("enough", "textures/emotions/" + name + ".png");
    }

    public String title() { return title; }
    public String wireName() { return name; }
    public long durationMs() { return durationMs; }
    public Identifier texture() { return texture; }

    public static EmotionType fromWireName(String value) {
        if (value == null) return null;
        for (EmotionType type : values()) if (type.name.equals(value)) return type;
        return null;
    }
}
