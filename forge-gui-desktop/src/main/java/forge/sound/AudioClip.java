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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.sound.sampled.UnsupportedAudioFileException;

import com.google.common.io.Files;
import com.sipgate.mp3wav.Converter;

import forge.sound.SoftwareMixer.Voice;

/**
 * SoundSystem - a simple sound playback system for Forge.
 * Do not use directly. Instead, use the {@link forge.sound.SoundEffectType} enumeration.
 *
 * <p>A clip is its decoded samples, nothing more: playing it hands a voice to
 * {@link SoftwareMixer}, which sums every voice onto one audio line. Overlapping plays cost a read
 * cursor apiece rather than a device line apiece.
 *
 * @author Agetian
 */
public class AudioClip implements IAudioClip {
    /**
     * Enough overlap for anything Forge triggers at once, and a bound on a runaway event storm.
     */
    private static final int MAX_VOICES = 16;

    private static final Map<String, byte[]> audioClips = new ConcurrentHashMap<>(30);

    /** This sound in {@link SoftwareMixer#FORMAT}, or null if it could not be loaded. */
    private final byte[] pcm;
    /** Voices handed to the mixer that may still be sounding. */
    private final List<Voice> voices = new CopyOnWriteArrayList<>();
    private volatile float volume = 1f;
    /** When the next voice may start sounding, so a batch stays granular. See {@link #play}. */
    private volatile long nextStart;

    public static byte[] getAudioClips(File file) throws IOException {
        // The file's own format, which is what AltSoundSystem wants; the mixer converts its copy.
        byte[] cached = audioClips.get(file.toString());
        if (cached == null) {
            cached = Converter.convertFrom(Files.asByteSource(file).openStream()).toByteArray();
            audioClips.put(file.toString(), cached);
        }
        return cached;
    }

    public static boolean fileExists(String fileName) {
        File fSound = SoundSystem.instance.getSoundResource(fileName);
        return fSound != null && fSound.exists();
    }

    public AudioClip(final String filename) {
        pcm = decode(filename);
    }

    /**
     * A sound that will not load leaves this clip silent. It used to throw, out of a call made
     * while resolving the stack -- a question about audio is not worth interrupting a game for.
     */
    private static byte[] decode(final String filename) {
        File fSound = SoundSystem.instance.getSoundResource(filename);
        if (fSound == null || !fSound.exists()) {
            System.err.println("Sound file does not exist, cannot make a clip of it: " + filename);
            return null;
        }
        try {
            return SoftwareMixer.decode(getAudioClips(fSound));
        } catch (IOException ex) {
            System.err.println("Unable to load sound file: " + filename);
        } catch (UnsupportedAudioFileException ex) {
            System.err.println("Unsupported file type of the sound file: " + fSound + " - " + ex.getMessage());
        }
        return null;
    }

    @Override
    public final void play(float value) {
        volume = value;
        // A batch of one sound -- four lands tapping together -- should stay granular instead of
        // summing into one louder copy, so each voice starts SoundSystem.DELAY after the one
        // before it. The mixer owes the voice that much silence; the old code got the same spacing
        // by sleeping the delay on the caller, which during a match is the game thread.
        final long now = System.currentTimeMillis();
        final long startAt = isDone() ? now : Math.max(now, nextStart);
        nextStart = startAt + SoundSystem.DELAY;
        start(value, false, SoftwareMixer.framesForMillis(startAt - now));
    }

    @Override
    public final void loop() {
        start(volume, true, 0);
    }

    private void start(float value, boolean loop, int delayFrames) {
        voices.removeIf(voice -> !voice.isPlaying());
        if (pcm == null || voices.size() >= MAX_VOICES) {
            return;
        }
        voices.add(SoftwareMixer.play(pcm, value, loop, delayFrames));
    }

    @Override
    public void dispose() {
        stop();
        audioClips.clear();
    }

    @Override
    public final void stop() {
        for (Voice voice : voices) {
            voice.stop();
        }
        voices.clear();
    }

    @Override
    public final boolean isDone() {
        voices.removeIf(voice -> !voice.isPlaying());
        return voices.isEmpty();
    }
}
