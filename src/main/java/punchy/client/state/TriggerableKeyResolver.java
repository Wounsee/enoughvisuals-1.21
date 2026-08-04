package punchy.client.state;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/TriggerableKeyResolver.class */
final class TriggerableKeyResolver {
    private static final Map<String, Integer> CACHE = new HashMap();

    private TriggerableKeyResolver() {
    }

    static int resolveKeyCode(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            return -1;
        }
        String normalized = normalize(rawKey);
        if (normalized.isEmpty()) {
            return -1;
        }
        Integer cached = CACHE.get(normalized);
        if (cached != null) {
            return cached.intValue();
        }
        int resolved = resolveNormalized(normalized);
        CACHE.put(normalized, Integer.valueOf(resolved));
        return resolved;
    }

    static boolean matches(String rawKey, int keyCode) {
        return keyCode >= 0 && resolveKeyCode(rawKey) == keyCode;
    }

    private static String normalize(String rawKey) {
        String key = rawKey.trim().toUpperCase(Locale.ROOT);
        if (key.startsWith("KEY.KEYBOARD.")) {
            key = key.substring("KEY.KEYBOARD.".length());
        } else if (key.startsWith("KEY.KEYPAD.")) {
            key = key.substring("KEY.KEYPAD.".length());
        }
        return key.replace(" ", "").replace("-", "").replace("_", "");
    }

    private static int resolveNormalized(String key) {
        char c;
        if (key.length() == 1) {
            char c2 = key.charAt(0);
            if (c2 >= 'A' && c2 <= 'Z') {
                return 65 + (c2 - 'A');
            }
            if (c2 >= '0' && c2 <= '9') {
                return 48 + (c2 - '0');
            }
        }
        if (key.startsWith("F")) {
            String num = key.substring(1);
            try {
                int n = Integer.parseInt(num);
                if (n >= 1 && n <= 25) {
                    return 290 + (n - 1);
                }
            } catch (NumberFormatException e) {
            }
        }
        if (key.startsWith("NUMPAD") || key.startsWith("KP")) {
            String digits = key.replace("NUMPAD", "").replace("KP", "");
            if (digits.length() == 1 && (c = digits.charAt(0)) >= '0' && c <= '9') {
                return 320 + (c - '0');
            }
            if ("PLUS".equals(digits)) {
                return 334;
            }
            if ("MINUS".equals(digits)) {
                return 333;
            }
            if ("MULTIPLY".equals(digits)) {
                return 332;
            }
            if ("DIVIDE".equals(digits)) {
                return 331;
            }
            if ("DECIMAL".equals(digits) || "DOT".equals(digits)) {
                return 330;
            }
            if ("ENTER".equals(digits)) {
                return 335;
            }
        }
        switch (key) {
            case "ESC":
            case "ESCAPE":
                return 256;
            case "TAB":
                return 258;
            case "ENTER":
            case "RETURN":
                return 257;
            case "SPACE":
                return 32;
            case "BACKSPACE":
                return 259;
            case "INSERT":
            case "INS":
                return 260;
            case "DELETE":
            case "DEL":
                return 261;
            case "HOME":
                return 268;
            case "END":
                return 269;
            case "PAGEUP":
            case "PGUP":
                return 266;
            case "PAGEDOWN":
            case "PGDN":
                return 267;
            case "UP":
                return 265;
            case "DOWN":
                return 264;
            case "LEFT":
                return 263;
            case "RIGHT":
                return 262;
            case "LEFTSHIFT":
            case "LSHIFT":
                return 340;
            case "RIGHTSHIFT":
            case "RSHIFT":
                return 344;
            case "SHIFT":
                return 340;
            case "LEFTCTRL":
            case "LCTRL":
            case "CONTROL":
                return 341;
            case "RIGHTCTRL":
            case "RCTRL":
                return 345;
            case "LEFTALT":
            case "LALT":
            case "ALT":
                return 342;
            case "RIGHTALT":
            case "RALT":
                return 346;
            case "CAPSLOCK":
                return 280;
            case "SCROLLLOCK":
                return 281;
            case "NUMLOCK":
                return 282;
            case "PRINTSCREEN":
            case "PRTSCR":
            case "PRTSC":
                return 283;
            case "PAUSE":
                return 284;
            case "APOSTROPHE":
            case "QUOTE":
                return 39;
            case "COMMA":
                return 44;
            case "PERIOD":
            case "DOT":
                return 46;
            case "SLASH":
                return 47;
            case "SEMICOLON":
                return 59;
            case "EQUAL":
            case "PLUS":
                return 61;
            case "MINUS":
            case "DASH":
                return 45;
            case "LEFTBRACKET":
            case "LBRACKET":
                return 91;
            case "RIGHTBRACKET":
            case "RBRACKET":
                return 93;
            case "BACKSLASH":
                return 92;
            case "GRAVE":
            case "TILDE":
                return 96;
            default:
                return -1;
        }
    }
}
