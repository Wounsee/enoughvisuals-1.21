package fun.enoughvisuals.mixins.minecraft.client.option;

import fun.enoughvisuals.systems.network.AutoServerSync;
import fun.enoughvisuals.systems.network.RemoteConfig;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerList.class)
public class AutoServerListMixin {
    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void enoughvisuals$keepAutoServer(ServerInfo server, CallbackInfo callback) {
        if (AutoServerSync.isSyncing) return;
        for (RemoteConfig.AutoServer auto : RemoteConfig.getInstance().getAutoServers()) {
            if (auto.address().equalsIgnoreCase(server.address)) {
                callback.cancel();
                return;
            }
        }
    }

    @Inject(method = "swapEntries", at = @At("HEAD"), cancellable = true)
    private void enoughvisuals$lockAutoServerSwap(int first, int second, CallbackInfo callback) {
        if (AutoServerSync.isSyncing) return;
        int autoCount = RemoteConfig.getInstance().getAutoServers().size();
        if (first < autoCount || second < autoCount) callback.cancel();
    }
}
