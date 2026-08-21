package fun.enoughvisuals.systems.webpanel;

import com.cinemamod.mcef.MCEF;
import org.cef.browser.CefBrowser;
import org.cef.handler.CefAudioHandler;
import org.cef.misc.CefAudioParameters;
import org.cef.misc.DataPointer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/** Captures CEF PCM and replays every browser independently with panel distance gain. */
final class CefSpatialAudio implements CefAudioHandler {
    private static final CefSpatialAudio INSTANCE = new CefSpatialAudio();
    private static final ConcurrentHashMap<Integer, Double> GAINS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, AudioStream> STREAMS = new ConcurrentHashMap<>();
    private static volatile boolean installed;

    private CefSpatialAudio() {
    }

    static boolean register(CefBrowser browser) {
        if (browser == null || !install()) return false;
        GAINS.putIfAbsent(browser.getIdentifier(), 1.0);
        return true;
    }

    static boolean setGain(CefBrowser browser, double gain) {
        if (browser == null || !install()) return false;
        int id = browser.getIdentifier();
        double clamped = Math.max(0.0, Math.min(1.0, gain));
        GAINS.put(id, clamped);
        AudioStream stream = STREAMS.get(id);
        if (stream != null) {
            CefNativeAudio.setMuted(browser, true);
            stream.setGain(clamped);
        }
        return true;
    }

    static void unregister(CefBrowser browser) {
        if (browser == null) return;
        int id = browser.getIdentifier();
        GAINS.remove(id);
        AudioStream stream = STREAMS.remove(id);
        if (stream != null) stream.close();
    }

    private static boolean install() {
        if (installed) return CefNativeAudio.available();
        synchronized (CefSpatialAudio.class) {
            if (installed) return CefNativeAudio.available();
            if (!CefNativeAudio.available() || !MCEF.isInitialized()) return false;
            try {
                MCEF.getClient().addAudioHandler(INSTANCE);
                installed = true;
                return true;
            } catch (Throwable ignored) {
                return false;
            }
        }
    }

    @Override
    public boolean getAudioParameters(CefBrowser browser, CefAudioParameters parameters) {
        return false;
    }

    @Override
    public void onAudioStreamStarted(CefBrowser browser, CefAudioParameters parameters, int channels) {
        int id = browser.getIdentifier();
        CefNativeAudio.setMuted(browser, true);
        AudioStream replacement = AudioStream.open(parameters.sampleRate, channels,
                GAINS.getOrDefault(id, 1.0));
        if (replacement == null) {
            CefNativeAudio.setMuted(browser, false);
            return;
        }
        CefNativeAudio.setMuted(browser, true);
        AudioStream previous = STREAMS.put(id, replacement);
        if (previous != null) previous.close();
    }

    @Override
    public void onAudioStreamPacket(CefBrowser browser, DataPointer data, int frames, long pts) {
        AudioStream stream = STREAMS.get(browser.getIdentifier());
        if (stream != null) stream.accept(data, frames);
    }

    @Override
    public void onAudioStreamStopped(CefBrowser browser) {
        AudioStream stream = STREAMS.remove(browser.getIdentifier());
        if (stream != null) stream.close();
    }

    @Override
    public void onAudioStreamError(CefBrowser browser, String message) {
        onAudioStreamStopped(browser);
    }

    private static final class AudioStream implements AutoCloseable {
        private static final byte[] STOP = new byte[0];
        private final int channels;
        private final SourceDataLine line;
        private final ArrayBlockingQueue<byte[]> packets = new ArrayBlockingQueue<>(12);
        private final Thread worker;
        private volatile double gain;
        private volatile boolean closed;

        private AudioStream(int sampleRate, int channels, SourceDataLine line, double gain) {
            this.channels = Math.max(1, channels);
            this.line = line;
            this.gain = gain;
            this.worker = new Thread(this::playbackLoop, "Enough-CEF-Spatial-Audio");
            this.worker.setDaemon(true);
            this.worker.start();
        }

        static AudioStream open(int sampleRate, int channels, double gain) {
            try {
                AudioFormat format = new AudioFormat(sampleRate, 16, 2, true, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(format, Math.max(4096, sampleRate / 12 * format.getFrameSize()));
                line.start();
                return new AudioStream(sampleRate, channels, line, gain);
            } catch (Throwable ignored) {
                return null;
            }
        }

        void setGain(double value) {
            gain = value;
            if (value <= 0.0001) {
                packets.clear();
                line.flush();
            }
        }

        void accept(DataPointer packet, int frames) {
            double packetGain = gain;
            if (closed || frames <= 0 || packetGain <= 0.0001) return;
            try {
                DataPointer channelArray = packet.forCapacity(channels * Long.BYTES);
                DataPointer left = channelArray.getData(0).forCapacity(frames * Float.BYTES).withAlignment(2);
                DataPointer right = channels > 1
                        ? channelArray.getData(1).forCapacity(frames * Float.BYTES).withAlignment(2)
                        : left;
                byte[] pcm = new byte[frames * 4];
                for (int frame = 0; frame < frames; frame++) {
                    writeSample(pcm, frame * 4, left.getFloat(frame) * packetGain);
                    writeSample(pcm, frame * 4 + 2, right.getFloat(frame) * packetGain);
                }
                if (!packets.offer(pcm)) {
                    packets.poll();
                    packets.offer(pcm);
                }
            } catch (Throwable ignored) {
            }
        }

        private static void writeSample(byte[] output, int offset, double sample) {
            int value = (int) Math.round(Math.max(-1.0, Math.min(1.0, sample)) * 32767.0);
            output[offset] = (byte) value;
            output[offset + 1] = (byte) (value >>> 8);
        }

        private void playbackLoop() {
            try {
                while (!closed) {
                    byte[] packet = packets.poll(250, TimeUnit.MILLISECONDS);
                    if (packet == null) continue;
                    if (packet == STOP) break;
                    line.write(packet, 0, packet.length);
                }
            } catch (Throwable ignored) {
            } finally {
                line.stop();
                line.flush();
                line.close();
            }
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            packets.clear();
            packets.offer(STOP);
            worker.interrupt();
        }
    }
}
