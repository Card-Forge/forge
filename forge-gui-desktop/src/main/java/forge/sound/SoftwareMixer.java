/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2012  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package forge.sound;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Sums Forge's sound effects in Java and plays them through a single audio line.
 *
 * <p>javax.sound.sampled hands out one device line per {@link javax.sound.sampled.Clip}, and on
 * ALSA, PulseAudio and PipeWire a line is a separate application stream. Playing effects as Clips
 * therefore costs a stream per overlapping sound and, because a Clip holds its line from open()
 * until close(), a stream per distinct effect for as long as Forge runs. Once the device stops
 * handing lines out, Clip.open() parks in the driver while holding the mixer's own monitor, and
 * whichever thread asked for the sound freezes there -- in a match, the game thread.
 *
 * <p>Mixing the samples ourselves costs one line no matter how many sounds overlap, and that line
 * is given back once nothing has played for {@link #LINE_IDLE_CLOSE_MS}. Only this class's own
 * thread ever talks to the device, so a slow open(), write() or close() can no longer reach the
 * caller.
 */
final class SoftwareMixer {

    /**
     * What the line is opened with, and what every sound is converted to as it loads, so the
     * mixing loop has one case to handle. AudioSystem converts sample depth, signedness, channel
     * count and sample rate into this, so a sound set in any PCM format still plays.
     */
    static final AudioFormat FORMAT =
            new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100f, 16, 2, 4, 44100f, false);

    /** Bytes the mixer moves per pass: ~12ms, so stopping a sound is not audibly late. */
    private static final int FRAMES_PER_WRITE = 512;
    /** ~93ms of slack for a scheduling hiccup, which would otherwise be heard as a dropout. */
    private static final int LINE_BUFFER_FRAMES = 4096;
    /** Close the line when Forge has been quiet this long, rather than holding the device idle. */
    private static final long LINE_IDLE_CLOSE_MS = 3_000L;
    /** A device that refused the line is asked again no more often than this. */
    private static final long LINE_RETRY_MS = 30_000L;
    private static final int MAX_VOICES = 32;
    private static final Set<String> SLOW_REPEAT_SOUNDS = Set.of("draw.mp3", "tap.mp3", "untap.mp3");

    /** Voices still to be mixed. All access is synchronized on this list. */
    private static final List<Voice> voices = new ArrayList<>();
    // Guarded by voices. Weak keys let disposed sound sets release their samples.
    private static final Map<byte[], Long> nextStartFrame = new WeakHashMap<>();
    private static long frameCursor;

    // The fields below belong to the mixer thread alone.
    private static SourceDataLine line;
    private static long lastSound;
    private static long lineUnavailableUntil;

    static {
        final Thread mixer = new Thread(SoftwareMixer::run, "Forge audio mixer");
        mixer.setDaemon(true);
        mixer.start();
    }

    private SoftwareMixer() {
    }

    /** One sound playing once. Handed back to the caller so it can be stopped or waited on. */
    static final class Voice {
        private final byte[] pcm;
        private final float volume;
        private final boolean loop;
        /** Read cursor into {@link #pcm}; mixer thread only. */
        private int position;
        /** Frames of silence still owed before this voice sounds; mixer thread only. */
        private int silentFrames;
        private volatile boolean playing = true;

        private Voice(byte[] pcm, float volume, boolean loop, int delayFrames) {
            this.pcm = pcm;
            this.volume = volume;
            this.loop = loop;
            this.silentFrames = delayFrames;
        }

        void stop() {
            playing = false;
        }

        boolean isPlaying() {
            return playing;
        }
    }

    /**
     * Read a sound file's bytes into {@link #FORMAT}.
     *
     * @throws UnsupportedAudioFileException if the file is not audio this JDK can convert.
     */
    static byte[] decode(byte[] audioFile) throws IOException, UnsupportedAudioFileException {
        try (AudioInputStream source = AudioSystem.getAudioInputStream(new ByteArrayInputStream(audioFile))) {
            if (FORMAT.matches(source.getFormat())) {
                return source.readAllBytes();
            }
            if (!AudioSystem.isConversionSupported(FORMAT, source.getFormat())) {
                throw new UnsupportedAudioFileException("cannot convert " + source.getFormat() + " to " + FORMAT);
            }
            try (AudioInputStream converted = AudioSystem.getAudioInputStream(FORMAT, source)) {
                return converted.readAllBytes();
            }
        }
    }

    static int framesForMillis(long millis) {
        return (int) (millis * FORMAT.getSampleRate() / 1000L);
    }

    static int repeatDelayMillis(String filename) {
        return SLOW_REPEAT_SOUNDS.contains(new File(filename).getName()) ? 70 : SoundSystem.DELAY;
    }

    /**
     * Start {@code pcm} (already in {@link #FORMAT}), spacing repeats on the mixer's sample clock.
     * Returns at once: the caller never waits for the device.
     */
    static Voice play(byte[] pcm, float volume, boolean loop, int repeatDelayMillis) {
        synchronized (voices) {
            voices.removeIf(voice -> !voice.isPlaying());
            if (pcm.length == 0 || voices.size() >= MAX_VOICES) {
                final Voice skipped = new Voice(pcm, volume, loop, 0);
                skipped.stop();
                return skipped;
            }
            final long start = repeatDelayMillis == 0 ? frameCursor
                    : Math.max(frameCursor, nextStartFrame.getOrDefault(pcm, frameCursor));
            final Voice voice = new Voice(pcm, volume, loop, (int) (start - frameCursor));
            if (repeatDelayMillis > 0) {
                nextStartFrame.put(pcm, start + framesForMillis(repeatDelayMillis));
            }
            voices.add(voice);
            voices.notifyAll();
            return voice;
        }
    }

    private static void run() {
        final int[] accumulator = new int[FRAMES_PER_WRITE * FORMAT.getChannels()];
        final byte[] block = new byte[FRAMES_PER_WRITE * FORMAT.getFrameSize()];
        while (true) {
            try {
                if (hasVoices()) {
                    lastSound = System.currentTimeMillis();
                } else if (System.currentTimeMillis() - lastSound >= LINE_IDLE_CLOSE_MS) {
                    closeLine();
                    awaitVoice();
                    continue;
                }
                if (!openLine()) {
                    // Drop queued audio instead of saving a backlog for a device that may return.
                    dropVoices();
                    awaitVoice();
                    continue;
                }
                synchronized (voices) {
                    fill(voices, accumulator, block);
                    frameCursor += FRAMES_PER_WRITE;
                }
                // Blocks until the device has room, which is what paces this loop.
                line.write(block, 0, block.length);
            } catch (RuntimeException ex) {
                handleLineFailure(ex);
            }
        }
    }

    private static boolean hasVoices() {
        synchronized (voices) {
            voices.removeIf(voice -> !voice.isPlaying());
            return !voices.isEmpty();
        }
    }

    private static void awaitVoice() {
        synchronized (voices) {
            while (voices.isEmpty()) {
                try {
                    voices.wait();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        // Do not count the wait against the idle timeout: a sound has just arrived.
        lastSound = System.currentTimeMillis();
    }

    private static void dropVoices() {
        synchronized (voices) {
            for (Voice voice : voices) {
                voice.stop();
            }
            voices.clear();
            nextStartFrame.clear();
        }
    }

    /** Sum one block of every voice, clamp, and write it out as {@link #FORMAT} frames. */
    private static void fill(List<Voice> playing, int[] accumulator, byte[] block) {
        Arrays.fill(accumulator, 0);
        final int channels = FORMAT.getChannels();
        for (Voice voice : playing) {
            if (!voice.isPlaying()) {
                continue;
            }
            int sample = 0;
            while (sample < accumulator.length && voice.silentFrames > 0) {
                voice.silentFrames--;
                sample += channels;
            }
            while (sample < accumulator.length) {
                if (voice.position + 2 > voice.pcm.length) {
                    if (!voice.loop) {
                        voice.stop();
                        break;
                    }
                    voice.position = 0;
                }
                final int value = (voice.pcm[voice.position] & 0xFF) | (voice.pcm[voice.position + 1] << 8);
                accumulator[sample++] += (int) (value * voice.volume);
                voice.position += 2;
            }
        }
        for (int sample = 0, at = 0; sample < accumulator.length; sample++) {
            final int value = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, accumulator[sample]));
            block[at++] = (byte) value;
            block[at++] = (byte) (value >> 8);
        }
    }

    private static boolean openLine() {
        if (line != null) {
            return true;
        }
        if (System.currentTimeMillis() < lineUnavailableUntil) {
            return false;
        }
        try {
            line = AudioSystem.getSourceDataLine(FORMAT);
            line.open(FORMAT, LINE_BUFFER_FRAMES * FORMAT.getFrameSize());
            line.start();
            return true;
        } catch (LineUnavailableException | IllegalArgumentException ex) {
            // Asked for once rather than per sound, so a busy or absent device costs one message
            // and one retry timer instead of a stall on every effect for the rest of the session.
            handleLineFailure(ex);
            return false;
        }
    }

    private static void handleLineFailure(Exception failure) {
        lineUnavailableUntil = System.currentTimeMillis() + LINE_RETRY_MS;
        dropVoices();
        System.err.println("Error playing sound: " + failure);
        try {
            closeLine();
        } catch (RuntimeException ex) {
            System.err.println("Error closing sound output: " + ex);
        }
    }

    private static void closeLine() {
        if (line == null) {
            return;
        }
        // Everything still audible was written during the idle timeout, so there is no tail to
        // drain. close() can block in the driver, which is safe here and would not be on a caller.
        final SourceDataLine closing = line;
        line = null;
        try {
            closing.stop();
        } finally {
            try {
                closing.flush();
            } finally {
                closing.close();
            }
        }
    }
}
