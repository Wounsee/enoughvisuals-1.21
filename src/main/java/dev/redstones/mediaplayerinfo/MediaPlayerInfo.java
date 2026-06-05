/*
 * Decompiled with CFR 0.152.
 */
package dev.redstones.mediaplayerinfo;

import dev.redstones.mediaplayerinfo.IMediaSession;
import dev.redstones.mediaplayerinfo.impl.DummyMediaPlayerInfo;
import dev.redstones.mediaplayerinfo.impl.win.WindowsMediaPlayerInfo;
import java.util.List;

public interface MediaPlayerInfo {
    public static final MediaPlayerInfo INSTANCE = SystemMediaPlayerInfo.getInstance();

    public List<IMediaSession> getMediaSessions();

    public static class SystemMediaPlayerInfo {
        private static final MediaPlayerInfo instance = System.getProperty("os.name").toLowerCase().startsWith("windows") ? new WindowsMediaPlayerInfo() : new DummyMediaPlayerInfo();

        public static MediaPlayerInfo getInstance() {
            return instance;
        }
    }
}

