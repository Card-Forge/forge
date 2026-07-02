package forge.sound;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;

/**
 *
 * @author agetian
 */
class AltSoundMixer implements Runnable {
    private static final Object LOCK = new Object();
    private static final float SAMPLE_RATE = 44100f;
    private static final int CHANNELS = 2;
    private static final int SAMPLE_SIZE_BITS = 16;
    private static final int BYTES_PER_SAMPLE = SAMPLE_SIZE_BITS / 8;
    private static final int FRAME_SIZE = CHANNELS * BYTES_PER_SAMPLE;
    private static final int BUFFER_FRAMES = 512;
    private static final int BUFFER_BYTES = BUFFER_FRAMES * FRAME_SIZE;
    private static final int TAP_DELAY_MS = 70;
    private static final long SOUND_DELAY_FRAMES = millisecondsToFrames(SoundSystem.DELAY);
    private static final long TAP_DELAY_FRAMES = millisecondsToFrames(TAP_DELAY_MS);
    private static final int MAX_SYNCED_SOUND_ITERATIONS = 1;
    private static final int MAX_UNSYNCED_SOUND_ITERATIONS = 16;
    private static final int MAX_TOTAL_SOUND_ITERATIONS = 32;
    private static final AudioFormat MIX_FORMAT = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED, SAMPLE_RATE, SAMPLE_SIZE_BITS, CHANNELS, FRAME_SIZE, SAMPLE_RATE, false);

    private static final Map<String, CachedSound> soundCache = new HashMap<>();
    private static final Map<String, Integer> soundsPlaying = new HashMap<>();
    private static final Map<String, Long> nextStartFrame = new HashMap<>();
    private static final List<ActiveSound> activeSounds = new ArrayList<>();
    private static int totalSoundsPlaying;
    private static long frameCursor;
    private static boolean started;

    public static void play(String filename, boolean isSync) {
        CachedSound sound;
        try {
            sound = loadSound(filename);
        } catch (UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
            return;
        }

        synchronized (LOCK) {
            int maxIterations = isSync ? MAX_SYNCED_SOUND_ITERATIONS : MAX_UNSYNCED_SOUND_ITERATIONS;
            if (totalSoundsPlaying >= MAX_TOTAL_SOUND_ITERATIONS
                    || soundsPlaying.getOrDefault(filename, 0) >= maxIterations) {
                return;
            }

            startMixer();

            long startFrame = Math.max(frameCursor, nextStartFrame.getOrDefault(filename, 0L));
            nextStartFrame.put(filename, startFrame + getDelayFrames(filename));
            soundsPlaying.merge(filename, 1, Integer::sum);
            totalSoundsPlaying++;
            activeSounds.add(new ActiveSound(filename, sound, startFrame));
            LOCK.notifyAll();
        }
    }

    private static long getDelayFrames(String filename) {
        String soundName = new File(filename).getName();
        if ("draw.mp3".equals(soundName) || "tap.mp3".equals(soundName) || "untap.mp3".equals(soundName)) {
            return TAP_DELAY_FRAMES;
        }
        return SOUND_DELAY_FRAMES;
    }

    private static long millisecondsToFrames(int milliseconds) {
        return milliseconds * (long) SAMPLE_RATE / 1000L;
    }

    private static CachedSound loadSound(String filename) throws UnsupportedAudioFileException, IOException {
        synchronized (LOCK) {
            CachedSound sound = soundCache.get(filename);
            if (sound != null) {
                return sound;
            }
        }

        File soundFile = new File(filename);
        if (!soundFile.exists()) {
            throw new IOException("Sound file does not exist: " + filename);
        }

        byte[] converted = AudioClip.getAudioClips(soundFile);
        try (ByteArrayInputStream bis = new ByteArrayInputStream(converted);
                AudioInputStream sourceStream = AudioSystem.getAudioInputStream(bis);
                AudioInputStream pcmStream = AudioSystem.getAudioInputStream(MIX_FORMAT, sourceStream)) {
            byte[] data = pcmStream.readAllBytes();
            CachedSound sound = new CachedSound(data, data.length / FRAME_SIZE);
            synchronized (LOCK) {
                CachedSound existing = soundCache.get(filename);
                if (existing != null) {
                    return existing;
                }
                soundCache.put(filename, sound);
                return sound;
            }
        }
    }

    private static void startMixer() {
        if (started) {
            return;
        }

        started = true;
        Thread mixerThread = new Thread(new AltSoundMixer(), "Forge alternate sound mixer");
        mixerThread.setDaemon(true);
        mixerThread.start();
    }

    @Override
    public void run() {
        SourceDataLine audioLine;
        try {
            audioLine = openAudioLine();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
            synchronized (LOCK) {
                started = false;
            }
            return;
        }

        audioLine.start();

        int[] mixBuffer = new int[BUFFER_FRAMES * CHANNELS];
        byte[] outputBuffer = new byte[BUFFER_BYTES];

        while (true) {
            float volume;
            synchronized (LOCK) {
                mixActiveSounds(mixBuffer);
                volume = FModel.getPreferences().getPrefInt(FPref.UI_VOL_SOUNDS) / 100f;
            }

            writeMixedAudio(mixBuffer, outputBuffer, volume);
            audioLine.write(outputBuffer, 0, outputBuffer.length);
        }
    }

    private static SourceDataLine openAudioLine() throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, MIX_FORMAT);

        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            if (mixer.isLineSupported(info)) {
                SourceDataLine line = AudioSystem.getSourceDataLine(MIX_FORMAT, mixerInfo);
                line.open(MIX_FORMAT, BUFFER_BYTES * 4);
                return line;
            }
        }

        SourceDataLine line = AudioSystem.getSourceDataLine(MIX_FORMAT);
        line.open(MIX_FORMAT, BUFFER_BYTES * 4);
        return line;
    }

    private static void mixActiveSounds(int[] mixBuffer) {
        Arrays.fill(mixBuffer, 0);

        for (Iterator<ActiveSound> iterator = activeSounds.iterator(); iterator.hasNext();) {
            ActiveSound active = iterator.next();
            mixSound(active, mixBuffer);
            if (active.positionFrame >= active.sound.frameCount) {
                iterator.remove();
                unregisterSound(active.filename);
            }
        }

        frameCursor += BUFFER_FRAMES;
    }

    private static void mixSound(ActiveSound active, int[] mixBuffer) {
        long relativeStart = active.startFrame - frameCursor;
        if (relativeStart >= BUFFER_FRAMES) {
            return;
        }

        int outputFrame = Math.max(0, (int) relativeStart);
        int inputFrame = active.positionFrame;

        int framesToMix = Math.min(BUFFER_FRAMES - outputFrame, active.sound.frameCount - inputFrame);
        for (int frame = 0; frame < framesToMix; frame++) {
            int inputOffset = (inputFrame + frame) * FRAME_SIZE;
            int outputOffset = (outputFrame + frame) * CHANNELS;
            for (int channel = 0; channel < CHANNELS; channel++) {
                int sampleOffset = inputOffset + channel * BYTES_PER_SAMPLE;
                int sample = (active.sound.data[sampleOffset] & 0xff) | (active.sound.data[sampleOffset + 1] << 8);
                mixBuffer[outputOffset + channel] += sample;
            }
        }

        if (relativeStart <= 0) {
            active.positionFrame = inputFrame + framesToMix;
        }
    }

    private static void writeMixedAudio(int[] mixBuffer, byte[] outputBuffer, float volume) {
        for (int i = 0; i < mixBuffer.length; i++) {
            int sample = Math.round(mixBuffer[i] * volume);
            sample = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, sample));
            int outputOffset = i * BYTES_PER_SAMPLE;
            outputBuffer[outputOffset] = (byte) sample;
            outputBuffer[outputOffset + 1] = (byte) (sample >> 8);
        }
    }

    private static void unregisterSound(String soundName) {
        if (soundsPlaying.containsKey(soundName) && soundsPlaying.get(soundName) > 1) {
            soundsPlaying.merge(soundName, -1, Integer::sum);
        } else {
            soundsPlaying.remove(soundName);
        }
        if (totalSoundsPlaying > 0) {
            totalSoundsPlaying--;
        }
    }

    private static class CachedSound {
        private final byte[] data;
        private final int frameCount;

        private CachedSound(byte[] data, int frameCount) {
            this.data = data;
            this.frameCount = frameCount;
        }
    }

    private static class ActiveSound {
        private final String filename;
        private final CachedSound sound;
        private final long startFrame;
        private int positionFrame;

        private ActiveSound(String filename, CachedSound sound, long startFrame) {
            this.filename = filename;
            this.sound = sound;
            this.startFrame = startFrame;
        }
    }
}

public class AltSoundSystem extends Thread {

    private final String filename;
    private final boolean isSync;

    public AltSoundSystem(String wavfile, boolean synced) {
        filename = wavfile;
        isSync = synced;
    }

    @Override
    public void run() {
        AltSoundMixer.play(filename, isSync);
    }
}
