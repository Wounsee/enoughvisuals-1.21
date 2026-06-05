package fun.enoughvisuals.systems.modules.api;

import fun.enoughvisuals.utils.system.ClientLanguage;

public enum Category {
    VISUAL("Visuals", "Визуалы"),
    UTILS("Utils", "Утилиты"),
    CUSTOM("Client", "Client");

    private final String displayName;
    private final String description;

    Category(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return ClientLanguage.categoryName(name().toLowerCase(), displayName);
    }

    public String getDescription() {
        return ClientLanguage.categoryDescription(name().toLowerCase(), description);
    }
}
