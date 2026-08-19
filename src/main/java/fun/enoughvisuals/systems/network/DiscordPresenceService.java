package fun.enoughvisuals.systems.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Minimal synchronous Discord IPC client. All pipe I/O stays on one worker. */
public final class DiscordPresenceService {
    private static final Logger LOGGER = LoggerFactory.getLogger("EnoughVisuals/DiscordRPC");
    private static final long APPLICATION_ID = 1526949056717258823L;
    private static final long STARTED_AT = System.currentTimeMillis() / 1000L;
    private static final long REPUBLISH_INTERVAL_MS = TimeUnit.MINUTES.toMillis(2);
    private static final DiscordPresenceService INSTANCE = new DiscordPresenceService();

    private final AtomicBoolean started = new AtomicBoolean();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "EnoughVisuals-DiscordRPC");
        thread.setDaemon(true);
        return thread;
    });

    private volatile DiscordPipe pipe;
    private volatile String lastRole;
    private volatile long lastActivityUpdateAt;

    private DiscordPresenceService() {
    }

    public static DiscordPresenceService getInstance() {
        return INSTANCE;
    }

    public void start() {
        if (!started.compareAndSet(false, true)) return;
        LOGGER.info("Starting Discord Rich Presence");
        scheduler.scheduleWithFixedDelay(this::refreshOrReconnectSafely, 0, 15, TimeUnit.SECONDS);
    }

    private void refreshOrReconnectSafely() {
        if (!started.get()) return;
        try {
            DiscordPipe current = pipe;
            if (current == null) {
                current = DiscordPipe.connect(APPLICATION_ID);
                pipe = current;
                lastRole = null;
                lastActivityUpdateAt = 0L;
                LOGGER.info("Discord Rich Presence connected through {}", current.path());
            }

            String role = RemoteConfig.getInstance().getLauncherRole();
            long now = System.currentTimeMillis();
            if (role.equals(lastRole) && now - lastActivityUpdateAt < REPUBLISH_INTERVAL_MS) return;

            current.publish(role);
            lastRole = role;
            lastActivityUpdateAt = System.currentTimeMillis();
            LOGGER.info("Discord Rich Presence activity published");
        } catch (Throwable error) {
            closePipe();
            lastRole = null;
            lastActivityUpdateAt = 0L;
            LOGGER.debug("Discord RPC unavailable: {}", rootMessage(error));
        }
    }

    public void stop() {
        if (!started.compareAndSet(true, false)) return;
        scheduler.shutdownNow();
        closePipe();
    }

    private void closePipe() {
        DiscordPipe current = pipe;
        pipe = null;
        if (current == null) return;
        try {
            current.close();
        } catch (Throwable ignored) {
        }
    }

    private static String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) current = current.getCause();
        return current.toString();
    }

    private static final class DiscordPipe implements AutoCloseable {
        private static final int OP_HANDSHAKE = 0;
        private static final int OP_FRAME = 1;
        private static final int OP_CLOSE = 2;
        private static final int OP_PING = 3;
        private static final int OP_PONG = 4;
        private static final int MAX_PAYLOAD_BYTES = 1024 * 1024;

        private final String path;
        private final RandomAccessFile file;

        private DiscordPipe(String path, RandomAccessFile file) {
            this.path = path;
            this.file = file;
        }

        static DiscordPipe connect(long applicationId) throws IOException {
            IOException lastError = null;
            for (int index = 0; index < 10; index++) {
                String path = "\\\\.\\pipe\\discord-ipc-" + index;
                RandomAccessFile file = null;
                try {
                    file = new RandomAccessFile(path, "rw");
                    DiscordPipe pipe = new DiscordPipe(path, file);
                    JsonObject handshake = new JsonObject();
                    handshake.addProperty("v", 1);
                    handshake.addProperty("client_id", Long.toUnsignedString(applicationId));
                    pipe.writeFrame(OP_HANDSHAKE, handshake);
                    Frame response = pipe.readApplicationFrame();
                    if (response.opcode() != OP_FRAME) {
                        throw new IOException("Unexpected Discord handshake opcode " + response.opcode());
                    }
                    String event = string(response.payload(), "evt");
                    if (!"READY".equals(event)) {
                        throw new IOException("Discord handshake rejected: " + response.payload());
                    }
                    return pipe;
                } catch (IOException error) {
                    lastError = error;
                    if (file != null) {
                        try {
                            file.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
            }
            throw lastError == null ? new IOException("Discord IPC pipe not found") : lastError;
        }

        void publish(String role) throws IOException {
            JsonObject timestamps = new JsonObject();
            timestamps.addProperty("start", STARTED_AT);

            JsonObject assets = new JsonObject();
            assets.addProperty("large_image", "icon");
            assets.addProperty("large_text", "Enough Visuals");

            JsonObject activity = new JsonObject();
            activity.addProperty("type", 0);
            activity.addProperty("details", "Build: 2.6");
            activity.addProperty("state", "Role: " + role);
            activity.add("timestamps", timestamps);
            activity.add("assets", assets);

            com.google.gson.JsonArray buttons = new com.google.gson.JsonArray();
            buttons.add(button("Site", "https://enoughvisuals.fun"));
            buttons.add(button("Telegram", "https://t.me/enoughvisuals"));
            activity.add("buttons", buttons);

            JsonObject args = new JsonObject();
            args.addProperty("pid", ProcessHandle.current().pid());
            args.add("activity", activity);

            JsonObject command = new JsonObject();
            command.addProperty("cmd", "SET_ACTIVITY");
            command.add("args", args);
            command.addProperty("nonce", UUID.randomUUID().toString());

            writeFrame(OP_FRAME, command);
            Frame response = readApplicationFrame();
            if (response.opcode() != OP_FRAME) {
                throw new IOException("Unexpected Discord activity opcode " + response.opcode());
            }
            if ("ERROR".equals(string(response.payload(), "evt"))) {
                throw new IOException("Discord rejected activity: " + response.payload());
            }
        }

        private Frame readApplicationFrame() throws IOException {
            while (true) {
                Frame frame = readFrame();
                if (frame.opcode() == OP_PING) {
                    writeFrame(OP_PONG, frame.payload());
                    continue;
                }
                if (frame.opcode() == OP_CLOSE) {
                    throw new IOException("Discord closed IPC: " + frame.payload());
                }
                return frame;
            }
        }

        private Frame readFrame() throws IOException {
            byte[] header = new byte[8];
            file.readFully(header);
            ByteBuffer headerBuffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
            int opcode = headerBuffer.getInt();
            int length = headerBuffer.getInt();
            if (length < 0 || length > MAX_PAYLOAD_BYTES) {
                throw new IOException("Invalid Discord IPC payload length " + length);
            }
            byte[] payload = new byte[length];
            file.readFully(payload);
            JsonObject json;
            try {
                json = length == 0
                        ? new JsonObject()
                        : JsonParser.parseString(new String(payload, StandardCharsets.UTF_8)).getAsJsonObject();
            } catch (RuntimeException error) {
                throw new IOException("Invalid Discord IPC JSON", error);
            }
            return new Frame(opcode, json);
        }

        private void writeFrame(int opcode, JsonObject payload) throws IOException {
            byte[] bytes = payload.toString().getBytes(StandardCharsets.UTF_8);
            ByteBuffer frame = ByteBuffer.allocate(8 + bytes.length).order(ByteOrder.LITTLE_ENDIAN);
            frame.putInt(opcode);
            frame.putInt(bytes.length);
            frame.put(bytes);
            file.write(frame.array());
        }

        String path() {
            return path;
        }

        @Override
        public void close() throws IOException {
            try {
                JsonObject close = new JsonObject();
                close.addProperty("code", 1000);
                close.addProperty("message", "Client shutting down");
                writeFrame(OP_CLOSE, close);
            } catch (IOException ignored) {
            } finally {
                file.close();
            }
        }

        private static JsonObject button(String label, String url) {
            JsonObject button = new JsonObject();
            button.addProperty("label", label);
            button.addProperty("url", url);
            return button;
        }

        private static String string(JsonObject object, String key) {
            return object != null && object.has(key) && !object.get(key).isJsonNull()
                    ? object.get(key).getAsString()
                    : "";
        }

        private record Frame(int opcode, JsonObject payload) {
        }
    }
}
