package punchy.client.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.joml.Vector3f;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/model/BedrockGeoModel.class */
public class BedrockGeoModel {
    private final Map<String, BedrockBone> bones = new LinkedHashMap();

    public void addBone(BedrockBone bone) {
        if (bone == null) {
            return;
        }
        this.bones.put(bone.getName(), bone);
    }

    public Map<String, BedrockBone> getBones() {
        return Collections.unmodifiableMap(this.bones);
    }

    public Vector3f getLocalPivot(String boneName) {
        BedrockBone bone = this.bones.get(boneName);
        if (bone != null) {
            return bone.getLocalPivot();
        }
        return null;
    }
}
