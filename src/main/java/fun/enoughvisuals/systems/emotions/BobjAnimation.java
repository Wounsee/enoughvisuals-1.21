package fun.enoughvisuals.systems.emotions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** A parsed {@code bobj/1} animation with fractional-frame sampling. */
public final class BobjAnimation {
    private static final double EPSILON = 1.0e-9;

    private final String name;
    private final Map<TrackKey, Track> tracks;
    private final Set<String> bones;

    BobjAnimation(String name, Map<TrackKey, List<Keyframe>> sourceTracks) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name must not be blank");
        this.name = name;

        Map<TrackKey, Track> parsedTracks = new LinkedHashMap<>();
        Set<String> parsedBones = new LinkedHashSet<>();
        for (Map.Entry<TrackKey, List<Keyframe>> entry : sourceTracks.entrySet()) {
            Track track = new Track(entry.getValue());
            parsedTracks.put(entry.getKey(), track);
            parsedBones.add(entry.getKey().bone());
        }
        this.tracks = Collections.unmodifiableMap(parsedTracks);
        this.bones = Collections.unmodifiableSet(parsedBones);
    }

    public String name() {
        return name;
    }

    public Map<TrackKey, Track> tracks() {
        return tracks;
    }

    public Set<String> bones() {
        return bones;
    }

    public Optional<Track> track(String bone, Channel channel, int axis) {
        return Optional.ofNullable(tracks.get(new TrackKey(bone, channel, axis)));
    }

    public Map<String, BoneTransform> sample(double frame) {
        Map<String, BoneTransform> result = new LinkedHashMap<>();
        for (String bone : bones) result.put(bone, sampleBone(bone, frame));
        return Collections.unmodifiableMap(result);
    }

    public BoneTransform sampleBone(String bone, double frame) {
        Vec3 location = sampleVector(bone, Channel.LOCATION, frame, 0.0);
        Vec3 rotation = sampleVector(bone, Channel.ROTATION, frame, 0.0);
        Vec3 scale = sampleVector(bone, Channel.SCALE, frame, 1.0);
        return new BoneTransform(location, rotation, scale);
    }

    private Vec3 sampleVector(String bone, Channel channel, double frame, double defaultValue) {
        return new Vec3(
                sampleAxis(bone, channel, 0, frame, defaultValue),
                sampleAxis(bone, channel, 1, frame, defaultValue),
                sampleAxis(bone, channel, 2, frame, defaultValue));
    }

    private double sampleAxis(String bone, Channel channel, int axis, double frame, double defaultValue) {
        Track track = tracks.get(new TrackKey(bone, channel, axis));
        return track == null ? defaultValue : track.sample(frame);
    }

    public enum Channel {
        LOCATION,
        ROTATION,
        SCALE;

        static Channel parse(String token) {
            return switch (token) {
                case "location" -> LOCATION;
                case "rotation" -> ROTATION;
                case "scale" -> SCALE;
                default -> throw new IllegalArgumentException("Unknown BOBJ channel: " + token);
            };
        }
    }

    public enum Interpolation {
        LINEAR,
        CONSTANT,
        BEZIER
    }

    public record TrackKey(String bone, Channel channel, int axis) {
        public TrackKey {
            if (bone == null || bone.isBlank()) throw new IllegalArgumentException("bone must not be blank");
            if (channel == null) throw new IllegalArgumentException("channel must not be null");
            if (axis < 0 || axis > 2) throw new IllegalArgumentException("axis must be 0, 1 or 2");
        }
    }

    public record ControlPoint(double frame, double value) {
    }

    public record Keyframe(
            double frame,
            double value,
            Interpolation interpolation,
            ControlPoint leftHandle,
            ControlPoint rightHandle
    ) {
        public Keyframe {
            if (!Double.isFinite(frame) || !Double.isFinite(value)) {
                throw new IllegalArgumentException("Keyframe frame and value must be finite");
            }
            if (interpolation == null) throw new IllegalArgumentException("interpolation must not be null");
        }
    }

    public static final class Track {
        private final List<Keyframe> keyframes;

        private Track(List<Keyframe> source) {
            if (source.isEmpty()) throw new IllegalArgumentException("A BOBJ track must contain keyframes");
            List<Keyframe> copy = new ArrayList<>(source);
            double previous = Double.NEGATIVE_INFINITY;
            for (Keyframe keyframe : copy) {
                if (keyframe.frame() < previous) {
                    throw new IllegalArgumentException("BOBJ keyframes must be ordered by frame");
                }
                previous = keyframe.frame();
            }
            keyframes = List.copyOf(copy);
        }

        public List<Keyframe> keyframes() {
            return keyframes;
        }

        /**
         * Samples this track. Duplicate keyframes are intentional BOBJ step
         * discontinuities; at the exact shared frame the last one wins.
         */
        public double sample(double frame) {
            if (Double.isNaN(frame)) throw new IllegalArgumentException("frame must not be NaN");
            if (frame < keyframes.get(0).frame()) return keyframes.get(0).value();

            int low = 0;
            int high = keyframes.size();
            while (low < high) {
                int middle = (low + high) >>> 1;
                if (keyframes.get(middle).frame() <= frame) low = middle + 1;
                else high = middle;
            }

            int leftIndex = low - 1;
            Keyframe left = keyframes.get(leftIndex);
            if (leftIndex == keyframes.size() - 1) return left.value();

            int rightIndex = leftIndex + 1;
            double rightFrame = keyframes.get(rightIndex).frame();
            while (rightIndex + 1 < keyframes.size()
                    && Math.abs(keyframes.get(rightIndex + 1).frame() - rightFrame) <= EPSILON) {
                rightIndex++;
            }
            Keyframe right = keyframes.get(rightIndex);

            /*
             * BOBJ normally stores interpolation on the source keyframe. BBS
             * shifts it to the destination when importing. A duplicate frame
             * is imported by overwriting the first value, while retaining the
             * first duplicate's interpolation; Pulse uses exactly that pattern
             * for a held segment followed by an instantaneous jump.
             */
            Interpolation interpolation = left.interpolation();
            if (rightIndex > leftIndex + 1) {
                interpolation = keyframes.get(rightIndex - 1).interpolation();
            }

            return switch (interpolation) {
                case CONSTANT -> left.value();
                case LINEAR -> linear(left, right, frame);
                case BEZIER -> bezier(left, right, frame);
            };
        }

        private static double linear(Keyframe left, Keyframe right, double frame) {
            double progress = (frame - left.frame()) / (right.frame() - left.frame());
            return left.value() + (right.value() - left.value()) * progress;
        }

        private static double bezier(Keyframe left, Keyframe right, double frame) {
            ControlPoint p1 = left.rightHandle() == null
                    ? new ControlPoint(left.frame(), left.value())
                    : left.rightHandle();
            ControlPoint p2 = right.leftHandle() == null
                    ? new ControlPoint(right.frame(), right.value())
                    : right.leftHandle();

            // BBS clamps time handles to the keyframe interval before solving
            // the curve. Pulse contains a small number of overshooting Blender
            // handles, so omitting this changes the sampled motion.
            double p1Frame = Math.max(left.frame(), Math.min(right.frame(), p1.frame()));
            double p2Frame = Math.max(left.frame(), Math.min(right.frame(), p2.frame()));

            double parameter = solveBezierParameter(
                    frame, left.frame(), p1Frame, p2Frame, right.frame());
            return cubic(
                    left.value(), p1.value(), p2.value(), right.value(), parameter);
        }

        private static double solveBezierParameter(
                double target, double x0, double x1, double x2, double x3) {
            double guess = (target - x0) / (x3 - x0);
            guess = Math.max(0.0, Math.min(1.0, guess));

            // Newton converges quickly for normal Blender handles.
            for (int i = 0; i < 8; i++) {
                double error = cubic(x0, x1, x2, x3, guess) - target;
                if (Math.abs(error) <= EPSILON) return guess;
                double derivative = cubicDerivative(x0, x1, x2, x3, guess);
                if (Math.abs(derivative) <= EPSILON) break;
                double next = guess - error / derivative;
                if (next < 0.0 || next > 1.0) break;
                guess = next;
            }

            // Exported BOBJ time handles are monotonic; bisection is the
            // deterministic fallback for flat tangents.
            double lower = 0.0;
            double upper = 1.0;
            for (int i = 0; i < 48; i++) {
                double middle = (lower + upper) * 0.5;
                if (cubic(x0, x1, x2, x3, middle) < target) lower = middle;
                else upper = middle;
            }
            return (lower + upper) * 0.5;
        }

        private static double cubic(double p0, double p1, double p2, double p3, double t) {
            double inverse = 1.0 - t;
            return inverse * inverse * inverse * p0
                    + 3.0 * inverse * inverse * t * p1
                    + 3.0 * inverse * t * t * p2
                    + t * t * t * p3;
        }

        private static double cubicDerivative(double p0, double p1, double p2, double p3, double t) {
            double inverse = 1.0 - t;
            return 3.0 * inverse * inverse * (p1 - p0)
                    + 6.0 * inverse * t * (p2 - p1)
                    + 3.0 * t * t * (p3 - p2);
        }
    }

    public record Vec3(double x, double y, double z) {
    }

    public record BoneTransform(Vec3 location, Vec3 rotation, Vec3 scale) {
    }
}
