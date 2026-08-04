package punchy.platform.services;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/platform/services/IPlatformHelper.class */
public interface IPlatformHelper {
    String getPlatformName();

    boolean isModLoaded(String str);

    boolean isDevelopmentEnvironment();

    default String getModDisplayName(String modId) {
        return modId;
    }
}
