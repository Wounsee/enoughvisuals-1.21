package fun.wounsee.cosmetic.utils.fabric;

import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import fun.wounsee.cosmetic.utils.FiguraIdentifier;
import fun.wounsee.cosmetic.utils.FiguraResourceListener;

import java.util.function.Consumer;

public class FiguraResourceListenerImpl extends FiguraResourceListener implements SimpleSynchronousResourceReloadListener {
    public FiguraResourceListenerImpl(String id, Consumer<ResourceManager> reloadConsumer) {
        super(id, reloadConsumer);
    }

    public static FiguraResourceListener createResourceListener(String id, Consumer<ResourceManager> reloadConsumer) {
        return new FiguraResourceListenerImpl(id, reloadConsumer);
    }

    public ResourceLocation getFabricId() {
        return FiguraIdentifier.of(this.id());
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        reloadConsumer().accept(manager);
    }
}
