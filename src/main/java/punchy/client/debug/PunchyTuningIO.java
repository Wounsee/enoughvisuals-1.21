package punchy.client.debug;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3300;
import punchy.Punchy;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/debug/PunchyTuningIO.class */
public class PunchyTuningIO {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/debug/PunchyTuningIO$Section.class */
    private enum Section {
        GLOBAL,
        ARM_RIGHT,
        ARM_LEFT,
        ITEM_RIGHT,
        ITEM_LEFT
    }

    public static void writeDefaultFromResources(class_3300 resourceManager) {
        if (resourceManager == null) {
            return;
        }
        class_2960 id = class_2960.method_60655(Punchy.MOD_ID, "tuning/default.json");
        try {
            byte[] data = resourceManager.getResourceOrThrow(id).method_14482().readAllBytes();
            Path path = getDefaultPath();
            Files.createDirectories(path.getParent(), new FileAttribute[0]);
            Files.write(path, data, new OpenOption[0]);
        } catch (IOException e) {
        }
    }

    public static void saveSession() {
        try {
            writeJson(getSessionPath(), buildJson());
        } catch (IOException e) {
        }
    }

    public static void loadDefaultToDebug() {
        Path path = getDefaultPath();
        if (!Files.exists(path, new LinkOption[0])) {
            return;
        }
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            applyJson(root);
        } catch (Exception e) {
        }
    }

    public static Path getSessionPath() {
        return getGameDir().resolve("config").resolve(Punchy.MOD_ID).resolve("punchy_tuning_session.json");
    }

    public static Path getDefaultPath() {
        return getGameDir().resolve("config").resolve(Punchy.MOD_ID).resolve("punchy_tuning_default.json");
    }

    private static Path getGameDir() {
        class_310 mc = class_310.method_1551();
        return mc.field_1697.toPath();
    }

    private static void writeJson(Path path, JsonObject obj) throws IOException {
        Files.createDirectories(path.getParent(), new FileAttribute[0]);
        byte[] data = GSON.toJson(obj).getBytes(StandardCharsets.UTF_8);
        Files.write(path, data, new OpenOption[0]);
    }

    private static void applyJson(JsonObject root) {
        if (root == null) {
            return;
        }
        applySection(root.getAsJsonObject("global"), Section.GLOBAL);
        applySection(root.getAsJsonObject("arm_right"), Section.ARM_RIGHT);
        applySection(root.getAsJsonObject("arm_left"), Section.ARM_LEFT);
        applySection(root.getAsJsonObject("item_right"), Section.ITEM_RIGHT);
        applySection(root.getAsJsonObject("item_left"), Section.ITEM_LEFT);
    }

    private static void applySection(JsonObject obj, Section section) {
        if (obj == null) {
        }
        float posX = getFloat(obj, "pos_x", 0.0f);
        float posY = getFloat(obj, "pos_y", 0.0f);
        float posZ = getFloat(obj, "pos_z", 0.0f);
        float rotX = getFloat(obj, "rot_x", 0.0f);
        float rotY = getFloat(obj, "rot_y", 0.0f);
        float rotZ = getFloat(obj, "rot_z", 0.0f);
        float scale = getFloat(obj, "scale", 1.0f);
        float pivotX = getFloat(obj, "pivot_x", 0.0f);
        float pivotY = getFloat(obj, "pivot_y", 0.0f);
        float pivotZ = getFloat(obj, "pivot_z", 0.0f);
        switch (section) {
            case GLOBAL:
                PunchyDebug.POS_X = posX;
                PunchyDebug.POS_Y = posY;
                PunchyDebug.POS_Z = posZ;
                PunchyDebug.ROT_X = rotX;
                PunchyDebug.ROT_Y = rotY;
                PunchyDebug.ROT_Z = rotZ;
                PunchyDebug.SCALE = scale;
                break;
            case ARM_RIGHT:
                PunchyDebug.ARM_R_POS_X = posX;
                PunchyDebug.ARM_R_POS_Y = posY;
                PunchyDebug.ARM_R_POS_Z = posZ;
                PunchyDebug.ARM_R_ROT_X = rotX;
                PunchyDebug.ARM_R_ROT_Y = rotY;
                PunchyDebug.ARM_R_ROT_Z = rotZ;
                PunchyDebug.ARM_R_SCALE_X = scale;
                PunchyDebug.ARM_R_SCALE_Y = scale;
                PunchyDebug.ARM_R_SCALE_Z = scale;
                break;
            case ARM_LEFT:
                PunchyDebug.ARM_L_POS_X = posX;
                PunchyDebug.ARM_L_POS_Y = posY;
                PunchyDebug.ARM_L_POS_Z = posZ;
                PunchyDebug.ARM_L_ROT_X = rotX;
                PunchyDebug.ARM_L_ROT_Y = rotY;
                PunchyDebug.ARM_L_ROT_Z = rotZ;
                PunchyDebug.ARM_L_SCALE_X = scale;
                PunchyDebug.ARM_L_SCALE_Y = scale;
                PunchyDebug.ARM_L_SCALE_Z = scale;
                break;
            case ITEM_RIGHT:
                PunchyDebug.ITEM_R_POS_X = posX;
                PunchyDebug.ITEM_R_POS_Y = posY;
                PunchyDebug.ITEM_R_POS_Z = posZ;
                PunchyDebug.ITEM_R_ROT_X = rotX;
                PunchyDebug.ITEM_R_ROT_Y = rotY;
                PunchyDebug.ITEM_R_ROT_Z = rotZ;
                PunchyDebug.ITEM_R_SCALE_X = scale;
                PunchyDebug.ITEM_R_SCALE_Y = scale;
                PunchyDebug.ITEM_R_SCALE_Z = scale;
                PunchyDebug.ITEM_R_PIVOT_X = pivotX;
                PunchyDebug.ITEM_R_PIVOT_Y = pivotY;
                PunchyDebug.ITEM_R_PIVOT_Z = pivotZ;
                break;
            case ITEM_LEFT:
                PunchyDebug.ITEM_L_POS_X = posX;
                PunchyDebug.ITEM_L_POS_Y = posY;
                PunchyDebug.ITEM_L_POS_Z = posZ;
                PunchyDebug.ITEM_L_ROT_X = rotX;
                PunchyDebug.ITEM_L_ROT_Y = rotY;
                PunchyDebug.ITEM_L_ROT_Z = rotZ;
                PunchyDebug.ITEM_L_SCALE_X = scale;
                PunchyDebug.ITEM_L_SCALE_Y = scale;
                PunchyDebug.ITEM_L_SCALE_Z = scale;
                PunchyDebug.ITEM_L_PIVOT_X = pivotX;
                PunchyDebug.ITEM_L_PIVOT_Y = pivotY;
                PunchyDebug.ITEM_L_PIVOT_Z = pivotZ;
                break;
        }
    }

    private static float getFloat(JsonObject obj, String key, float fallback) {
        try {
            if (obj.has(key)) {
                return obj.get(key).getAsFloat();
            }
        } catch (Exception e) {
        }
        return fallback;
    }

    private static JsonObject buildJson() {
        JsonObject root = new JsonObject();
        root.add("global", globalSection());
        root.add("arm_right", armSection(true));
        root.add("arm_left", armSection(false));
        root.add("item_right", itemSection(true));
        root.add("item_left", itemSection(false));
        return root;
    }

    private static JsonObject globalSection() {
        JsonObject obj = new JsonObject();
        obj.addProperty("pos_x", Float.valueOf(PunchyDebug.POS_X));
        obj.addProperty("pos_y", Float.valueOf(PunchyDebug.POS_Y));
        obj.addProperty("pos_z", Float.valueOf(PunchyDebug.POS_Z));
        obj.addProperty("rot_x", Float.valueOf(PunchyDebug.ROT_X));
        obj.addProperty("rot_y", Float.valueOf(PunchyDebug.ROT_Y));
        obj.addProperty("rot_z", Float.valueOf(PunchyDebug.ROT_Z));
        obj.addProperty("scale", Float.valueOf(PunchyDebug.SCALE));
        return obj;
    }

    private static JsonObject armSection(boolean right) {
        JsonObject obj = new JsonObject();
        if (right) {
            obj.addProperty("pos_x", Float.valueOf(PunchyDebug.ARM_R_POS_X));
            obj.addProperty("pos_y", Float.valueOf(PunchyDebug.ARM_R_POS_Y));
            obj.addProperty("pos_z", Float.valueOf(PunchyDebug.ARM_R_POS_Z));
            obj.addProperty("rot_x", Float.valueOf(PunchyDebug.ARM_R_ROT_X));
            obj.addProperty("rot_y", Float.valueOf(PunchyDebug.ARM_R_ROT_Y));
            obj.addProperty("rot_z", Float.valueOf(PunchyDebug.ARM_R_ROT_Z));
            obj.addProperty("scale", Float.valueOf(PunchyDebug.ARM_R_SCALE_X));
        } else {
            obj.addProperty("pos_x", Float.valueOf(PunchyDebug.ARM_L_POS_X));
            obj.addProperty("pos_y", Float.valueOf(PunchyDebug.ARM_L_POS_Y));
            obj.addProperty("pos_z", Float.valueOf(PunchyDebug.ARM_L_POS_Z));
            obj.addProperty("rot_x", Float.valueOf(PunchyDebug.ARM_L_ROT_X));
            obj.addProperty("rot_y", Float.valueOf(PunchyDebug.ARM_L_ROT_Y));
            obj.addProperty("rot_z", Float.valueOf(PunchyDebug.ARM_L_ROT_Z));
            obj.addProperty("scale", Float.valueOf(PunchyDebug.ARM_L_SCALE_X));
        }
        return obj;
    }

    private static JsonObject itemSection(boolean right) {
        JsonObject obj = new JsonObject();
        if (right) {
            obj.addProperty("pos_x", Float.valueOf(PunchyDebug.ITEM_R_POS_X));
            obj.addProperty("pos_y", Float.valueOf(PunchyDebug.ITEM_R_POS_Y));
            obj.addProperty("pos_z", Float.valueOf(PunchyDebug.ITEM_R_POS_Z));
            obj.addProperty("rot_x", Float.valueOf(PunchyDebug.ITEM_R_ROT_X));
            obj.addProperty("rot_y", Float.valueOf(PunchyDebug.ITEM_R_ROT_Y));
            obj.addProperty("rot_z", Float.valueOf(PunchyDebug.ITEM_R_ROT_Z));
            obj.addProperty("scale", Float.valueOf(PunchyDebug.ITEM_R_SCALE_X));
            obj.addProperty("pivot_x", Float.valueOf(PunchyDebug.ITEM_R_PIVOT_X));
            obj.addProperty("pivot_y", Float.valueOf(PunchyDebug.ITEM_R_PIVOT_Y));
            obj.addProperty("pivot_z", Float.valueOf(PunchyDebug.ITEM_R_PIVOT_Z));
        } else {
            obj.addProperty("pos_x", Float.valueOf(PunchyDebug.ITEM_L_POS_X));
            obj.addProperty("pos_y", Float.valueOf(PunchyDebug.ITEM_L_POS_Y));
            obj.addProperty("pos_z", Float.valueOf(PunchyDebug.ITEM_L_POS_Z));
            obj.addProperty("rot_x", Float.valueOf(PunchyDebug.ITEM_L_ROT_X));
            obj.addProperty("rot_y", Float.valueOf(PunchyDebug.ITEM_L_ROT_Y));
            obj.addProperty("rot_z", Float.valueOf(PunchyDebug.ITEM_L_ROT_Z));
            obj.addProperty("scale", Float.valueOf(PunchyDebug.ITEM_L_SCALE_X));
            obj.addProperty("pivot_x", Float.valueOf(PunchyDebug.ITEM_L_PIVOT_X));
            obj.addProperty("pivot_y", Float.valueOf(PunchyDebug.ITEM_L_PIVOT_Y));
            obj.addProperty("pivot_z", Float.valueOf(PunchyDebug.ITEM_L_PIVOT_Z));
        }
        return obj;
    }
}
