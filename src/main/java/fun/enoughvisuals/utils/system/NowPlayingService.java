package fun.enoughvisuals.utils.system;

import dev.redstones.mediaplayerinfo.IMediaSession;
import dev.redstones.mediaplayerinfo.MediaInfo;
import dev.redstones.mediaplayerinfo.MediaPlayerInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public final class NowPlayingService {

    public record TrackInfo(
            boolean active,
            String title,
            String artist,
            boolean isPlaying,
            long position,
            long duration,
            long sampledAtMs,
            boolean hasArtwork,
            byte[] artworkPng
    ) {
        public String text() {
            return artist == null || artist.isBlank() ? title : artist + " - " + title;
        }

        public float progress() {
            return progressAt(System.currentTimeMillis());
        }

        float progressAt(long nowMs) {
            if (duration <= 0L) return 0f;
            double current = position;
            if (isPlaying && sampledAtMs > 0L) {
                current += Math.max(0L, nowMs - sampledAtMs) / 1_000.0;
            }
            return Math.max(0f, Math.min(1f, (float) (current / duration)));
        }
    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger("NowPlaying");

    private static final TrackInfo EMPTY =
            new TrackInfo(false, "", "", false, 0L, 0L, 0L, false, null);

    private static volatile TrackInfo cached = EMPTY;
    private static volatile IMediaSession activeSession;
    private static volatile boolean booted = false;
    private static volatile String lastMediaDebug = "";
    private static volatile String lastFailureDebug = "";

    private static final long POLL_INTERVAL = 1200L;

    private static final Thread TRACKER = new Thread(() -> {
        while (!Thread.currentThread().isInterrupted()) {
            if (!MediaSessionSupport.isAvailable()) {
                cached = EMPTY;
                activeSession = null;
                return;
            }
            try {
                cached = read();
                Thread.sleep(POLL_INTERVAL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Throwable t) {
                LOGGER.warn("poll fail: {}", t.toString());
            }
        }
    }, "NowPlaying");

    static {
        TRACKER.setDaemon(true);
    }

    private NowPlayingService() {}

    public static TrackInfo getNowPlaying() {
        if (!booted) {
            booted = true;
            if (MediaSessionSupport.isAvailable()) {
                TRACKER.start();
                LOGGER.warn("MediaSession polling started");
            }
        }
        return cached;
    }

    public static void previous() {
        control(IMediaSession::previous);
    }

    public static void playPause() {
        control(IMediaSession::playPause);
    }

    public static void next() {
        control(IMediaSession::next);
    }

    private static void control(java.util.function.Consumer<IMediaSession> action) {
        IMediaSession session = activeSession;
        if (session == null || !MediaSessionSupport.isAvailable()) return;
        try {
            action.accept(session);
        } catch (LinkageError e) {
            activeSession = null;
            MediaSessionSupport.disable("native media library failed to link: " + e);
        } catch (Throwable t) {
            debugFailure(t);
        }
    }

    private static TrackInfo read() {
        if (!MediaSessionSupport.isAvailable()) {
            activeSession = null;
            return EMPTY;
        }
        try {
            List<IMediaSession> sessions = fetchSessions();

            if (sessions == null || sessions.isEmpty()) {
                activeSession = null;
                return EMPTY;
            }

            IMediaSession best = null;

                              
            for (IMediaSession s : sessions) {
                if (s == null) continue;

                MediaInfo m = s.getMedia();
                if (m == null) continue;

                if (m.isPlaying()) {
                    best = s;
                    break;
                }
            }

                                            
            if (best == null) {
                for (IMediaSession s : sessions) {
                    if (s == null) continue;
                    if (s.getMedia() != null) {
                        best = s;
                        break;
                    }
                }
            }

            if (best == null) {
                activeSession = null;
                return EMPTY;
            }

            MediaInfo media = best.getMedia();
            if (media == null) {
                activeSession = null;
                return EMPTY;
            }

            String title = safe(media.getTitle());
            String artist = safe(media.getArtist());

            if (title.isBlank() && artist.isBlank()) {
                activeSession = null;
                return EMPTY;
            }

            String text = artist.isEmpty()
                    ? title
                    : artist + " - " + title;

            byte[] artwork = media.getArtworkPng();
            debugMedia(text, best.getOwner(), artwork);
            activeSession = best;

            return new TrackInfo(
                    true,
                    title,
                    artist,
                    media.isPlaying(),
                    Math.max(0L, media.getPosition()),
                    Math.max(0L, media.getDuration()),
                    System.currentTimeMillis(),
                    artwork != null && artwork.length > 0,
                    artwork
            );

        } catch (LinkageError e) {
            activeSession = null;
            MediaSessionSupport.disable("native media library failed to link: " + e);
            return EMPTY;
        } catch (Throwable t) {
            activeSession = null;
            debugFailure(t);
            return EMPTY;
        }
    }

    /**
     * The marker is written before control leaves for native code, so a JVM
     * killed inside the library is recognised on the next launch.
     */
    private static List<IMediaSession> fetchSessions() {
        MediaSessionSupport.armProbe();
        try {
            return MediaPlayerInfo.INSTANCE.getMediaSessions();
        } finally {
            // Runs after a normal return and after a thrown exception alike:
            // both prove the JVM survived, so only a hard crash keeps the marker.
            MediaSessionSupport.confirmNativeCall();
        }
    }

    private static void debugMedia(String text, String owner, byte[] artwork) {
        int artworkLength = artwork == null ? -1 : artwork.length;
        String key = text + "|" + safe(owner) + "|" + artworkLength;
        if (!key.equals(lastMediaDebug)) {
            lastMediaDebug = key;
            LOGGER.warn("track='{}' owner='{}' artwork_bytes={}", text, safe(owner), artworkLength);
        }
    }

    private static void debugFailure(Throwable t) {
        String key = t.getClass().getName() + ":" + String.valueOf(t.getMessage());
        if (!key.equals(lastFailureDebug)) {
            lastFailureDebug = key;
            LOGGER.warn("poll read fail", t);
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
