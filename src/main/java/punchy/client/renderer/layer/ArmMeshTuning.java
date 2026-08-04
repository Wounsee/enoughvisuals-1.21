package punchy.client.renderer.layer;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/renderer/layer/ArmMeshTuning.class */
public final class ArmMeshTuning {
    private static final Profile DEFAULT = new Profile(1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
    private static final Profile RIGHT_PROFILE = DEFAULT.copy();
    private static final Profile LEFT_PROFILE = RIGHT_PROFILE.mirror();

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/renderer/layer/ArmMeshTuning$Profile.class */
    public static final class Profile {
        public float scale;
        public float offX;
        public float offY;
        public float offZ;
        public float rotX;
        public float rotY;
        public float rotZ;

        public Profile(float scale, float offX, float offY, float offZ, float rotX, float rotY, float rotZ) {
            this.scale = scale;
            this.offX = offX;
            this.offY = offY;
            this.offZ = offZ;
            this.rotX = rotX;
            this.rotY = rotY;
            this.rotZ = rotZ;
        }

        public Profile copy() {
            return new Profile(this.scale, this.offX, this.offY, this.offZ, this.rotX, this.rotY, this.rotZ);
        }

        public Profile mirror() {
            return new Profile(this.scale, -this.offX, this.offY, this.offZ, this.rotX, -this.rotY, -this.rotZ);
        }
    }

    private ArmMeshTuning() {
    }

    public static void resetDefaults() {
        RIGHT_PROFILE.scale = DEFAULT.scale;
        RIGHT_PROFILE.offX = DEFAULT.offX;
        RIGHT_PROFILE.offY = DEFAULT.offY;
        RIGHT_PROFILE.offZ = DEFAULT.offZ;
        RIGHT_PROFILE.rotX = DEFAULT.rotX;
        RIGHT_PROFILE.rotY = DEFAULT.rotY;
        RIGHT_PROFILE.rotZ = DEFAULT.rotZ;
        LEFT_PROFILE.scale = RIGHT_PROFILE.scale;
        LEFT_PROFILE.offX = -RIGHT_PROFILE.offX;
        LEFT_PROFILE.offY = RIGHT_PROFILE.offY;
        LEFT_PROFILE.offZ = RIGHT_PROFILE.offZ;
        LEFT_PROFILE.rotX = RIGHT_PROFILE.rotX;
        LEFT_PROFILE.rotY = -RIGHT_PROFILE.rotY;
        LEFT_PROFILE.rotZ = -RIGHT_PROFILE.rotZ;
    }

    public static Profile getGlobal(boolean left) {
        return left ? LEFT_PROFILE : RIGHT_PROFILE;
    }

    public static void applyGlobal(boolean left, Profile profile) {
        if (profile == null) {
            return;
        }
        Profile target = getGlobal(left);
        target.scale = profile.scale;
        target.offX = profile.offX;
        target.offY = profile.offY;
        target.offZ = profile.offZ;
        target.rotX = profile.rotX;
        target.rotY = profile.rotY;
        target.rotZ = profile.rotZ;
    }

    public static Profile snapshotGlobal(boolean left) {
        Profile src = getGlobal(left);
        return new Profile(src.scale, src.offX, src.offY, src.offZ, src.rotX, src.rotY, src.rotZ);
    }

    public static void resetGlobal(boolean left) {
        applyGlobal(left, DEFAULT);
    }
}
