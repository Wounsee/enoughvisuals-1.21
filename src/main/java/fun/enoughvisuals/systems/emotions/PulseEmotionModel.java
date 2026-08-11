package fun.enoughvisuals.systems.emotions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Metadata and sampled animation data from one Pulse {@code model.json}. */
public final class PulseEmotionModel {
    /** Pulse frame numbers advance once per Minecraft client tick. */
    public static final double FRAMES_PER_SECOND = 20.0;

    private final String format;
    private final String name;
    private final String action;
    private final double durationFrames;
    private final boolean looping;
    private final BobjAnimation animation;
    private final JsonElement props;
    private final List<PulseEmotionProp> propModels;

    private PulseEmotionModel(
            String format,
            String name,
            String action,
            double durationFrames,
            boolean looping,
            BobjAnimation animation,
            JsonElement props
    ) {
        this.format = format;
        this.name = name;
        this.action = action;
        this.durationFrames = durationFrames;
        this.looping = looping;
        this.animation = animation;
        this.props = props == null ? null : props.deepCopy();
        this.propModels = parseProps(props);
    }

    public static PulseEmotionModel parse(Reader reader) {
        JsonElement parsed = JsonParser.parseReader(reader);
        if (!parsed.isJsonObject()) throw new IllegalArgumentException("Pulse model root must be an object");
        JsonObject root = parsed.getAsJsonObject();

        String format = requiredString(root, "format");
        if (!"bobj/1".equals(format)) throw new IllegalArgumentException("Unsupported Pulse format: " + format);
        String name = requiredString(root, "name");
        String action = requiredString(root, "action");
        double durationFrames = required(root, "duration").getAsDouble();
        if (!Double.isFinite(durationFrames) || durationFrames < 0.0) {
            throw new IllegalArgumentException("duration must be a finite non-negative frame count");
        }
        boolean looping = required(root, "looping").getAsBoolean();
        BobjAnimation animation = BobjParser.parse(requiredString(root, "bobj"));
        JsonElement props = root.get("props");
        return new PulseEmotionModel(
                format, name, action, durationFrames, looping, animation, props);
    }

    public static PulseEmotionModel load(PulseEmotionCatalog.Entry entry) throws IOException {
        return load(entry, PulseEmotionModel.class.getClassLoader());
    }

    public static PulseEmotionModel load(PulseEmotionCatalog.Entry entry, ClassLoader classLoader)
            throws IOException {
        InputStream stream = classLoader.getResourceAsStream(entry.resourcePath());
        if (stream == null) throw new IOException("Missing Pulse emote resource: " + entry.resourcePath());
        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return parse(reader);
        }
    }

    public String format() {
        return format;
    }

    public String name() {
        return name;
    }

    public String action() {
        return action;
    }

    public double durationFrames() {
        return durationFrames;
    }

    public double durationSeconds() {
        return durationFrames / FRAMES_PER_SECOND;
    }

    public boolean looping() {
        return looping;
    }

    public BobjAnimation animation() {
        return animation;
    }

    public Optional<JsonElement> props() {
        return props == null ? Optional.empty() : Optional.of(props.deepCopy());
    }

    /** Parsed once with the model; texture payloads are already base64-decoded. */
    public List<PulseEmotionProp> propModels() {
        return propModels;
    }

    public Map<String, BobjAnimation.BoneTransform> sampleFrame(double frame) {
        return animation.sample(normalizeFrame(frame));
    }

    public Map<String, BobjAnimation.BoneTransform> sampleSeconds(double seconds) {
        if (!Double.isFinite(seconds)) throw new IllegalArgumentException("seconds must be finite");
        return sampleFrame(seconds * FRAMES_PER_SECOND);
    }

    public double normalizeFrame(double frame) {
        if (!Double.isFinite(frame)) throw new IllegalArgumentException("frame must be finite");
        if (durationFrames <= 0.0) return 0.0;
        if (!looping) return Math.max(0.0, Math.min(durationFrames, frame));
        double normalized = frame % durationFrames;
        return normalized < 0.0 ? normalized + durationFrames : normalized;
    }

    private static JsonElement required(JsonObject object, String field) {
        JsonElement value = object.get(field);
        if (value == null || value.isJsonNull()) {
            throw new IllegalArgumentException("Pulse model is missing '" + field + "'");
        }
        return value;
    }

    private static String requiredString(JsonObject object, String field) {
        String value = required(object, field).getAsString();
        if (value.isBlank()) throw new IllegalArgumentException("Pulse field '" + field + "' must not be blank");
        return value;
    }

    private static List<PulseEmotionProp> parseProps(JsonElement props) {
        if (props == null || props.isJsonNull()) return List.of();
        if (!props.isJsonArray()) throw new IllegalArgumentException("Pulse 'props' must be an array");
        List<PulseEmotionProp> result = new ArrayList<>();
        for (JsonElement element : props.getAsJsonArray()) {
            if (!element.isJsonObject()) throw new IllegalArgumentException("Pulse prop must be an object");
            result.add(PulseEmotionProp.parse(element.getAsJsonObject()));
        }
        return List.copyOf(result);
    }
}
