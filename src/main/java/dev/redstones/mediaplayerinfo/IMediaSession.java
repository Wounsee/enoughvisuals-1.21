/*
 * Decompiled with CFR 0.152.
 */
package dev.redstones.mediaplayerinfo;

import dev.redstones.mediaplayerinfo.MediaInfo;

public interface IMediaSession {
    public String getOwner();

    public MediaInfo getMedia();

    public void play();

    public void pause();

    public void playPause();

    public void stop();

    public void next();

    public void previous();

    public void swapCycle();

    public int getCycleType();
}

