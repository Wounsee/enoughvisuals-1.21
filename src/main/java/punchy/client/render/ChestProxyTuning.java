package punchy.client.render;

import java.util.EnumMap;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/ChestProxyTuning.class */
public final class ChestProxyTuning {
    private static final EnumMap<Kind, Profile> PROFILES = new EnumMap<>(Kind.class);

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/ChestProxyTuning$Kind.class */
    public enum Kind {
        BOAT,
        RAFT,
        MINECART
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/ChestProxyTuning$Profile.class */
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
        PROFILES.put(Kind.BOAT, new Profile(0.0f, 0.12f, -0.08f, 0.0f, 0.0f, 0.0f, 0.75f));
        PROFILES.put(Kind.RAFT, new Profile(0.0f, 0.12f, -0.08f, 0.0f, 0.0f, 0.0f, 0.75f));
        PROFILES.put(Kind.MINECART, new Profile(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.75f));
    }

    private ChestProxyTuning() {
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
        Profile profile;
        if (kind == null) {
            return;
        }
        switch (kind) {
            case BOAT:
                profile = new Profile(0.0f, 0.12f, -0.08f, 0.0f, 0.0f, 0.0f, 0.75f);
                break;
            case RAFT:
                profile = new Profile(0.0f, 0.12f, -0.08f, 0.0f, 0.0f, 0.0f, 0.75f);
                break;
            case MINECART:
                profile = new Profile(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.75f);
                break;
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
        Profile defaults = profile;
        PROFILES.put(kind, defaults);
    }
}
