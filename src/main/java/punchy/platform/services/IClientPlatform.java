package punchy.platform.services;

import java.util.function.Consumer;
import net.minecraft.class_2960;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_3300;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/platform/services/IClientPlatform.class */
public interface IClientPlatform {
    void registerKeyMapping(class_304 class_304Var);

    void registerClientTickEnd(Consumer<class_310> consumer);

    void registerReloadListener(class_2960 class_2960Var, Consumer<class_3300> consumer);
}
