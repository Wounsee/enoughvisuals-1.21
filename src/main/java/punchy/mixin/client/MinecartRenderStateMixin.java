package punchy.mixin.client;

import net.minecraft.class_10045;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import punchy.client.render.MinecartLightState;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/mixin/client/MinecartRenderStateMixin.class */
@Mixin({class_10045.class})
public class MinecartRenderStateMixin implements MinecartLightState {

    @Unique
    private int punchy$light = -1;

    @Override // punchy.client.render.MinecartLightState
    public void punchy$setLight(int light) {
        this.punchy$light = light;
    }

    @Override // punchy.client.render.MinecartLightState
    public int punchy$getLight() {
        return this.punchy$light;
    }
}
