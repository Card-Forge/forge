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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
//import forge.Forge;

public class AudioClip implements IAudioClip {
    private Sound clip;

    public static AudioClip createClip(String filename) {
        FileHandle fileHandle = Gdx.files.absolute(filename);
        if (!fileHandle.exists()) { return null; }
        return new AudioClip(fileHandle);
    }

    private AudioClip(final FileHandle fileHandle) {
        try {
            //investigate why sound is called outside edt -> Forge.getAssets().getSound(fileHandle), seems the audioclip is cached in SoundSystem instead of using it directly from assetManager
            clip = Gdx.audio.newSound(fileHandle);
        }
        catch (Exception ex) {
            System.err.println("Unable to load sound file: " + fileHandle.toString());
        }
    }

    public final void play(float value) {
        if (clip == null) {
            return;
        }
        try {
            Thread.sleep(SoundSystem.DELAY);
        }
        catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        try {
            clip.play(value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public final void loop() {
        if (clip == null) {
            return;
        }
        try {
            Thread.sleep(SoundSystem.DELAY);
        }
        catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        try {
            clip.loop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void dispose() {
        if (clip != null) {
            try {
                clip.dispose();
            } catch (Exception e) {
                e.printStackTrace();
            }
            clip = null;
        }
    }

    public final void stop() {
        if (clip == null) {
            return;
        }
        try {
            clip.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public final boolean isDone() {
        return clip != null;//TODO: Make this smarter if Sound supports marking as done
    }
}
