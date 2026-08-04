package punchy.client.modelparts;

import net.minecraft.class_310;
import punchy.client.animation.ItemAnimationManager;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/modelparts/AnimationStateMachine.class */
final class AnimationStateMachine {
    AnimationStateMachine() {
    }

    void tick(class_310 client) {
        ItemAnimationManager.tick(client);
    }
}
