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
import java.util.MissingResourceException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;



/**
 * SoundSystem - a simple sound playback system for Forge.
 * Do not use directly. Instead, use the {@link forge.sound.SoundEffectType} enumeration.
 * 
 * @author Agetian
 */
public class AudioClip implements IAudioClip{
    private final Clip clip;
    private final int SOUND_SYSTEM_DELAY = 30;

    private final static String PathToSound = "res/sound";  
    
    public static boolean fileExists(String fileName) { 
        File fSound = new File(PathToSound, fileName);
        return fSound.exists();
    }
    
    public AudioClip(final String filename) {
        File fSound = new File(PathToSound, filename);
        if ( !fSound.exists() )
            throw new IllegalArgumentException("Sound file " + fSound.toString() + " does not exist, cannot make a clip of it");
        
        try {
            AudioInputStream stream = AudioSystem.getAudioInputStream(fSound);
            AudioFormat format = stream.getFormat();
            DataLine.Info info = new DataLine.Info(Clip.class, stream.getFormat(), ((int) stream.getFrameLength() * format.getFrameSize()));
            clip = (Clip) AudioSystem.getLine(info);
            clip.open(stream);
            return;

        } catch (IOException ex) {
            System.err.println("Unable to load sound file: " + filename);
        } catch (LineUnavailableException ex) {
            System.err.println("Error initializing sound system: " + ex);
        } catch (UnsupportedAudioFileException ex) {
            System.err.println("Unsupported file type of the sound file: " + ex);
        }
        throw new MissingResourceException("Sound clip failed to load", this.getClass().getName(), filename);
    }

    public final void play() {
        if (null == clip)
            return;
        clip.setMicrosecondPosition(0);
        try {
            Thread.sleep(SOUND_SYSTEM_DELAY);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        clip.start();
    }

    public final void loop() {
        if (null == clip)
            return;
        clip.setMicrosecondPosition(0);
        try {
            Thread.sleep(SOUND_SYSTEM_DELAY);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        clip.loop(Clip.LOOP_CONTINUOUSLY);
    }

    public final void stop() {
        if (null == clip)
            return;
        clip.stop();
    }

    public final boolean isDone() {
        if (null == clip)
            return false;
        return !clip.isRunning();
    }
}
