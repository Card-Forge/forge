package forge.sound;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Music.OnCompletionListener;
import com.badlogic.gdx.files.FileHandle;
import forge.Forge;
import forge.adventure.stage.GameHUD;

public class AudioMusic implements IAudioMusic {
    private Music music;
    private FileHandle file;

    public AudioMusic(String filename) {
        file = Gdx.files.absolute(filename);
        music = Forge.getAssets().getMusic(file);
    }

    public void play(final Runnable onComplete) {
        if (music == null)
            return;
        if (Forge.isMobileAdventureMode) {
            if (GameHUD.getInstance().audioIsPlaying())
                return;
        }
        music.setOnCompletionListener(new OnCompletionListener() {
            @Override
            public void onCompletion(Music music) {
                onComplete.run();
            }
        });
        music.play();
    }

    public void pause() {
        if (music == null)
            return;
        if (music.isPlaying())
            music.pause();
    }

    public void resume() {
        if (music == null)
            return;
        if (!music.isPlaying()) {
            music.play();
        }
    }

    public void stop() {
        if (music == null)
            return;
        music.setOnCompletionListener(null); //prevent firing if stopped manually
        music.stop();
    }

    public void dispose() {
        stop();
        Forge.getAssets().manager().unload(file.path());
    }

    @Override
    public void setVolume(float value) {
        if (music == null)
            return;
        music.setVolume(value);
    }
}
