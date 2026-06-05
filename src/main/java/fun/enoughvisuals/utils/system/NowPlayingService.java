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
            String text,
            boolean isPlaying,
            boolean hasArtwork,
            byte[] artworkPng
    ) {}

    private static final Logger LOGGER =
            LoggerFactory.getLogger("EnoughVisuals/NowPlaying");

    private static final TrackInfo EMPTY =
            new TrackInfo(false, "", false, false, null);

    private static volatile TrackInfo cached = EMPTY;
    private static volatile boolean booted = false;
    private static volatile String lastMediaDebug = "";
    private static volatile String lastFailureDebug = "";

    private static final long POLL_INTERVAL = 1200L;

    private static final Thread TRACKER = new Thread(() -> {
        while (!Thread.currentThread().isInterrupted()) {
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
            TRACKER.start();
            LOGGER.warn("MediaSession polling started");
        }
        return cached;
    }

    private static TrackInfo read() {
        try {
            List<IMediaSession> sessions = MediaPlayerInfo.INSTANCE.getMediaSessions();

            if (sessions == null || sessions.isEmpty()) return EMPTY;

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

            if (best == null) return EMPTY;

            MediaInfo media = best.getMedia();
            if (media == null) return EMPTY;

            String title = safe(media.getTitle());
            String artist = safe(media.getArtist());

            if (title.isBlank() && artist.isBlank()) return EMPTY;

            String text = artist.isEmpty()
                    ? title
                    : artist + " - " + title;

            byte[] artwork = media.getArtworkPng();
            debugMedia(text, best.getOwner(), artwork);

            return new TrackInfo(
                    true,
                    text,
                    media.isPlaying(),
                    artwork != null && artwork.length > 0,
                    artwork
            );

        } catch (Throwable t) {
            debugFailure(t);
            return EMPTY;
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
