package punchy.client.modelparts;

import net.minecraft.class_2960;
import net.minecraft.class_3300;
import punchy.Punchy;
import punchy.client.model.ResourcePackGeoRegistry;
import punchy.client.model.ResourcePackItemModelRegistry;
import punchy.client.model.SimpleBedrockGeo;
import punchy.client.model.SimpleBedrockGeoThirdPerson;
import punchy.util.ResourceLocationCompat;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/modelparts/ResourceStateMachine.class */
final class ResourceStateMachine {
    private State state = State.UNLOADED;

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/modelparts/ResourceStateMachine$State.class */
    enum State {
        UNLOADED,
        LOADING,
        READY,
        FAILED
    }

    ResourceStateMachine() {
    }

    void load(class_3300 manager) {
        if (manager == null) {
            this.state = State.FAILED;
            return;
        }
        this.state = State.LOADING;
        try {
            class_2960 playerGeoId = ResourceLocationCompat.fromNamespaceAndPath(Punchy.MOD_ID, "geo/player.geo.json");
            SimpleBedrockGeo.load(playerGeoId);
            class_2960 thirdGeoId = ResourceLocationCompat.fromNamespaceAndPath(Punchy.MOD_ID, "geo/player_3_per.geo.json");
            SimpleBedrockGeoThirdPerson.load(thirdGeoId);
            ResourcePackGeoRegistry.load(manager);
            ResourcePackItemModelRegistry.load(manager);
            this.state = State.READY;
        } catch (Throwable th) {
            this.state = State.FAILED;
        }
    }

    State getState() {
        return this.state;
    }
}
