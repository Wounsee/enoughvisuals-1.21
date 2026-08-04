package punchy.mixin.client;

import net.minecraft.class_10004;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import punchy.client.render.BoatLidState;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/mixin/client/BoatRenderStateMixin.class */
@Mixin({class_10004.class})
public class BoatRenderStateMixin implements BoatLidState {

    @Unique
    private double punchy$vy;

    @Unique
    private boolean punchy$chestBoat;

    @Unique
    private boolean punchy$inHand;

    @Unique
    private boolean punchy$lidApplied;

    @Unique
    private boolean punchy$chestPoseValid;

    @Unique
    private float punchy$chestX;

    @Unique
    private float punchy$chestY;

    @Unique
    private float punchy$chestZ;

    @Unique
    private float punchy$chestRotX;

    @Unique
    private float punchy$chestRotY;

    @Unique
    private float punchy$chestRotZ;

    @Unique
    private float punchy$lidOpen;

    @Unique
    private float punchy$lidVel;

    @Unique
    private long punchy$lidLastUpdateNanos;

    @Unique
    private int punchy$light = -1;

    @Override // punchy.client.render.BoatLidState
    public void punchy$setVy(double vy) {
        this.punchy$vy = vy;
    }

    @Override // punchy.client.render.BoatLidState
    public double punchy$getVy() {
        return this.punchy$vy;
    }

    @Override // punchy.client.render.BoatLidState
    public void punchy$setChestBoat(boolean chestBoat) {
        this.punchy$chestBoat = chestBoat;
    }

    @Override // punchy.client.render.BoatLidState
    public boolean punchy$isChestBoat() {
        return this.punchy$chestBoat;
    }

    @Override // punchy.client.render.BoatLidState
    public void punchy$setInHand(boolean inHand) {
        this.punchy$inHand = inHand;
    }

    @Override // punchy.client.render.BoatLidState
    public boolean punchy$isInHand() {
        return this.punchy$inHand;
    }

    @Override // punchy.client.render.BoatLidState
    public void punchy$setLidApplied(boolean applied) {
        this.punchy$lidApplied = applied;
    }

    @Override // punchy.client.render.BoatLidState
    public boolean punchy$isLidApplied() {
        return this.punchy$lidApplied;
    }

    @Override // punchy.client.render.BoatLidState
    public void punchy$setChestPose(float x, float y, float z, float rotX, float rotY, float rotZ) {
        this.punchy$chestPoseValid = true;
        this.punchy$chestX = x;
        this.punchy$chestY = y;
        this.punchy$chestZ = z;
        this.punchy$chestRotX = rotX;
        this.punchy$chestRotY = rotY;
        this.punchy$chestRotZ = rotZ;
    }

    @Override // punchy.client.render.BoatLidState
    public float punchy$getChestX() {
        return this.punchy$chestX;
    }

    @Override // punchy.client.render.BoatLidState
    public float punchy$getChestY() {
        return this.punchy$chestY;
    }

    @Override // punchy.client.render.BoatLidState
    public float punchy$getChestZ() {
        return this.punchy$chestZ;
    }

    @Override // punchy.client.render.BoatLidState
    public float punchy$getChestRotX() {
        return this.punchy$chestRotX;
    }

    @Override // punchy.client.render.BoatLidState
    public float punchy$getChestRotY() {
        return this.punchy$chestRotY;
    }

    @Override // punchy.client.render.BoatLidState
    public float punchy$getChestRotZ() {
        return this.punchy$chestRotZ;
    }

    @Override // punchy.client.render.BoatLidState
    public boolean punchy$isChestPoseValid() {
        return this.punchy$chestPoseValid;
    }

    @Override // punchy.client.render.BoatLidState
    public void punchy$setChestPoseValid(boolean valid) {
        this.punchy$chestPoseValid = valid;
    }

    @Override // punchy.client.render.BoatLidState
    public void punchy$setLidOpen(float open) {
        this.punchy$lidOpen = open;
    }

    @Override // punchy.client.render.BoatLidState
    public float punchy$getLidOpen() {
        return this.punchy$lidOpen;
    }

    @Override // punchy.client.render.BoatLidState
    public void punchy$setLidVel(float vel) {
        this.punchy$lidVel = vel;
    }

    @Override // punchy.client.render.BoatLidState
    public float punchy$getLidVel() {
        return this.punchy$lidVel;
    }

    @Override // punchy.client.render.BoatLidState
    public void punchy$setLidLastUpdateNanos(long nanos) {
        this.punchy$lidLastUpdateNanos = nanos;
    }

    @Override // punchy.client.render.BoatLidState
    public long punchy$getLidLastUpdateNanos() {
        return this.punchy$lidLastUpdateNanos;
    }

    @Override // punchy.client.render.BoatLidState
    public void punchy$setLight(int light) {
        this.punchy$light = light;
    }

    @Override // punchy.client.render.BoatLidState
    public int punchy$getLight() {
        return this.punchy$light;
    }
}
