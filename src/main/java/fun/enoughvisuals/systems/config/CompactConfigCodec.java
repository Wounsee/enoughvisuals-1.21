package fun.enoughvisuals.systems.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * Compact binary form of a JSON config tree. Not encryption: the file is a
 * string table plus a typed value tree, raw-DEFLATE compressed.
 *
 * <pre>
 * file   := "EVC" version(1) crc32(4, of the inflated body) deflate(body)
 * body   := varint(stringCount) string* value
 * string := varint(byteLength) utf8
 * value  := NULL | FALSE | TRUE | INT varint(zigzag) | FLOAT f32 | DOUBLE f64
 *         | STRING varint(index) | ARRAY varint(n) value* | OBJECT varint(n) (varint(keyIndex) value)*
 * </pre>
 *
 * Every key and string value is stored once in the table, so the repeated
 * module and setting ids of a config cost a single varint each.
 */
public final class CompactConfigCodec {
    private static final byte[] MAGIC = {'E', 'V', 'C'};
    private static final int FORMAT_VERSION = 1;
    private static final int MAX_INFLATED_BYTES = 32 * 1024 * 1024;

    private static final int NULL = 0, FALSE = 1, TRUE = 2, INT = 3, FLOAT = 4, DOUBLE = 5,
            STRING = 6, ARRAY = 7, OBJECT = 8;

    private CompactConfigCodec() {
    }

    public static boolean isCompact(byte[] data) {
        return data != null && data.length > MAGIC.length + 5
                && data[0] == MAGIC[0] && data[1] == MAGIC[1] && data[2] == MAGIC[2];
    }

    /** Uncompressed body; cheap enough to fingerprint a snapshot every autosave poll. */
    public static byte[] body(JsonElement root) {
        try {
            Map<String, Integer> index = new HashMap<>();
            List<String> table = new ArrayList<>();
            collectStrings(root, index, table);

            ByteArrayOutputStream bytes = new ByteArrayOutputStream(4096);
            DataOutputStream out = new DataOutputStream(bytes);
            writeVarInt(out, table.size());
            for (String value : table) {
                byte[] utf8 = value.getBytes(StandardCharsets.UTF_8);
                writeVarInt(out, utf8.length);
                out.write(utf8);
            }
            writeValue(out, root, index);
            out.flush();
            return bytes.toByteArray();
        } catch (IOException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    public static byte[] encode(JsonElement root) {
        return pack(body(root));
    }

    public static byte[] pack(byte[] body) {
        CRC32 crc = new CRC32();
        crc.update(body);

        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION, true);
        try {
            deflater.setInput(body);
            deflater.finish();
            ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(64, body.length / 3));
            out.write(MAGIC, 0, MAGIC.length);
            out.write(FORMAT_VERSION);
            int checksum = (int) crc.getValue();
            out.write(checksum >>> 24);
            out.write(checksum >>> 16);
            out.write(checksum >>> 8);
            out.write(checksum);
            byte[] buffer = new byte[8192];
            while (!deflater.finished()) {
                int written = deflater.deflate(buffer);
                out.write(buffer, 0, written);
            }
            return out.toByteArray();
        } finally {
            deflater.end();
        }
    }

    public static JsonElement decode(byte[] data) throws IOException {
        if (!isCompact(data)) throw new IOException("not a compact config");
        if (data[3] != FORMAT_VERSION) throw new IOException("unsupported compact config version " + data[3]);
        int expectedCrc = ((data[4] & 0xFF) << 24) | ((data[5] & 0xFF) << 16)
                | ((data[6] & 0xFF) << 8) | (data[7] & 0xFF);

        byte[] body;
        Inflater inflater = new Inflater(true);
        try (InputStream in = new InflaterInputStream(
                new java.io.ByteArrayInputStream(data, 8, data.length - 8), inflater)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(data.length * 4);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
                if (out.size() > MAX_INFLATED_BYTES) throw new IOException("compact config is too large");
            }
            body = out.toByteArray();
        } finally {
            inflater.end();
        }

        CRC32 crc = new CRC32();
        crc.update(body);
        if ((int) crc.getValue() != expectedCrc) throw new IOException("compact config checksum mismatch");

