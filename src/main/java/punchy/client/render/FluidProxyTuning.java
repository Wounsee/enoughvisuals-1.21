package punchy.client.render;

import java.util.EnumMap;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/FluidProxyTuning.class */
public final class FluidProxyTuning {
    private static final EnumMap<Kind, Profile> PROFILES = new EnumMap<>(Kind.class);

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/FluidProxyTuning$Kind.class */
    public enum Kind {
        WATER,
        LAVA,
        MILK
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/FluidProxyTuning$Profile.class */
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
        PROFILES.put(Kind.WATER, new Profile(0.0f, 0.06f, 0.0f, 0.0f, 0.0f, 0.0f, 0.58f));
        PROFILES.put(Kind.LAVA, new Profile(0.0f, 0.06f, 0.0f, 0.0f, 0.0f, 0.0f, 0.58f));
        PROFILES.put(Kind.MILK, new Profile(0.0f, 0.06f, 0.0f, 0.0f, 0.0f, 0.0f, 0.58f));
    }

    private FluidProxyTuning() {
    }

    public static Profile get(Kind kind) {
        return PROFILES.get(kind);
    }

    public static void apply(Kind kind, Profile profile) {
        if (kind == null || profile == null) {
            return;
        }
        PROFILES.put(kind, profile);
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    public static void reset(Kind kind) throws MatchException {
        if (kind == null) {
            return;
        }
        switch (kind) {
            case WATER:
            case LAVA:
            case MILK:
                Profile defaults = new Profile(0.0f, 0.06f, 0.0f, 0.0f, 0.0f, 0.0f, 0.58f);
                PROFILES.put(kind, defaults);
                return;
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
    }
}
