package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

@Register(name="Fullbright", id="fullbright", description="Ночное зрение", category = Category.VISUAL)
public class Fullbright extends Module {

    @Override
    public void onTick() {
        if (!isInGame()) return;

        mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 67676767, 0, false, false, false));
    }

    @Override
    public void onDisable() {
        if (!isInGame()) return;

        mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
    }
}
