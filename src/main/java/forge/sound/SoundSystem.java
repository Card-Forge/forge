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

import java.io.*;
import javax.sound.sampled.*;

/**
 * SoundSystem - a simple sound playback system for Forge.
 * Do not use directly. Instead, use the {@link forge.sound.Sounds} enumeration.
 * 
 * @author Agetian
 */
public class SoundSystem {
    private Clip clip;

    public SoundSystem(String filename) {
        try {
            AudioInputStream stream = AudioSystem.getAudioInputStream(new File(filename));
            AudioFormat format = stream.getFormat();
            DataLine.Info info = new DataLine.Info(Clip.class, stream.getFormat(), ((int) stream.getFrameLength() * format.getFrameSize()));
            clip = (Clip) AudioSystem.getLine(info);

            clip.open(stream);

        } catch (IOException ex) {
            System.err.println("Unable to load sound file: " + filename);
        } catch (LineUnavailableException ex) {
            System.err.println("Error initializing sound system: " + ex);
        } catch (UnsupportedAudioFileException ex) {
            System.err.println("Unsupported file type of the sound file: " + ex);
        }
    }

    public void play() {
        if (clip != null) {
            clip.stop();
            clip.flush();
            clip.setFramePosition(0);
            clip.start();
        }
    }

    public void loop() {
        if (clip != null) {
            clip.stop();
            clip.flush();
            clip.setFramePosition(0);
            clip.loop(clip.LOOP_CONTINUOUSLY);
        }
    }

    public void stop() {
        if (clip != null) {
            clip.stop();
        }
    }

    public boolean isDone() {
        if (clip != null) {
            return !clip.isRunning();
        } else {
            return false;
        }
    }

}