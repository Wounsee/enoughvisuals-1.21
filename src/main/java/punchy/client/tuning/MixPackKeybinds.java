package punchy.client.tuning;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/tuning/MixPackKeybinds.class */
public final class MixPackKeybinds {
    private static boolean registered = false;

    private MixPackKeybinds() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
    }
}
