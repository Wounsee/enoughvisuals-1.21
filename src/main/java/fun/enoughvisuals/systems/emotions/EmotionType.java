package fun.enoughvisuals.systems.emotions;

/**
 * Stable public identifiers for the Pulse emote pack.
 *
 * <p>The actual keyframes live in {@code assets/enough/emotions/pulse}; keeping
 * the wire id separate from the resource id lets the numeric "67" id remain
 * compatible with sync while still using a filesystem-friendly directory.</p>
 */
public enum EmotionType {
    ORANGE_JUSTICE("orange_justice", "orange_justice", "Orange Justice", 130, true),
    SIXTY_SEVEN("67", "67", "67", 46, false),
    FLOSS("floss", "floss", "Floss", 32, true),
    GANGNAM_STYLE("gangnam_style", "gangnam_style", "Gangnam Style", 18, true),
    SQUAT_KICK("squat_kick", "squat_kick", "Гопак", 232, true),
    GET_GRIDDY("get_griddy", "get_griddy", "Get Griddy", 121, true),
    NOWKIE_BAKE("nowkie_bake", "nowkie_bake", "Новки", 290, false),
    CRYING("crying", "crying", "Плач", 27, true),
    HYPE("hype", "hype", "Победный танец", 68, true),
    SCUBA("scuba", "scuba", "Scuba", 46, true),
    TWERK("twerk", "twerk", "Твёрк", 14, true),
    TOPLESS("topless", "topless", "Топлес", 15, true),
    L_DANCE("l_dance", "l_dance", "Fortnite", 16, true);

    public static final int FRAMES_PER_SECOND = 20;

    private final String wireName;
    private final String resourceId;
    private final String title;
    private final int durationFrames;
    private final boolean looping;

    EmotionType(String wireName, String resourceId, String title, int durationFrames, boolean looping) {
        this.wireName = wireName;
        this.resourceId = resourceId;
        this.title = title;
        this.durationFrames = durationFrames;
        this.looping = looping;
    }

    public String title() { return title; }
    public String wireName() { return wireName; }
    public String resourceId() { return resourceId; }
    public int durationFrames() { return durationFrames; }
    public long durationMs() { return durationFrames * 1000L / FRAMES_PER_SECOND; }
    public boolean looping() { return looping; }

    public static EmotionType fromWireName(String value) {
        if (value == null) return null;
        for (EmotionType type : values()) {
            if (type.wireName.equals(value) || type.resourceId.equals(value)) return type;
        }
        return null;
    }
}
