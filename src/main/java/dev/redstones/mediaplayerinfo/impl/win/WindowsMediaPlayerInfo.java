/*
 * Decompiled with CFR 0.152.
 */
package dev.redstones.mediaplayerinfo.impl.win;

import dev.redstones.mediaplayerinfo.IMediaSession;
import dev.redstones.mediaplayerinfo.MediaPlayerInfo;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.List;

public class WindowsMediaPlayerInfo
implements MediaPlayerInfo {
    @Override
    public native List<IMediaSession> getMediaSessions();

    static {
        try {
            Path tempDir = Files.createTempDirectory("mediaplayerinfo-", new FileAttribute[0]);
            Path dllFile = tempDir.resolve("MediaPlayerInfo.dll");
            try (InputStream inputStream = WindowsMediaPlayerInfo.class.getResourceAsStream("/mediaplayerinfo/natives/win/MediaPlayerInfo.dll");){
                if (inputStream == null) {
                    throw new IOException("Resource not found: /mediaplayerinfo/natives/win/MediaPlayerInfo.dll");
                }
                Files.write(dllFile, inputStream.readAllBytes(), new OpenOption[0]);
            }
            System.load(dllFile.toAbsolutePath().toString());
            try {
                Files.deleteIfExists(dllFile);
                Files.deleteIfExists(tempDir);
            }
            catch (IOException ignored) {
                dllFile.toFile().deleteOnExit();
                tempDir.toFile().deleteOnExit();
            }
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to load MediaPlayerInfo.dll", e);
        }
    }
}

