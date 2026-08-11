package fun.enoughvisuals.systems.cosmetic.pulse;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;

import java.lang.reflect.Constructor;

/** Starts only the local Pulse renderers. The recovered Pulse network and its extra menu stay disabled. */
public final class PulseLocalClient implements ClientModInitializer {
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onInitializeClient() {
        PulseCosmeticBridge.clear("CAPE");
        PulseCosmeticBridge.clear("GRAFFITI");
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, renderer, helper, context) -> {
            if (!(renderer instanceof PlayerEntityRenderer)) return;
            try {
                Class<?> type = Class.forName("dev.pulse.cosmetics.PulseCosmeticsFeatureRenderer");
                Constructor<?> constructor = type.getConstructor(FeatureRendererContext.class);
                helper.register((FeatureRenderer) constructor.newInstance(renderer));
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Unable to start the embedded Pulse cosmetic renderer", exception);
            }
        });

        // The bundled Pulse pet renderer is skipped on purpose: it anchors pets to the
        // owner's eye height, so they end up inside or far above the player.
        PulsePetWorldRenderer.register();
    }
}
