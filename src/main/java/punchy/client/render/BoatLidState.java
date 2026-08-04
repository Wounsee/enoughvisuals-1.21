package punchy.client.render;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/BoatLidState.class */
public interface BoatLidState {
    void punchy$setChestBoat(boolean z);

    boolean punchy$isChestBoat();

    void punchy$setInHand(boolean z);

    boolean punchy$isInHand();

    void punchy$setVy(double d);

    double punchy$getVy();

    void punchy$setLidApplied(boolean z);

    boolean punchy$isLidApplied();

    void punchy$setChestPose(float f, float f2, float f3, float f4, float f5, float f6);

    boolean punchy$isChestPoseValid();

    void punchy$setChestPoseValid(boolean z);

    float punchy$getChestX();

    float punchy$getChestY();

    float punchy$getChestZ();

    float punchy$getChestRotX();

    float punchy$getChestRotY();

    float punchy$getChestRotZ();

    void punchy$setLidOpen(float f);

    float punchy$getLidOpen();

    void punchy$setLidVel(float f);

    float punchy$getLidVel();

    void punchy$setLidLastUpdateNanos(long j);

    long punchy$getLidLastUpdateNanos();

    void punchy$setLight(int i);

    int punchy$getLight();
}
