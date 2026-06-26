package fun.enoughvisuals.systems.modules.modules.client;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.network.RemoteConfig;
import net.minecraft.text.Text;

import java.util.Locale;
import java.util.Set;

@Register(name = "Globals", id = "globals", description = "Shows EnoughVisuals users in nametags and chat", category = Category.CLIENT)
public class Globals extends Module {
    public static final String LOGO_ICON = "\uF002";
    public static final int LOGO_COLOR = 0xFFC88DFF;
    public static final String LOGO_SPACES = "   ";

    public static Globals INSTANCE;

    public final BooleanSetting nametags = add(new BooleanSetting(
            "Nametags", "nametags", "Shows the logo near player nametags", true
    ));
    public final BooleanSetting chat = add(new BooleanSetting(
            "Chat", "chat", "Shows the logo near names in chat", true
    ));

    public Globals() {
        INSTANCE = this;
    }

    public static boolean nametagsEnabled() {
        return INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.nametags.get();
    }

    public static boolean chatEnabled() {
        return INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.chat.get();
    }

    public static boolean isEnoughUser(String username) {
        return username != null && RemoteConfig.getInstance().isEnoughUser(username);
    }

    public static Match findEnoughUser(String text) {
        if (text == null || text.isBlank()) return Match.NONE;
        Set<String> users = RemoteConfig.getInstance().getEnoughUsers();
        if (users == null || users.isEmpty()) return Match.NONE;

        String lowerText = text.toLowerCase(Locale.ROOT);
        Match best = Match.NONE;
        for (String user : users) {
            if (user == null || user.isBlank()) continue;
            String lowerUser = user.toLowerCase(Locale.ROOT);
            int from = 0;
            while (from < lowerText.length()) {
                int index = lowerText.indexOf(lowerUser, from);
                if (index < 0) break;
                int end = index + lowerUser.length();
                if (hasNameBoundary(text, index, end) && (best == Match.NONE || index < best.start())) {
                    best = new Match(user, index, end);
                    break;
                }
                from = Math.max(index + 1, end);
            }
        }
        return best;
    }

    public static Text withLogoSpace(Text text) {
        if (text == null) return text;
        Match match = findEnoughUser(text.getString());
        if (!match.found()) return text;

        String raw = text.getString();
        return Text.literal(raw.substring(0, match.start()))
                .append(LOGO_SPACES)
                .append(raw.substring(match.start()));
    }

    private static boolean hasNameBoundary(String text, int start, int end) {
        return isBoundary(text, start - 1) && isBoundary(text, end);
    }

    private static boolean isBoundary(String text, int index) {
        if (index < 0 || index >= text.length()) return true;
        char chr = text.charAt(index);
        return !Character.isLetterOrDigit(chr) && chr != '_';
    }

    public record Match(String username, int start, int end) {
        public static final Match NONE = new Match("", -1, -1);

        public boolean found() {
            return start >= 0 && end > start;
        }
    }
}
