/*
 * Decompiled with CFR 0.152.
 */
package dev.redstones.mediaplayerinfo.impl;

import dev.redstones.mediaplayerinfo.IMediaSession;
import dev.redstones.mediaplayerinfo.MediaPlayerInfo;
import java.util.Collections;
import java.util.List;

public class DummyMediaPlayerInfo
implements MediaPlayerInfo {
    @Override
    public List<IMediaSession> getMediaSessions() {
        return Collections.emptyList();
    }
}