        DataInputStream in = new DataInputStream(new java.io.ByteArrayInputStream(body));
        int count = readVarInt(in);
        if (count < 0 || count > body.length) throw new IOException("corrupt string table");
        String[] table = new String[count];
        for (int i = 0; i < count; i++) {
            int length = readVarInt(in);
            if (length < 0 || length > body.length) throw new IOException("corrupt string");
            byte[] utf8 = new byte[length];
            in.readFully(utf8);
            table[i] = new String(utf8, StandardCharsets.UTF_8);
        }
        return readValue(in, table, 0);
    }

    private static void collectStrings(JsonElement element, Map<String, Integer> index, List<String> table) {
        if (element == null || element.isJsonNull()) return;
        if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                intern(entry.getKey(), index, table);
                collectStrings(entry.getValue(), index, table);
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) collectStrings(child, index, table);
        } else if (element.getAsJsonPrimitive().isString()) {
            intern(element.getAsString(), index, table);
        }
    }

    private static void intern(String value, Map<String, Integer> index, List<String> table) {
        if (index.putIfAbsent(value, table.size()) == null) table.add(value);
    }

    private static void writeValue(DataOutputStream out, JsonElement element, Map<String, Integer> index)
            throws IOException {
        if (element == null || element.isJsonNull()) {
            out.write(NULL);
        } else if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            out.write(OBJECT);
            writeVarInt(out, object.size());
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                writeVarInt(out, index.get(entry.getKey()));
                writeValue(out, entry.getValue(), index);
            }
        } else if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            out.write(ARRAY);
            writeVarInt(out, array.size());
            for (JsonElement child : array) writeValue(out, child, index);
        } else {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isBoolean()) {
                out.write(primitive.getAsBoolean() ? TRUE : FALSE);
            } else if (primitive.isString()) {
                out.write(STRING);
                writeVarInt(out, index.get(primitive.getAsString()));
            } else {
                writeNumber(out, primitive.getAsDouble());
            }
        }
    }

    private static void writeNumber(DataOutputStream out, double value) throws IOException {
        if (value == Math.rint(value) && Math.abs(value) < (1L << 53) && !(value == 0 && 1 / value < 0)) {
            out.write(INT);
            long longValue = (long) value;
            writeVarLong(out, (longValue << 1) ^ (longValue >> 63));
        } else if ((double) (float) value == value) {
            out.write(FLOAT);
            out.writeFloat((float) value);
        } else {
            out.write(DOUBLE);
            out.writeDouble(value);
        }
    }

    private static JsonElement readValue(DataInputStream in, String[] table, int depth) throws IOException {
        if (depth > 64) throw new IOException("compact config is nested too deeply");
        int type = in.read();
        switch (type) {
            case NULL:
                return JsonNull.INSTANCE;
            case FALSE:
                return new JsonPrimitive(false);
            case TRUE:
                return new JsonPrimitive(true);
            case INT: {
                long zigzag = readVarLong(in);
                // Gson parses config numbers as Double; keep that contract for settings.
                return new JsonPrimitive((double) ((zigzag >>> 1) ^ -(zigzag & 1)));
            }
            case FLOAT:
                return new JsonPrimitive((double) in.readFloat());
            case DOUBLE:
                return new JsonPrimitive(in.readDouble());
            case STRING:
                return new JsonPrimitive(string(table, readVarInt(in)));
            case ARRAY: {
                int size = readVarInt(in);
                JsonArray array = new JsonArray(Math.min(size, 1024));
                for (int i = 0; i < size; i++) array.add(readValue(in, table, depth + 1));
                return array;
            }
            case OBJECT: {
                int size = readVarInt(in);
                JsonObject object = new JsonObject();
                for (int i = 0; i < size; i++) {
                    String key = string(table, readVarInt(in));
                    object.add(key, readValue(in, table, depth + 1));
                }
                return object;
            }
            case -1:
                throw new EOFException("truncated compact config");
            default:
                throw new IOException("unknown compact value type " + type);
        }
    }

    private static String string(String[] table, int index) throws IOException {
        if (index < 0 || index >= table.length) throw new IOException("string index out of range");
        return table[index];
    }

    private static void writeVarInt(DataOutputStream out, int value) throws IOException {
        writeVarLong(out, value & 0xFFFFFFFFL);
    }

    private static void writeVarLong(DataOutputStream out, long value) throws IOException {
        while ((value & ~0x7FL) != 0) {
            out.write((int) ((value & 0x7F) | 0x80));
            value >>>= 7;
        }
        out.write((int) value);
    }

    private static int readVarInt(DataInputStream in) throws IOException {
        long value = readVarLong(in);
        if (value > Integer.MAX_VALUE) throw new IOException("varint out of range");
        return (int) value;
    }

    private static long readVarLong(DataInputStream in) throws IOException {
        long result = 0;
        for (int shift = 0; shift < 64; shift += 7) {
            int b = in.read();
            if (b < 0) throw new EOFException("truncated varint");
            result |= (long) (b & 0x7F) << shift;
            if ((b & 0x80) == 0) return result;
        }
        throw new IOException("varint is too long");
    }
}
