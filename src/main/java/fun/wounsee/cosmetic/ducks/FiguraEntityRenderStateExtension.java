package fun.wounsee.cosmetic.ducks;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.UUID;

public interface FiguraEntityRenderStateExtension {
    Integer figura$getEntityId();
    void figura$setEntityId(int id);
    float figura$getTickDelta();
    void figura$setTickDelta(float tickDelta);
}
