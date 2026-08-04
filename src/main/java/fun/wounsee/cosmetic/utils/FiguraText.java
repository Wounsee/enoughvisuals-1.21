package fun.wounsee.cosmetic.utils;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import fun.wounsee.cosmetic.CosmeticMod;

public class FiguraText extends TranslatableContents {

    public FiguraText() {
        super(CosmeticMod.MOD_ID, CosmeticMod.MOD_NAME, NO_ARGS);
    }

    public FiguraText(String string) {
        super(CosmeticMod.MOD_ID + "." + string, null, NO_ARGS);
    }

    public FiguraText(String string, Object... args) {
        super(CosmeticMod.MOD_ID + "." + string, null, args);
    }

    public static MutableComponent of() {
        return MutableComponent.create(new FiguraText());
    }

    public static MutableComponent of(String string) {
        return MutableComponent.create(new FiguraText(string));
    }

    public static MutableComponent of(String string, Object... args) {
        return MutableComponent.create(new FiguraText(string, args));
    }
}