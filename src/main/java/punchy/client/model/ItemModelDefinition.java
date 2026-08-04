package punchy.client.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.class_1306;
import net.minecraft.class_2350;
import net.minecraft.class_5603;
import net.minecraft.class_5606;
import net.minecraft.class_5607;
import net.minecraft.class_5609;
import net.minecraft.class_5610;
import net.minecraft.class_630;
import org.joml.Vector3f;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/model/ItemModelDefinition.class */
public final class ItemModelDefinition {
    private final String namespace;
    private final int textureWidth;
    private final int textureHeight;
    private final Map<String, String> textures;
    private final List<Element> elements;
    private final Map<String, BoneDef> bones;
    private final Map<String, String> parentByBone;
    private final Map<String, List<String>> bonePaths;
    private final EnumMap<class_1306, Pose> poses = new EnumMap<>(class_1306.class);
    private final EnumMap<class_1306, DisplayTransform> displayTransforms = new EnumMap<>(class_1306.class);

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/model/ItemModelDefinition$Face.class */
    public static final class Face {
        private final float u0;
        private final float v0;
        private final float u1;
        private final float v1;
        private final int rotation;
        private final String textureRef;

        public Face(float u0, float v0, float u1, float v1, int rotation, String textureRef) {
            this.u0 = u0;
            this.v0 = v0;
            this.u1 = u1;
            this.v1 = v1;
            this.rotation = rotation;
            this.textureRef = textureRef;
        }

        public float getU0() {
            return this.u0;
        }

        public float getV0() {
            return this.v0;
        }

        public float getU1() {
            return this.u1;
        }

        public float getV1() {
            return this.v1;
        }

        public int getRotation() {
            return this.rotation;
        }

