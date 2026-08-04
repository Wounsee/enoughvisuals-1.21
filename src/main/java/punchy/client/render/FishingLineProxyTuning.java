package punchy.client.render;

import java.util.EnumMap;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/FishingLineProxyTuning.class */
public final class FishingLineProxyTuning {
    private static final EnumMap<Kind, Profile> RIGHT = new EnumMap<>(Kind.class);
    private static final EnumMap<Kind, Profile> LEFT = new EnumMap<>(Kind.class);

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/FishingLineProxyTuning$Kind.class */
    public enum Kind {
        ROD
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/FishingLineProxyTuning$Profile.class */
    public static final class Profile {
        public float offX;
        public float offY;
        public float offZ;
        public float rotX;
        public float rotY;
        public float rotZ;
        public float scale;

        public Profile(float offX, float offY, float offZ, float rotX, float rotY, float rotZ, float scale) {
            this.offX = offX;
            this.offY = offY;
            this.offZ = offZ;
            this.rotX = rotX;
            this.rotY = rotY;
            this.rotZ = rotZ;
            this.scale = scale;
        }
    }

    static {
        Profile base = new Profile(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f);
        RIGHT.put(Kind.ROD, base);
        LEFT.put(Kind.ROD, mirror(base));
    }

    private FishingLineProxyTuning() {
    }

    public static Profile get(Kind kind, boolean leftHand) {
        return leftHand ? LEFT.get(kind) : RIGHT.get(kind);
    }

    public static void apply(Kind kind, boolean leftHand, Profile profile) {
        if (kind == null || profile == null) {
            return;
        }
        if (leftHand) {
            LEFT.put(kind, profile);
        } else {
            RIGHT.put(kind, profile);
        }
    }

    public static void reset(Kind kind, boolean leftHand) {
        if (kind == null) {
            return;
        }
        Profile defaults = new Profile(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f);
        apply(kind, leftHand, leftHand ? mirror(defaults) : defaults);
    }

    public static Profile mirror(Profile src) {
        return new Profile(-src.offX, src.offY, src.offZ, src.rotX, -src.rotY, -src.rotZ, src.scale);
    }
}
