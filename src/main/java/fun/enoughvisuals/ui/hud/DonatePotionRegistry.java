package fun.enoughvisuals.ui.hud;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.entity.effect.StatusEffectInstance;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

final class DonatePotionRegistry {
    record Match(String name, int durationTicks) {
    }

    private record RequiredEffect(List<String> names, int minLevel) {
    }

    private record Rule(String name, List<RequiredEffect> effects) {
    }

    private static final List<Rule> RULES = load();

    private DonatePotionRegistry() {
    }

    static Match match(Collection<StatusEffectInstance> active) {
        Rule best = null;
        int bestDuration = 0;
        for (Rule rule : RULES) {
            int duration = Integer.MAX_VALUE;
            boolean matches = true;
            for (RequiredEffect required : rule.effects()) {
                StatusEffectInstance found = find(active, required);
                if (found == null) {
                    matches = false;
                    break;
                }
                duration = Math.min(duration, found.getDuration());
            }
            if (matches && (best == null || rule.effects().size() > best.effects().size())) {
                best = rule;
                bestDuration = duration;
            }
        }
        return best == null ? null : new Match(best.name(), bestDuration);
    }

    private static StatusEffectInstance find(Collection<StatusEffectInstance> active, RequiredEffect required) {
        for (StatusEffectInstance effect : active) {
            String translated = effect.getEffectType().value().getName().getString();
            String raw = effect.getEffectType().value().getTranslationKey();
            boolean nameMatches = required.names().stream().anyMatch(name ->
                    name.equalsIgnoreCase(translated)
                            || raw.toLowerCase().contains(name.toLowerCase().replace(" ", "_")));
            if (nameMatches && effect.getAmplifier() + 1 >= required.minLevel()) return effect;
        }
        return null;
    }

    private static List<Rule> load() {
        String path = "/assets/enough/data/donate_potions.json";
        try (InputStream stream = DonatePotionRegistry.class.getResourceAsStream(path)) {
            if (stream == null) return List.of();
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            List<Rule> result = new ArrayList<>();
            for (JsonElement potionElement : root.getAsJsonArray("potions")) {
                JsonObject potion = potionElement.getAsJsonObject();
                List<RequiredEffect> requirements = new ArrayList<>();
                for (JsonElement effectElement : potion.getAsJsonArray("effects")) {
                    JsonObject effect = effectElement.getAsJsonObject();
                    JsonArray namesJson = effect.getAsJsonArray("name");
                    List<String> names = new ArrayList<>();
                    namesJson.forEach(name -> names.add(name.getAsString()));
                    requirements.add(new RequiredEffect(names, effect.get("minLevel").getAsInt()));
                }
                result.add(new Rule(potion.get("name").getAsString(), requirements));
            }
            return List.copyOf(result);
        } catch (Exception ignored) {
            return List.of();
        }
    }
}
