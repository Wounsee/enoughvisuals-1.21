package fun.enoughvisuals.systems.config;

import fun.enoughvisuals.Info;
import fun.enoughvisuals.utils.font.TextGradientUtil;
import fun.enoughvisuals.utils.system.ClientLanguage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.util.List;

public final class CfgCommand {
    private CfgCommand() {}

    public static boolean handle(String chatText) {
        return handle(chatText, true);
    }

    public static boolean handle(String chatText, boolean addToHistory) {
        if (chatText == null) return false;
        String trimmed = chatText.trim();
        if (!trimmed.startsWith(".cfg")) return false;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (addToHistory && mc.inGameHud != null) {
            mc.inGameHud.getChatHud().addToMessageHistory(chatText);
        }

        ConfigSystem cfg = ConfigSystem.getInstance();
        cfg.initialize();

        String[] args = trimmed.split("\\s+");
        if (args.length == 1) {
            sendHelp();
            return true;
        }

        String sub = args[1].toLowerCase();
        switch (sub) {
            case "list" -> {
                List<String> configs = cfg.listConfigs();
                if (configs.isEmpty()) {
                    sendMessage(ClientLanguage.text("chat.cfg.empty", "Конфигов пока нет."));
                    return true;
                }

                sendMessage(ClientLanguage.text("chat.cfg.list", "Конфиги:"));
                String current = cfg.getLastConfigName();
                for (int i = 0; i < configs.size(); i++) {
                    String name = configs.get(i);
                    String marker = name.equalsIgnoreCase(current) ? " §a(active)" : "";
                    sendMessage(" §8" + (i + 1) + ". §d" + name + marker);
                }
                return true;
            }
            case "save" -> {
                String arg = joinFrom(args, 2);
                boolean ok = arg == null ? cfg.saveCurrent() : cfg.saveAs(arg);
                if (!ok) {
                    sendMessage(ClientLanguage.text("chat.cfg.save_fail", "§cНе удалось сохранить конфиг."));
                    return true;
                }
                String saved = arg == null ? cfg.getLastConfigName() : cfg.resolveNameOrIndex(arg);
                sendMessage(String.format(ClientLanguage.text("chat.cfg.saved", "Конфиг '§d%s§f' сохранен."), saved == null ? cfg.getLastConfigName() : saved));
                return true;
            }
            case "load" -> {
                String target = joinFrom(args, 2);
                if (target == null) {
                    sendMessage(ClientLanguage.text("chat.cfg.usage_load", "Использование: §f.cfg load <name|index>"));
                    return true;
                }
                String resolved = cfg.resolveNameOrIndex(target);
                if (resolved == null) {
                    sendMessage(ClientLanguage.text("chat.cfg.not_found", "§cКонфиг не найден."));
                    return true;
                }
                boolean ok = cfg.load(resolved);
                sendMessage(ok
                        ? String.format(ClientLanguage.text("chat.cfg.loaded", "Загружен конфиг '§d%s§f'."), resolved)
                        : ClientLanguage.text("chat.cfg.load_fail", "§cНе удалось загрузить конфиг."));
                return true;
            }
            case "remove" -> {
                String target = joinFrom(args, 2);
                if (target == null) {
                    sendMessage(ClientLanguage.text("chat.cfg.usage_remove", "Использование: §f.cfg remove <name|index>"));
                    return true;
                }
                String resolved = cfg.resolveNameOrIndex(target);
                if (resolved == null) {
                    sendMessage(ClientLanguage.text("chat.cfg.not_found", "§cКонфиг не найден."));
                    return true;
                }
                boolean ok = cfg.remove(resolved);
                sendMessage(ok
                        ? String.format(ClientLanguage.text("chat.cfg.removed", "Конфиг '§d%s§f' удален."), resolved)
                        : ClientLanguage.text("chat.cfg.remove_fail", "§cНе удалось удалить конфиг."));
                return true;
            }
            case "dir" -> {
                try {
                    Util.getOperatingSystem().open(cfg.getConfigDirectory().toFile());
                    sendMessage(String.format(ClientLanguage.text("chat.cfg.dir_opened", "Открыта папка конфигов: §d%s"), cfg.getConfigDirectory().toAbsolutePath()));
                } catch (Exception e) {
                    sendMessage(ClientLanguage.text("chat.cfg.dir_fail", "§cНе удалось открыть папку конфигов."));
                }
                return true;
            }
            default -> {
                sendHelp();
                return true;
            }
        }
    }

    private static String joinFrom(String[] args, int start) {
        if (args.length <= start) return null;
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (i > start) builder.append(' ');
            builder.append(args[i]);
        }
        String out = builder.toString().trim();
        return out.isEmpty() ? null : out;
    }

    private static void sendHelp() {
        sendMessage(ClientLanguage.text("chat.cfg.help", "Помощь по конфигам:"));
        sendMessage(ClientLanguage.text("chat.cfg.help_list", " §d.cfg list §f- список конфигов"));
        sendMessage(ClientLanguage.text("chat.cfg.help_save", " §d.cfg save [name] §f- сохранить текущий"));
        sendMessage(ClientLanguage.text("chat.cfg.help_load", " §d.cfg load <name|index> §f- загрузить конфиг"));
        sendMessage(ClientLanguage.text("chat.cfg.help_remove", " §d.cfg remove <name|index> §f- удалить конфиг"));
        sendMessage(ClientLanguage.text("chat.cfg.help_dir", " §d.cfg dir §f- открыть папку конфигов"));
    }

    private static void sendMessage(String text) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.inGameHud == null) return;

        MutableText prefix = TextGradientUtil.gradient(Info.CHAT_PREFIX, 0x6D3BFF, 0xD2D5FF);
        MutableText message = Text.literal(text).setStyle(Style.EMPTY.withColor(0xFFFFFF));
        mc.inGameHud.getChatHud().addMessage(prefix.append(message));
    }
}
