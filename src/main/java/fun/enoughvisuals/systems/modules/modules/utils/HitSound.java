package fun.enoughvisuals.systems.modules.modules.utils;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

@Register(name="HitSound", id="hit_sound", description="Звук при ударе", category = Category.UTILS)
public class HitSound extends Module {

    public static HitSound INSTANCE;

                                                                               
    public final ModeSetting sound = add(new ModeSetting(
            "Звук", "sound", "Звук", "bell",
            new String[]{"bell", "magic", "pew", "run", "bonk", "hit1", "hit2", "hit3", "moan1", "moan2", "moan3", "moan4", "pop", "uwu"}
    ));

    public final SliderSetting volume = add(new SliderSetting(
            "Громкость", "volume", "Громкость звука", 0.8, 0.0, 1.0, 0.05
    ));

    public final BooleanSetting replaceCrit = add(new BooleanSetting(
            "Заменять крит", "replace_crit", "Отключать ванильный звук критического удара", true
    ));

    public HitSound() {
        INSTANCE = this;
    }

    public static void playHit() {
        if (INSTANCE == null || !INSTANCE.isEnabled() || mc.player == null || mc.world == null) return;

                                                                           
        String soundName = "hit." + INSTANCE.sound.get().toLowerCase();
        SoundEvent event = SoundEvent.of(Identifier.of("enough", soundName));

        mc.world.playSound(
                mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                event,
                SoundCategory.PLAYERS,
                INSTANCE.volume.get().floatValue(),
                1.0f,
                false
        );
    }

    public boolean shouldReplaceCrit() {
        return replaceCrit.get();
    }
}
