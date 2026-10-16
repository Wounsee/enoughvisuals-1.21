package fun.enoughvisuals.systems.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompactConfigCodecTest {

    @Test
    void roundTripKeepsEveryValue() throws IOException {
        JsonElement source = JsonParser.parseString("""
                {"version":3,"name":"pvp","savedAt":1760000000000,
                 "modules":[{"id":"shader_player","enabled":true,"keyBind":-1,
                   "settings":{"glow_size":10.5,"opacity":0.92,"color":-10584833,"mode":"Свечение",
                               "targets":["Игроки","Себя"],"empty":null,"tiny":1.0E-9,"neg":-3}}],
                 "hud":{"viewportWidth":960.0,"elements":[]},
                 "cosmetics":{"hat":"pulse_201"}}
                """);

        JsonElement decoded = CompactConfigCodec.decode(CompactConfigCodec.encode(source));

        // Gson compares numbers by value, so 3 equals the decoded 3.0.
        assertEquals(source, decoded);
    }

    @Test
    void repeatedConfigIsMuchSmallerThanJson() {
        JsonObject root = new JsonObject();
        JsonArray modules = new JsonArray();
        for (int i = 0; i < 90; i++) {
            JsonObject module = new JsonObject();
            module.addProperty("id", "module_" + i);
            module.addProperty("enabled", i % 3 == 0);
            module.addProperty("keyBind", -1);
            JsonObject settings = new JsonObject();
            settings.addProperty("distance", 96.0);
            settings.addProperty("color_source", "Тема");
            settings.addProperty("opacity", 0.75);
            module.add("settings", settings);
            modules.add(module);
        }
        root.add("modules", modules);

        int json = root.toString().getBytes(StandardCharsets.UTF_8).length;
        int compact = CompactConfigCodec.encode(root).length;
        assertTrue(compact * 5 < json, "compact=" + compact + " json=" + json);
    }

    @Test
    void corruptedDataIsRejected() {
        byte[] data = CompactConfigCodec.encode(JsonParser.parseString("{\"a\":[1,2,3],\"b\":\"text\"}"));
        data[data.length - 1] ^= 0x5A;
        assertThrows(IOException.class, () -> CompactConfigCodec.decode(data));
    }
}
