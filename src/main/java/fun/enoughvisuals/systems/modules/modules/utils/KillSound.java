package fun.enoughvisuals.systems.modules.modules.utils;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;

@Register(name="KillSound", id="kill_sound", description="Воспроизводит звук при убийстве", category=Category.UTILS)
public class KillSound extends Module {
    private final SliderSetting volume = add(new SliderSetting(
            "Громкость",
            "volume",
            "Громкость звука убийства",
            0.9,
            0.0,
            1.0,
            0.05
    ));

    public float getVolume() {
        return volume.get().floatValue();
    }
}
