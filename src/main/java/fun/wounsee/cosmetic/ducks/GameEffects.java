package fun.wounsee.cosmetic.ducks;

import net.minecraft.resources.ResourceLocation;

// Mojang no longer ships the effects field in GameRenderer, instead statically calling the shaders themselves
public interface GameEffects {

    static ResourceLocation[] getEffects() {
        return EFFECTS;
    }

    ResourceLocation[] EFFECTS = new ResourceLocation[]{ResourceLocation.parse("shaders/post/blur.json"), ResourceLocation.parse("shaders/post/entity_outline.json"), ResourceLocation.parse("shaders/post/invert.json"), ResourceLocation.parse("shaders/post/blur.json"), ResourceLocation.parse("shaders/post/creeper.json"), ResourceLocation.parse("shaders/post/spider.json")};
}