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
import forge.gui.GuiBase;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;

import java.io.File;

public class AudioClip implements IAudioClip {
    // On iOS, sound effects play through libGDX Music (AVAudioPlayer/AudioQueue) instead
    // of Sound (OpenAL). The OpenAL effects engine and the AVAudioPlayer music are two
    // independent CoreAudio clients whose render cycles beat against each other, producing
    // a slow periodic crackle in the co-summed music (~14-32s, un-fixable by rate-matching
    // alone). Playing effects in the SAME AVAudioPlayer/mediaserverd domain as the music
    // collapses everything to one clock and eliminates the beat; the now-unused OpenAL
    // engine is suspended in Main (forge-gui-ios). Trade-off: a same-type effect can't
    // overlap itself (one player per type) - re-triggers while still playing are dropped
    // rather than truncating the sound. Evaluated per-instance (not a class-load-time
    // constant) so it can never latch the wrong value and strand effects on the suspended
    // OpenAL engine.
    private final boolean ios;

    private Sound clip;       // non-iOS: OpenAL sound
    private Music musicClip;   // iOS: AVAudioPlayer-backed effect

    public static AudioClip createClip(String filename) {
        FileHandle fileHandle = Gdx.files.absolute(filename);
        if (!fileHandle.exists()) { return null; }
        return new AudioClip(fileHandle);
    }

    public static AudioClip createClip(File file) {
        if(file == null) return null;
        FileHandle fileHandle = Gdx.files.absolute(file.getPath());
        if(!fileHandle.exists()) return null;
        return new AudioClip(fileHandle);
    }

    private AudioClip(final FileHandle fileHandle) {
        ios = GuiBase.isIOS();
        try {
            if (ios) {
                //route effects through the AVAudioPlayer domain (see class comment)
                musicClip = Gdx.audio.newMusic(fileHandle);
            } else {
                //investigate why sound is called outside edt -> Forge.getAssets().getSound(fileHandle), seems the audioclip is cached in SoundSystem instead of using it directly from assetManager
                clip = Gdx.audio.newSound(fileHandle);
            }
        }
        catch (Exception ex) {
            System.err.println("Unable to load sound file: " + fileHandle.toString());
        }
    }

    public final void play(float value) {
        if (ios) {
            //Drop a re-trigger while the effect is still playing: the single per-type
            //AVAudioPlayer can't overlap itself, so re-triggering would truncate the
            //sound into machine-gun fragments. One clean sound per duration-window. Also
            //avoids the pointless 30ms game-thread stall on a dropped event.
            if (musicClip == null || musicClip.isPlaying()) {
                return;
            }
            try {
                musicClip.stop();           //reset to the start
                musicClip.setLooping(false);
                musicClip.setVolume(value);
                musicClip.play();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }
        //non-iOS (OpenAL): keep the historical 30ms voice-reuse delay
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
        if (ios) {
            if (musicClip == null || musicClip.isPlaying()) {
                return;
            }
            try {
                musicClip.setLooping(true);
                musicClip.play();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }
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
        try {
            if (musicClip != null) {
                musicClip.dispose();
                musicClip = null;
            }
            if (clip != null) {
                clip.dispose();
                clip = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public final void stop() {
        try {
            if (ios) {
                if (musicClip != null) {
                    musicClip.stop();
                }
            } else {
                if (clip != null) {
                    clip.stop();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public final boolean isDone() {
        if (ios) {
            //a Music-backed effect is "done" when it isn't currently playing, so
            //synchronized effects re-trigger only after the previous one finishes
            //(matches the desktop one-at-a-time contract for synced effects)
            return musicClip == null || !musicClip.isPlaying();
        }
        return clip != null;//TODO: Make this smarter if Sound supports marking as done
    }
}
