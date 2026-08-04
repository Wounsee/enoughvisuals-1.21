package punchy.platform;

import java.util.ServiceLoader;
import punchy.platform.services.IClientPlatform;
import punchy.platform.services.IPlatformHelper;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/platform/Services.class */
public final class Services {
    public static final IPlatformHelper PLATFORM = (IPlatformHelper) load(IPlatformHelper.class);
    public static final IClientPlatform CLIENT = (IClientPlatform) load(IClientPlatform.class);

    private Services() {
    }

    private static <T> T load(Class<T> cls) {
        return (T) ServiceLoader.load(cls).findFirst().orElseThrow(() -> {
            return new NullPointerException("Failed to load service for " + cls.getName());
        });
    }
}
