package forge.sound;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;

// iOS sound effect, played through libGDX Music (AVAudioPlayer/AudioQueue) instead
// of Sound (OpenAL). The OpenAL effects engine and the AVAudioPlayer music are two
// independent CoreAudio clients whose render cycles beat against each other, producing
// a slow periodic crackle in the co-summed music (~14-32s, un-fixable by rate-matching
// alone). Playing effects in the SAME AVAudioPlayer/mediaserverd domain as the music
// collapses everything to one clock and eliminates the beat; the now-unused OpenAL
// engine is suspended in Main (forge-gui-ios). Trade-off: a same-type effect can't
// overlap itself (one player per type) - re-triggers while still playing are dropped
// rather than truncating the sound.
public class MusicAudioClip implements IAudioClip {
    private Music musicClip;

    MusicAudioClip(final FileHandle fileHandle) {
        try {
            musicClip = Gdx.audio.newMusic(fileHandle);
        }
        catch (Exception ex) {
            System.err.println("Unable to load sound file: " + fileHandle.toString());
        }
    }

    @Override
    public final void play(float value) {
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
    }

    @Override
    public final void loop() {
        if (musicClip == null || musicClip.isPlaying()) {
            return;
        }
        try {
            musicClip.setLooping(true);
            musicClip.play();
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public final void stop() {
        try {
            if (musicClip != null) {
                musicClip.stop();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public final boolean isDone() {
        //a Music-backed effect is "done" when it isn't currently playing, so
        //synchronized effects re-trigger only after the previous one finishes
        //(matches the desktop one-at-a-time contract for synced effects)
        return musicClip == null || !musicClip.isPlaying();
    }
}
