package fun.enoughvisuals.systems.modules.api;

import fun.enoughvisuals.utils.system.ClientLanguage;

public enum Category {
    VISUAL("Визуалы", "Визуальные функции"),
    UTILS("Утилиты", "Полезные функции"),
    CLIENT("Клиент", "Настройки клиента");

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
