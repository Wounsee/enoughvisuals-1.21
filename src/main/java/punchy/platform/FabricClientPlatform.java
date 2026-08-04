package punchy.platform;

import java.util.Objects;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.class_2960;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_3264;
import net.minecraft.class_3300;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import punchy.platform.services.IClientPlatform;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/platform/FabricClientPlatform.class */
public class FabricClientPlatform implements IClientPlatform {
    private static final Logger LOGGER = LoggerFactory.getLogger("punchy/FabricClientPlatform");

    @Override // punchy.platform.services.IClientPlatform
    public void registerKeyMapping(class_304 mapping) {
        try {
            KeyBindingHelper.registerKeyBinding(mapping);
        } catch (AssertionError e) {
            LOGGER.warn("Failed to register keybinding via Fabric API: {}. The keybinding may still work if auto-registered by the constructor.", e.getMessage());
        }
    }

    @Override // punchy.platform.services.IClientPlatform
    public void registerClientTickEnd(Consumer<class_310> handler) {
        Event event = ClientTickEvents.END_CLIENT_TICK;
        Objects.requireNonNull(handler);
        event.register((v1) -> {
            r1.accept(v1);
        });
    }

    @Override // punchy.platform.services.IClientPlatform
    public void registerReloadListener(final class_2960 id, final Consumer<class_3300> handler) {
        ResourceManagerHelper.get(class_3264.field_14188).registerReloadListener(new SimpleSynchronousResourceReloadListener(this) { // from class: punchy.platform.FabricClientPlatform.1
            public class_2960 getFabricId() {
                return id;
            }

            public void method_14491(class_3300 resourceManager) {
                handler.accept(resourceManager);
            }
        });
    }
}
