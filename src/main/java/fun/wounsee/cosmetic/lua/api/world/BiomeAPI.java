package fun.wounsee.cosmetic.lua.api.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import fun.wounsee.cosmetic.lua.LuaWhitelist;
import fun.wounsee.cosmetic.lua.docs.LuaFieldDoc;
import fun.wounsee.cosmetic.lua.docs.LuaMethodDoc;
import fun.wounsee.cosmetic.lua.docs.LuaMethodOverload;
import fun.wounsee.cosmetic.lua.docs.LuaTypeDoc;
import fun.wounsee.cosmetic.math.vector.FiguraVec3;
import fun.wounsee.cosmetic.mixin.BiomeAccessor;
import fun.wounsee.cosmetic.utils.ColorUtils;
import fun.wounsee.cosmetic.utils.LuaUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@LuaWhitelist
@LuaTypeDoc(
        name = "Biome",
        value = "biome"
)
public class BiomeAPI {

    private final Biome biome;
    private BlockPos pos;

    @LuaWhitelist
    @LuaFieldDoc("biome.id")
    public final String id;

    public BiomeAPI(Biome biome, BlockPos pos) {
        this.biome = biome;
        this.pos = pos;
        this.id = WorldAPI.getCurrentWorld().registryAccess().lookupOrThrow(Registries.BIOME).getKey(biome).toString();
    }

    protected BlockPos getBlockPos() {
        return pos == null ? BlockPos.ZERO : pos;
    }

    @LuaWhitelist
    @LuaMethodDoc("biome.get_pos")
    public FiguraVec3 getPos() {
        return FiguraVec3.fromBlockPos(getBlockPos());
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "pos"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class},
                            argumentNames = {"x", "y", "z"}
                    )
            },
            aliases = "pos",
            value = "biome.set_pos"
    )
    public BiomeAPI setPos(Object x, Double y, Double z) {
        FiguraVec3 newPos = LuaUtils.parseVec3("setPos", x, y, z);
        pos = newPos.asBlockPos();
        return this;
    }

    @LuaWhitelist
    public BiomeAPI pos(Object x, Double y, Double z) {
        return setPos(x, y, z);
    }

    @LuaWhitelist
    @LuaMethodDoc("biome.get_tags")
    public List<String> getTags() {
        List<String> list = new ArrayList<>();

        Registry<Biome> registry = WorldAPI.getCurrentWorld().registryAccess().lookupOrThrow(Registries.BIOME);
        Optional<ResourceKey<Biome>> key = registry.getResourceKey(biome);

        if (key.isEmpty())
            return list;

        for (TagKey<Biome> biomeTagKey : registry.getOrThrow(key.get()).tags().toList())
            list.add(biomeTagKey.location().toString());

        return list;
    }

    @LuaWhitelist
    @LuaMethodDoc("biome.get_temperature")
    public float getTemperature() {
        return biome.getBaseTemperature();
    }

    @LuaWhitelist
    @LuaMethodDoc("biome.get_precipitation")
    public String getPrecipitation() {
        return biome.getPrecipitationAt(getBlockPos(), WorldAPI.getCurrentWorld().getSeaLevel()).name();
    }

    @LuaWhitelist
    @LuaMethodDoc("biome.get_sky_color")
    public FiguraVec3 getSkyColor() {
        return ColorUtils.intToRGB(biome.getSkyColor());
    }

    @LuaWhitelist
    @LuaMethodDoc("biome.get_foliage_color")
    public FiguraVec3 getFoliageColor() {
        return ColorUtils.intToRGB(biome.getFoliageColor());
    }

    @LuaWhitelist
    @LuaMethodDoc("biome.get_grass_color")
    public FiguraVec3 getGrassColor() {
        BlockPos pos = getBlockPos();
        return ColorUtils.intToRGB(biome.getGrassColor(pos.getX(), pos.getY()));
    }

    @LuaWhitelist
    @LuaMethodDoc("biome.get_fog_color")
    public FiguraVec3 getFogColor() {
        return ColorUtils.intToRGB(biome.getFogColor());
    }

    @LuaWhitelist
    @LuaMethodDoc("biome.get_water_color")
    public FiguraVec3 getWaterColor() {
        return ColorUtils.intToRGB(biome.getWaterColor());
    }

    @LuaWhitelist
    @LuaMethodDoc("biome.get_water_fog_color")
    public FiguraVec3 getWaterFogColor() {
        return ColorUtils.intToRGB(biome.getWaterFogColor());
    }

    @LuaWhitelist
    @LuaMethodDoc("biome.get_downfall")
    public float getDownfall() {
        return ((BiomeAccessor) (Object) biome).getClimateSettings().downfall();
    }

    @LuaWhitelist
    @LuaMethodDoc("biome.is_hot")
    public boolean isHot() {
        return ((BiomeAccessor) (Object) biome).getTheTemperature(getBlockPos(), WorldAPI.getCurrentWorld().getSeaLevel()) > 1f;
    }

    @LuaWhitelist
    @LuaMethodDoc("biome.is_cold")
    public boolean isCold() {
        return biome.coldEnoughToSnow(getBlockPos(), WorldAPI.getCurrentWorld().getSeaLevel());
    }

    @LuaWhitelist
    public boolean __eq(BiomeAPI other) {
        return this.biome.equals(other.biome);
    }

    @LuaWhitelist
    public Object __index(String arg) {
        return "id".equals(arg) ? id : null;
    }

    @Override
    public String toString() {
        return id + " (Biome)";
    }
}
