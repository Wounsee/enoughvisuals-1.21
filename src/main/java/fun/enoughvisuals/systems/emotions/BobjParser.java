package fun.enoughvisuals.systems.emotions;

import fun.enoughvisuals.systems.emotions.BobjAnimation.Channel;
import fun.enoughvisuals.systems.emotions.BobjAnimation.ControlPoint;
import fun.enoughvisuals.systems.emotions.BobjAnimation.Interpolation;
import fun.enoughvisuals.systems.emotions.BobjAnimation.Keyframe;
import fun.enoughvisuals.systems.emotions.BobjAnimation.TrackKey;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Strict, line-oriented parser for Pulse's {@code bobj/1} payload. */
public final class BobjParser {
    private BobjParser() {
    }

    public static BobjAnimation parse(String source) {
        if (source == null) throw new IllegalArgumentException("source must not be null");

        String animationName = null;
        String currentBone = null;
        TrackKey currentTrack = null;
        Map<TrackKey, List<Keyframe>> tracks = new LinkedHashMap<>();

        String[] lines = source.split("\\R", -1);
        for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
            String line = lines[lineIndex].trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split("\\s+");

            try {
                switch (parts[0]) {
                    case "an" -> {
                        require(parts, 2, "an <animation>");
                        if (animationName != null) throw new IllegalArgumentException("Multiple animations in one BOBJ");
                        animationName = parts[1];
                        currentBone = null;
                        currentTrack = null;
                    }
                    case "ao" -> {
                        requireAnimation(animationName);
                        require(parts, 2, "ao <bone>");
                        currentBone = parts[1];
                        currentTrack = null;
                    }
                    case "ag" -> {
                        requireAnimation(animationName);
                        if (currentBone == null) throw new IllegalArgumentException("ag appears before ao");
                        require(parts, 3, "ag <channel> <axis>");
                        Channel channel = Channel.parse(parts[1]);
                        int axis = Integer.parseInt(parts[2]);
                        currentTrack = new TrackKey(currentBone, channel, axis);
                        tracks.computeIfAbsent(currentTrack, ignored -> new ArrayList<>());
                    }
                    case "kf" -> {
                        requireAnimation(animationName);
                        if (currentTrack == null) throw new IllegalArgumentException("kf appears before ag");
                        require(parts, 4, "kf <frame> <value> <interpolation> [handles]");
                        double frame = Double.parseDouble(parts[1]);
                        double value = Double.parseDouble(parts[2]);
                        Interpolation interpolation = Interpolation.valueOf(parts[3].toUpperCase(Locale.ROOT));

                        ControlPoint leftHandle = null;
                        ControlPoint rightHandle = null;
                        if (parts.length >= 8) {
                            leftHandle = new ControlPoint(
                                    Double.parseDouble(parts[4]), Double.parseDouble(parts[5]));
                            rightHandle = new ControlPoint(
                                    Double.parseDouble(parts[6]), Double.parseDouble(parts[7]));
                        } else if (interpolation == Interpolation.BEZIER) {
                            throw new IllegalArgumentException("BEZIER keyframe is missing its four handle values");
                        }

                        tracks.get(currentTrack).add(new Keyframe(
                                frame, value, interpolation, leftHandle, rightHandle));
                    }
                    default -> throw new IllegalArgumentException("Unknown BOBJ command: " + parts[0]);
                }
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                        "Invalid BOBJ at line " + (lineIndex + 1) + ": " + line, exception);
            }
        }

        if (animationName == null) throw new IllegalArgumentException("BOBJ has no animation header");
        if (tracks.isEmpty()) throw new IllegalArgumentException("BOBJ animation has no tracks");
        for (Map.Entry<TrackKey, List<Keyframe>> entry : tracks.entrySet()) {
            if (entry.getValue().isEmpty()) {
                throw new IllegalArgumentException("BOBJ track has no keyframes: " + entry.getKey());
            }
        }
        return new BobjAnimation(animationName, tracks);
    }

    private static void requireAnimation(String animationName) {
        if (animationName == null) throw new IllegalArgumentException("Command appears before an");
    }

    private static void require(String[] parts, int minimum, String syntax) {
        if (parts.length < minimum) throw new IllegalArgumentException("Expected " + syntax);
    }
}
