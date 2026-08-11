package fun.enoughvisuals.systems.emotions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Base64;

/** Immutable decoded metadata for an embedded Pulse emote prop. */
public final class PulseEmotionProp {
    private final String name;
    private final String objSource;
    private final byte[] texturePng;

    private PulseEmotionProp(String name, String objSource, byte[] texturePng) {
        this.name = name;
        this.objSource = objSource;
        this.texturePng = texturePng.clone();
    }

    static PulseEmotionProp parse(JsonObject object) {
        String name = requiredString(object, "name");
        String objSource = requiredString(object, "obj");
        String encodedTexture = requiredString(object, "texture");
        byte[] texturePng;
        try {
            texturePng = Base64.getDecoder().decode(encodedTexture);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid base64 texture for Pulse prop '" + name + "'", exception);
        }
        return new PulseEmotionProp(name, objSource, texturePng);
    }

    public String name() {
        return name;
    }

    public String objSource() {
        return objSource;
    }

    /** Returns a defensive copy of the decoded PNG payload. */
    public byte[] texturePng() {
        return texturePng.clone();
    }

    public int textureByteLength() {
        return texturePng.length;
    }

    private static String requiredString(JsonObject object, String field) {
        JsonElement element = object.get(field);
        if (element == null || element.isJsonNull()) {
            throw new IllegalArgumentException("Pulse prop is missing '" + field + "'");
        }
        String value = element.getAsString();
        if (value.isBlank()) throw new IllegalArgumentException("Pulse prop field '" + field + "' is blank");
        return value;
    }
}
