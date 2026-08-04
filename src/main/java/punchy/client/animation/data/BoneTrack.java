package punchy.client.animation.data;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/animation/data/BoneTrack.class */
public class BoneTrack {
    private final String boneName;
    private final List<Keyframe> keyframes = new ArrayList();
    private final TreeMap<Float, Keyframe> timeIndex = new TreeMap<>();

    public BoneTrack(String boneName) {
        this.boneName = boneName;
    }

    public void addKeyframe(Keyframe kf) {
        this.keyframes.add(kf);
        this.timeIndex.put(Float.valueOf(kf.timeStamp()), kf);
    }

    public String getBoneName() {
        return this.boneName;
    }

    public List<Keyframe> getKeyframes() {
        return this.keyframes;
    }

    public TreeMap<Float, Keyframe> getTimeIndex() {
        return this.timeIndex;
    }
}
