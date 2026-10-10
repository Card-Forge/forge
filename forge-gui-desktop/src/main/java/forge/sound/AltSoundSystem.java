package forge.sound;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.sound.sampled.UnsupportedAudioFileException;

import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.sound.SoftwareMixer.Voice;

/**
 * The alternate sound system: sounds addressed by file path and capped per file, rather than a
 * clip per effect. It exists for people whose sound used to disappear on the pooled-{@code Clip}
 * path, and the preference switches it on without a restart.
 *
 * <p>It plays through {@link SoftwareMixer} like {@link AudioClip} does. It used to start a thread
 * per sound, each opening an audio line of its own -- which is the freeze this system was offered
 * as a workaround for, so the workaround reproduced it.
 *
 * @author Agetian
 */
public final class AltSoundSystem {
    /** How many copies of one sound may overlap. */
    private static final int MAX_SOUND_ITERATIONS = 5;

    private static final Map<String, Sound> sounds = new ConcurrentHashMap<>();

    private AltSoundSystem() {
    }

    /**
     * Start a sound, unless enough copies of it are already sounding. Synchronized play means one
     * copy at a time.
     *
     * <p>Runs on the caller's thread: handing a voice to the mixer is a list insertion, and only
     * the first play of a file decodes it.
     */
    public static synchronized void play(final String filename, final boolean isSynchronized) {
        final Sound sound = sounds.computeIfAbsent(filename, AltSoundSystem::load);
        if (sound.pcm == null) {
            return;
        }
        sound.voices.removeIf(voice -> !voice.isPlaying());
        if (sound.voices.size() >= (isSynchronized ? 1 : MAX_SOUND_ITERATIONS)) {
            return;
        }
        final float volume = FModel.getPreferences().getPrefInt(FPref.UI_VOL_SOUNDS) / 100f;
        sound.voices.add(SoftwareMixer.play(sound.pcm, volume, false, 0));
    }

    private static Sound load(final String filename) {
        final File file = new File(filename);
        if (!file.exists()) {
            return new Sound(null);
        }
        try {
            return new Sound(AudioClip.samplesOf(file));
        } catch (IOException | UnsupportedAudioFileException ex) {
            System.err.println("Unable to load sound file: " + filename + " - " + ex.getMessage());
            return new Sound(null);
        }
    }

    /** One sound file: its samples, and the voices of it that may still be sounding. */
    private static final class Sound {
        /** Samples in {@link SoftwareMixer#FORMAT}, or null if the file could not be loaded. */
        private final byte[] pcm;
        private final List<Voice> voices = new CopyOnWriteArrayList<>();

        private Sound(final byte[] pcm) {
            this.pcm = pcm;
        }
    }
}
