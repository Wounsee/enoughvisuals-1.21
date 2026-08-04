package punchy.client.render;

import java.util.EnumMap;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/EnchantParticleProxyTuning.class */
public final class EnchantParticleProxyTuning {
    private static final EnumMap<Kind, Profile> PROFILES_RIGHT = new EnumMap<>(Kind.class);
    private static final EnumMap<Kind, Profile> PROFILES_LEFT = new EnumMap<>(Kind.class);

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/EnchantParticleProxyTuning$Kind.class */
    public enum Kind {
        BOOK
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/EnchantParticleProxyTuning$Profile.class */
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
        PROFILES_RIGHT.put(Kind.BOOK, new Profile(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f));
        PROFILES_LEFT.put(Kind.BOOK, new Profile(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f));
    }

    private EnchantParticleProxyTuning() {
    }

    public static Profile get(Kind kind, boolean left) {
        return (left ? PROFILES_LEFT : PROFILES_RIGHT).get(kind);
    }

    public static void apply(Kind kind, boolean left, Profile profile) {
        if (kind == null || profile == null) {
            return;
        }
        (left ? PROFILES_LEFT : PROFILES_RIGHT).put(kind, profile);
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    public static void reset(Kind kind, boolean left) throws MatchException {
        if (kind == null) {
            return;
        }
        switch (kind) {
            case BOOK:
                Profile defaults = new Profile(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f);
                (left ? PROFILES_LEFT : PROFILES_RIGHT).put(kind, defaults);
                return;
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
    }
}
