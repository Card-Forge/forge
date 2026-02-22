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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.function.Supplier;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.google.common.io.Files;
import com.sipgate.mp3wav.Converter;

/**
 * SoundSystem - a simple sound playback system for Forge.
 * Do not use directly. Instead, use the {@link forge.sound.SoundEffectType} enumeration.
 *
 * @author Agetian
 */
public class AudioClip implements IAudioClip {
    private final int maxSize = 16;
    private final String filename;
    private final List<ClipWrapper> clips;
    private boolean failed;
    private static final Map<String, byte[]> audioClips = new HashMap<>(30);

    public static byte[] getAudioClips(File file) throws IOException {
        if (!audioClips.containsKey(file.toString()) ) {
            audioClips.put(file.toString(), Converter.convertFrom(Files.asByteSource(file).openStream()).toByteArray());
        }
        return audioClips.get(file.toString());
    }

    public static boolean fileExists(String fileName) {
        File fSound = SoundSystem.instance.getSoundResource(fileName);
        return fSound != null && fSound.exists();
    }

    public AudioClip(final String filename) {
        this.filename = filename;
        clips = new ArrayList<>(maxSize);
        addClip();
    }

    @Override
    public final void play(float value) {
        if (clips.stream().anyMatch(ClipWrapper::isRunning)) {
            // introduce small delay to make a batch sounds more granular,
            // e.g. when you auto-tap 4 lands the 4 tap sounds should
            // not become completely merged
            waitSoundSystemDelay();
        }
        getIdleClip().start(value);
    }

    @Override
    public final void loop() {
        getIdleClip().loop();
    }

    @Override
    public void dispose() {
        for (byte[] b : audioClips.values()) {
            b = null;
        }
        audioClips.clear();
    }

    @Override
    public final void stop() {
        for (ClipWrapper clip: clips) {
            clip.stop();
        }
    }

    @Override
    public final boolean isDone() {
        return clips.stream().noneMatch(ClipWrapper::isRunning);
    }

    private ClipWrapper getIdleClip() {
        return clips.stream()
                .filter(clip -> !clip.isRunning())
                .findFirst()
                .orElseGet(this::addClip);
    }

    private ClipWrapper addClip() {
        if (clips.size() < maxSize && !failed) {
            ClipWrapper clip = new ClipWrapper(filename);
            if (clip.isFailed()) {
                failed = true;
            } else {
                clips.add(clip);
            }
            return clip;
        }
        return ClipWrapper.Dummy;
    }



    private static boolean waitSoundSystemDelay() {
        try {
            Thread.sleep(SoundSystem.DELAY);
            return true;
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    static class ClipWrapper {
        private final Clip clip;
        private boolean started;
        static final ClipWrapper Dummy = new ClipWrapper();

        private ClipWrapper() {
            clip = null;
        }

        ClipWrapper(String filename) {
            clip = createClip(filename);
            if (clip != null) {
                clip.addLineListener(this::clipStateChanged);
            }
        }

        boolean isFailed() {
            return null == clip;
        }

        void start(float volume) {
            if (null == clip) {
                return;
            }
            synchronized (this) {
                applyVolume(volume);
                clip.setMicrosecondPosition(0);
                this.started = false;
                clip.start();
                // with JRE 1.8.0_211 if another thread called clip.setMicrosecondPosition
                // just now, it would deadlock. To prevent this we synchronize this method
                // and wait
                wait(() -> this.started);
            }
        }

        void loop() {
            if (null == clip) {
                return;
            }
            synchronized (this) {
                clip.setMicrosecondPosition(0);
                this.started = false;
                clip.loop(Clip.LOOP_CONTINUOUSLY);
                wait(() -> this.started);
            }
        }

        void stop() {
            if (null == clip) {
                return;
            }
            synchronized (this) {
                clip.stop();
            }
        }

        boolean isRunning() {
            return clip != null && (clip.isRunning() || clip.isActive());
        }

        private Clip createClip(String filename) {
            File fSound = SoundSystem.instance.getSoundResource(filename);
            if (fSound == null || !fSound.exists()) {
                throw new IllegalArgumentException("Sound file " + fSound + " does not exist, cannot make a clip of it");
            }
            try {
                ByteArrayInputStream bis = new ByteArrayInputStream(getAudioClips(fSound));
                AudioInputStream stream = AudioSystem.getAudioInputStream(bis);
                AudioFormat format = stream.getFormat();
                DataLine.Info info = new DataLine.Info(Clip.class, stream.getFormat(), ((int) stream.getFrameLength() * format.getFrameSize()));
                Clip clip = (Clip) AudioSystem.getLine(info);
                clip.open(stream);
                return clip;
            } catch (IOException ex) {
                System.err.println("Unable to load sound file: " + filename);
            } catch (LineUnavailableException ex) {
                System.err.println("Error initializing sound system: " + ex);
            } catch (UnsupportedAudioFileException ex) {
                System.err.println("Unsupported file type of the sound file: " + fSound + " - " + ex.getMessage());
                return null;
            }
            throw new MissingResourceException("Sound clip failed to load", this.getClass().getName(), filename);
        }

        private void applyVolume(float volume) {
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float dB = (float) (20.0 * Math.log10(Math.max(volume, 0.0001)));
                dB = Math.max(dB, gain.getMinimum());
                dB = Math.min(dB, gain.getMaximum());
                gain.setValue(dB);
            }
        }

        private void clipStateChanged(LineEvent lineEvent) {
            started |= lineEvent.getType() == LineEvent.Type.START;
        }

        private void wait(Supplier<Boolean> completed) {
            final int attempts = 5;
            for (int i = 0; i < attempts; i++) {
                if (completed.get() || !waitSoundSystemDelay()) {
                    break;
                }
            }
        }
    }
}