        public String getTextureRef() {
            return this.textureRef;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/model/ItemModelDefinition$Element.class */
    public static final class Element {
        private final String boneName;
        private final Vector3f from;
        private final Vector3f to;
        private final EnumMap<class_2350, Face> faces;
        private final int lightEmission;
        private final boolean worldLight;

        public Element(String boneName, Vector3f from, Vector3f to, EnumMap<class_2350, Face> faces, int lightEmission, boolean worldLight) {
            this.boneName = boneName;
            this.from = from;
            this.to = to;
            this.faces = faces;
            this.lightEmission = lightEmission;
            this.worldLight = worldLight;
        }

        public String getBoneName() {
            return this.boneName;
        }

        public Vector3f getFrom() {
            return this.from;
        }

        public Vector3f getTo() {
            return this.to;
        }

        public EnumMap<class_2350, Face> getFaces() {
            return this.faces;
        }

        public int getLightEmission() {
            return this.lightEmission;
        }

        public boolean hasWorldLight() {
            return this.worldLight;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/model/ItemModelDefinition$DisplayTransform.class */
    public static final class DisplayTransform {
        private final Vector3f rotation;
        private final Vector3f translation;
        private final Vector3f scale;

        public DisplayTransform(Vector3f rotation, Vector3f translation, Vector3f scale) {
            this.rotation = rotation;
            this.translation = translation;
            this.scale = scale;
        }

        public Vector3f getRotation() {
            return this.rotation;
        }

        public Vector3f getTranslation() {
            return this.translation;
        }

        public Vector3f getScale() {
            return this.scale;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/model/ItemModelDefinition$BoneDef.class */
    public static final class BoneDef {
        private final String name;
        private final String parent;
        private final Vector3f pivot;
        private final Vector3f rotation;

        public BoneDef(String name, String parent, Vector3f pivot, Vector3f rotation) {
            this.name = name;
            this.parent = parent;
            this.pivot = pivot;
            this.rotation = rotation;
        }

        public String getName() {
            return this.name;
        }

        public String getParent() {
            return this.parent;
        }

        public Vector3f getPivot() {
            return this.pivot;
        }

        public Vector3f getRotation() {
            return this.rotation;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/model/ItemModelDefinition$Pose.class */
    public static final class Pose {
        private final Map<String, class_630> parts;
        private final Map<String, String> parentByBone;

        public Pose(Map<String, class_630> parts, Map<String, String> parentByBone) {
            this.parts = parts;
            this.parentByBone = parentByBone;
        }

        public Map<String, class_630> getParts() {
            return this.parts;
        }

        public Map<String, String> getParentByBone() {
            return this.parentByBone;
        }
    }

    public ItemModelDefinition(String namespace, int textureWidth, int textureHeight, Map<String, String> textures, List<Element> elements, Map<String, BoneDef> bones, Map<String, String> parentByBone, Map<String, List<String>> bonePaths) {
        this.namespace = namespace;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.textures = textures;
        this.elements = elements;
        this.bones = bones;
        this.parentByBone = parentByBone;
        this.bonePaths = bonePaths;
    }

    public String getNamespace() {
        return this.namespace;
    }

    public int getTextureWidth() {
        return this.textureWidth;
    }

    public int getTextureHeight() {
        return this.textureHeight;
    }

    public Map<String, String> getTextures() {
        return this.textures;
    }

    public List<Element> getElements() {
        return this.elements;
    }

    public DisplayTransform getDisplayTransform(class_1306 arm) {
        return this.displayTransforms.get(arm);
    }

    public void setDisplayTransform(class_1306 arm, DisplayTransform transform) {
        if (arm == null || transform == null) {
            return;
        }
        this.displayTransforms.put(arm, transform);
    }

    public Pose getPose(class_1306 arm) {
        Pose pose = this.poses.get(arm);
        if (pose != null) {
            return pose;
        }
        Pose created = buildPose();
        this.poses.put(arm, created);
        return created;
    }

    private Pose buildPose() {
        BoneDef parentBone;
        class_5609 mesh = new class_5609();
        class_5610 root = mesh.method_32111();
        Map<String, class_5610> defs = new HashMap<>();
        Map<String, BoneDef> pending = new LinkedHashMap<>(this.bones);
        do {
            boolean progressed = false;
            Iterator<Map.Entry<String, BoneDef>> iterator = pending.entrySet().iterator();
            while (iterator.hasNext()) {
                BoneDef bone = iterator.next().getValue();
                String parentName = bone.getParent();
                if (parentName == null || defs.containsKey(parentName)) {
                    class_5610 parent = parentName == null ? root : defs.get(parentName);
                    Vector3f pivot = bone.getPivot();
                    Vector3f local = pivot;
                    if (parentName != null && (parentBone = this.bones.get(parentName)) != null) {
                        Vector3f parentPivot = parentBone.getPivot();
                        local = new Vector3f(pivot.x - parentPivot.x, pivot.y - parentPivot.y, pivot.z - parentPivot.z);
                    }
                    Vector3f rot = bone.getRotation();
                    class_5603 pose = class_5603.method_32091(local.x, local.y, local.z, (float) Math.toRadians(rot.x), (float) Math.toRadians(rot.y), (float) Math.toRadians(rot.z));
                    defs.put(bone.getName(), parent.method_32117(bone.getName(), class_5606.method_32108(), pose));
                    iterator.remove();
                    progressed = true;
                }
            }
            if (!progressed) {
                break;
            }
        } while (!pending.isEmpty());
        class_630 rootPart = class_5607.method_32110(mesh, this.textureWidth, this.textureHeight).method_32109();
        Map<String, class_630> parts = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : this.bonePaths.entrySet()) {
            class_630 part = rootPart;
            for (String step : entry.getValue()) {
                if (part == null) {
                    break;
                }
                part = part.method_32086(step);
            }
            if (part != null) {
                parts.put(entry.getKey(), part);
            }
        }
        return new Pose(parts, this.parentByBone);
    }

    public static Map<String, List<String>> buildPaths(Map<String, String> parentByBone) {
        Map<String, List<String>> paths = new HashMap<>();
        for (String bone : parentByBone.keySet()) {
            List<String> chain = new ArrayList<>();
            String str = bone;
            while (true) {
                String current = str;
                if (current != null) {
                    chain.add(0, current);
                    str = parentByBone.get(current);
                }
            }
            paths.put(bone, chain);
        }
        return paths;
    }

    public static Map<String, String> copyParentMap(Map<String, String> parentByBone) {
        return Collections.unmodifiableMap(new HashMap(parentByBone));
    }
}
