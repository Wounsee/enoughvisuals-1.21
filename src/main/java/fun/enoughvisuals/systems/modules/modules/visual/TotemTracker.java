package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Register(name = "Totem Tracker", id = "totem_tracker", description = "Отслеживает и показывает количество попнутых тотемов игроков", category = Category.VISUAL)
public class TotemTracker extends Module {

    public static final byte STATUS_USE_TOTEM = 35;

    private static TotemTracker INSTANCE;

    public final SliderSetting radius = add(new SliderSetting("Радиус", "radius", "Максимальная дистанция обнаружения", 32.0, 8.0, 256.0, 1.0));
    public final BooleanSetting chatNotify = add(new BooleanSetting("Сообщения в чате", "chat_notify", "Писать в чат при потере тотема игроком", true));
    public final BooleanSetting nametagSuffix = add(new BooleanSetting("Надпись над головой", "nametag_suffix", "Показывать количество попнутых тотемов в нике", true));
    public final SliderSetting resetCooldownSeconds = add(new SliderSetting("Сброс таймера (сек)", "reset_cooldown_seconds", "Сбрасывать счетчик через N секунд бездействия (0 = никогда)", 0.0, 0.0, 600.0, 5.0));

    private final Map<String, Integer> losses = new ConcurrentHashMap<>();
    private final Map<String, Long> lastUseMs = new ConcurrentHashMap<>();

    public TotemTracker() {
        INSTANCE = this;
    }

    public static TotemTracker getInstance() {
        return INSTANCE;
    }

    public void recordTotemUse(LivingEntity entity) {
        if (!isEnabled() || entity == null) {
            return;
        }
        if (!(entity instanceof PlayerEntity player)) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return;
        }
        if (player == client.player) {
            return;
        }
        double maxDist = radius.get();
        double distSq = player.squaredDistanceTo(client.player);
        if (distSq > maxDist * maxDist) {
            return;
        }

        String name = player.getName().getString();
        if (name == null || name.isEmpty()) {
            return;
        }
        int newCount = losses.merge(name, 1, Integer::sum);
        lastUseMs.put(name, System.currentTimeMillis());

        if (chatNotify.get()) {
            MutableText line = Text.literal("[Enough] ").formatted(Formatting.GOLD)
                    .append(Text.literal(name).formatted(Formatting.WHITE))
                    .append(Text.literal(" потерял тотем ").formatted(Formatting.GRAY))
                    .append(Text.literal("(-" + newCount + ")").formatted(Formatting.RED));
            client.execute(() -> {
                if (client.inGameHud != null) {
                    client.inGameHud.getChatHud().addMessage(line);
                }
            });
        }
    }

    public int getLossCount(String name) {
        if (name == null) {
            return 0;
        }
        return losses.getOrDefault(name, 0);
    }

    public int getLossCountFromText(String displayed) {
        if (displayed == null || displayed.isEmpty() || losses.isEmpty()) {
            return 0;
        }
        for (Map.Entry<String, Integer> e : losses.entrySet()) {
            String key = e.getKey();
            if (key.isEmpty()) continue;
            if (displayed.contains(key)) {
                return e.getValue();
            }
        }
        return 0;
    }

    public boolean shouldDecorateNametag(String name) {
        return isEnabled() && nametagSuffix.get() && getLossCount(name) > 0;
    }

    public void pruneStaleEntries() {
        if (!isEnabled()) {
            return;
        }
        int cooldown = resetCooldownSeconds.get().intValue();
        if (cooldown <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        long windowMs = cooldown * 1000L;
        Iterator<Map.Entry<String, Long>> it = lastUseMs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> e = it.next();
            if (now - e.getValue() >= windowMs) {
                losses.remove(e.getKey());
                it.remove();
            }
        }
    }

    @Override
    public void onTick() {
        pruneStaleEntries();
    }

    public Map<String, Integer> snapshot() {
        return new HashMap<>(losses);
    }

    public void resetAll() {
        losses.clear();
        lastUseMs.clear();
    }

    @Override
    protected void onDisable() {
        resetAll();
    }
}
