package fun.enoughvisuals.utils.sound;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class EnoughSoundPlayer {
    private EnoughSoundPlayer() {}

    public static void playModuleToggle(boolean on) {
        playCustom(on ? "ui.en" : "ui.dis", 0.9f, 1.0f);
    }

    public static void playScroll() {
        playCustom("ui.scroll", 0.75f, 1.0f);
    }

    public static void playSlider() {
        playCustom("ui.slider", 0.75f, 1.0f);
    }

    public static void playWarn() {
        playCustom("ui.warn", 0.9f, 1.0f);
    }

    public static void playEvent() {
        playCustom("ui.event", 0.95f, 1.0f);
    }

    public static void playMenuOpen() {
        playCustom("ui.menu_open", 0.9f, 1.0f);
    }

    public static void playMenuClose() {
        playCustom("ui.menu_close", 0.9f, 1.0f);
    }

    public static void playHit(String style, float volume) {
        String key = switch ((style == null ? "" : style).toLowerCase()) {
            case "bell" -> "hit.bell";
            case "magic" -> "hit.magic";
            case "run" -> "hit.run";
            case "pew" -> "hit.pew";
            default -> "hit.bell";
        };
        playCustom(key, volume, 1.0f);
    }

    public static void playKill(float volume) {
        playCustom("kill.kill", volume, 1.0f);
    }

    private static void playCustom(String soundPath, float volume, float pitch) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getSoundManager() == null) return;

        Identifier id = Identifier.of("enough", soundPath);
        SoundEvent event = SoundEvent.of(id);
        float v = Math.max(0.0f, Math.min(1.0f, volume));
        float p = Math.max(0.5f, Math.min(2.0f, pitch));

        client.getSoundManager().play(PositionedSoundInstance.master(event, p, v));
    }
}
