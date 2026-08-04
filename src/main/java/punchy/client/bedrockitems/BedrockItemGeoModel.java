package punchy.client.bedrockitems;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.class_2350;
import net.minecraft.class_630;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/bedrockitems/BedrockItemGeoModel.class */
final class BedrockItemGeoModel {
    private final class_630 root;
    private final Map<String, class_630> parts;
    private int textureWidth;
    private int textureHeight;
    private final Map<String, String> parentByBone;
    private final Map<String, List<BedrockCube>> cubesByBone;

    BedrockItemGeoModel(class_630 root, Map<String, class_630> parts, int textureWidth, int textureHeight, Map<String, String> parentByBone, Map<String, List<BedrockCube>> cubesByBone) {
        this.root = root;
        this.parts = parts == null ? Map.of() : new HashMap<>(parts);
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.parentByBone = parentByBone == null ? Map.of() : new HashMap<>(parentByBone);
        this.cubesByBone = cubesByBone == null ? Map.of() : new HashMap<>(cubesByBone);
    }

    class_630 getRoot() {
        return this.root;
    }

    Map<String, class_630> getParts() {
        return Collections.unmodifiableMap(this.parts);
    }

    int getTextureWidth() {
        return this.textureWidth;
    }

    int getTextureHeight() {
        return this.textureHeight;
    }

    void setTextureSize(int width, int height) {
        if (width > 0) {
            this.textureWidth = width;
        }
        if (height > 0) {
            this.textureHeight = height;
        }
    }

    Map<String, String> getParentByBone() {
        return Collections.unmodifiableMap(this.parentByBone);
    }

    Map<String, List<BedrockCube>> getCubesByBone() {
        return Collections.unmodifiableMap(this.cubesByBone);
    }

    void resetPose() {
        if (this.root != null) {
            this.root.method_41923();
        }
        for (class_630 part : this.parts.values()) {
            if (part != null) {
                part.method_41923();
            }
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/bedrockitems/BedrockItemGeoModel$BedrockFace.class */
    static final class BedrockFace {
        final float u;
        final float v;
        final float uSize;
        final float vSize;
        final int rotation;

        BedrockFace(float u, float v, float uSize, float vSize, int rotation) {
            this.u = u;
            this.v = v;
            this.uSize = uSize;
            this.vSize = vSize;
            this.rotation = rotation;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/bedrockitems/BedrockItemGeoModel$BedrockCube.class */
    static final class BedrockCube {
        final float x1;
        final float y1;
        final float z1;
        final float x2;
        final float y2;
        final float z2;
        final Map<class_2350, BedrockFace> faces;
        final boolean mirror;
        final float pivotX;
        final float pivotY;
        final float pivotZ;
        final float rotX;
        final float rotY;
        final float rotZ;
        final boolean hasRotation;

        BedrockCube(float x1, float y1, float z1, float x2, float y2, float z2, Map<class_2350, BedrockFace> faces, boolean mirror, float pivotX, float pivotY, float pivotZ, float rotX, float rotY, float rotZ) {
            this.x1 = x1;
            this.y1 = y1;
            this.z1 = z1;
            this.x2 = x2;
            this.y2 = y2;
            this.z2 = z2;
            this.faces = faces == null ? Map.of() : faces;
            this.mirror = mirror;
            this.pivotX = pivotX;
            this.pivotY = pivotY;
            this.pivotZ = pivotZ;
            this.rotX = rotX;
            this.rotY = rotY;
            this.rotZ = rotZ;
            this.hasRotation = (rotX == 0.0f && rotY == 0.0f && rotZ == 0.0f) ? false : true;
        }
    }
}
