package fun.enoughvisuals.systems.network;

import fun.crashsystem.jdrpc.DiscordIPC;
import fun.crashsystem.jdrpc.DiscordIPCConfig;
import fun.crashsystem.jdrpc.activity.Activity;
import fun.crashsystem.jdrpc.activity.ActivityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Non-blocking Discord Rich Presence lifecycle backed by pure-Java jDRPC. */
public final class DiscordPresenceService {
    private static final Logger LOGGER = LoggerFactory.getLogger("EnoughVisuals/DiscordRPC");
    private static final long APPLICATION_ID = 1526949056717258823L;
    private static final long STARTED_AT = System.currentTimeMillis() / 1000L;
    private static final DiscordPresenceService INSTANCE = new DiscordPresenceService();

    private final AtomicBoolean started = new AtomicBoolean();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "EnoughVisuals-DiscordRPC");
        thread.setDaemon(true);
        return thread;
    });
    private volatile DiscordIPC client;
    private volatile String lastRole = "";

    private DiscordPresenceService() {
    }

    public static DiscordPresenceService getInstance() {
        return INSTANCE;
    }

    public void start() {
        if (!started.compareAndSet(false, true)) return;
        scheduler.execute(this::connect);
        scheduler.scheduleWithFixedDelay(this::refreshSafely, 5, 15, TimeUnit.SECONDS);
    }

    private void connect() {
        try {
            DiscordIPCConfig config = DiscordIPCConfig.builder()
                    .clientId(APPLICATION_ID)
                    .reconnect(true)
                    .maxReconnectAttempts(8)
                    .build();
            DiscordIPC next = DiscordIPC.create(config);
            client = next;
            next.connectAsync()
                    .thenRun(this::refreshSafely)
                    .exceptionally(error -> {
                        LOGGER.debug("Discord RPC unavailable: {}", rootMessage(error));
                        return null;
                    });
        } catch (Throwable error) {
            LOGGER.debug("Discord RPC initialization failed: {}", rootMessage(error));
        }
    }

    private void refreshSafely() {
        try {
            DiscordIPC current = client;
            if (current == null || !current.isConnected()) return;
            String role = RemoteConfig.getInstance().getLauncherRole();
            if (role.equals(lastRole)) return;
            lastRole = role;
            Activity activity = new Activity.Builder()
                    .setType(ActivityType.PLAYING)
                    .setDetails("Build: 2.6")
                    .setState("Role: " + role)
                    .setLargeImage("icon", "Enough Visuals")
                    .setStartTimestamp(STARTED_AT)
                    .addButton("Site", "https://enoughvisuals.fun")
                    .addButton("Telegram", "https://t.me/enoughvisuals")
                    .build();
            current.setActivityAsync(activity).exceptionally(error -> {
                LOGGER.debug("Discord RPC update failed: {}", rootMessage(error));
                return null;
            });
        } catch (Throwable error) {
            LOGGER.debug("Discord RPC refresh failed: {}", rootMessage(error));
        }
    }

    public void stop() {
        if (!started.compareAndSet(true, false)) return;
        scheduler.shutdownNow();
        DiscordIPC current = client;
        client = null;
        if (current != null) {
            try {
                current.close();
            } catch (Throwable ignored) {
            }
        }
    }

    private static String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) current = current.getCause();
        return current.toString();
    }
}
