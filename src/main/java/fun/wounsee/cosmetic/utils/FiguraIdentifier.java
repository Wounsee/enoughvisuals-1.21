package fun.wounsee.cosmetic.utils;

import fun.wounsee.cosmetic.CosmeticMod;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;

public final class FiguraIdentifier {
    private FiguraIdentifier() {}

    public static ResourceLocation of(String path) {
        return ResourceLocation.fromNamespaceAndPath(CosmeticMod.MOD_ID, path);
    }

    public static String formatPath(String path) {
        return Util.sanitizeName(path, ResourceLocation::validPathChar);
    }
}
