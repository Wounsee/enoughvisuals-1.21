package fun.enoughvisuals.systems.modules.settings;

import fun.enoughvisuals.utils.system.ClientLanguage;

public class StringSetting extends Setting<String> {

    public enum InputMode {
        ANY,
        DIGITS,
        LETTERS,
        ALPHANUMERIC;

        public boolean allows(char chr) {
            return switch (this) {
                case ANY -> true;
                case DIGITS -> Character.isDigit(chr);
                case LETTERS -> Character.isLetter(chr);
                case ALPHANUMERIC -> Character.isLetterOrDigit(chr);
            };
        }
    }

    private final int maxLength;
    private final String placeholder;
    private final InputMode inputMode;

    public StringSetting(String name, String id, String description,
                         String defaultValue, int maxLength, String placeholder, InputMode inputMode) {
        super(name, id, description, defaultValue);
        this.maxLength = maxLength;
        this.placeholder = placeholder;
        this.inputMode = inputMode == null ? InputMode.ANY : inputMode;
    }

    public StringSetting(String name, String id, String description,
                         String defaultValue, int maxLength, String placeholder) {
        this(name, id, description, defaultValue, maxLength, placeholder, InputMode.ANY);
    }

    public StringSetting(String name, String id, String defaultValue) {
        this(name, id, "", defaultValue, 256, "");
    }

    public StringSetting(String name, String id, String description, String defaultValue) {
        this(name, id, description, defaultValue, 256, "");
    }

    @Override
    protected String validate(String value) {
        if (value == null) return "";
        value = filterByInputMode(value);
        if (maxLength > 0 && value.length() > maxLength) {
            return value.substring(0, maxLength);
        }
        return value;
    }

    public boolean isEmpty() {
        return get().isEmpty();
    }

    public boolean isCharAllowed(char chr) {
        return inputMode.allows(chr);
    }

    private String filterByInputMode(String raw) {
        if (inputMode == InputMode.ANY || raw.isEmpty()) {
            return raw;
        }
        StringBuilder out = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char chr = raw.charAt(i);
            if (inputMode.allows(chr)) {
                out.append(chr);
            }
        }
        return out.toString();
    }

    public int getMaxLength() { return maxLength; }
    public String getPlaceholder() { return ClientLanguage.text("settings." + getOwnerModuleId() + "." + getId() + ".placeholder", placeholder); }
    public String getRawPlaceholder() { return placeholder; }
    public InputMode getInputMode() { return inputMode; }

    @Override
    public Object serialize() {
        return get();
    }

    @Override
    public void deserialize(Object data) {
        if (data instanceof String s) {
            set(s);
        }
    }
}
