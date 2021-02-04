package forge.sound;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Music.OnCompletionListener;

public class AudioMusic implements IAudioMusic {
    private Music music;

    public AudioMusic(String filename) {
        music = Gdx.audio.newMusic(Gdx.files.absolute(filename));
    }

    public void play(final Runnable onComplete) {
        music.setOnCompletionListener(new OnCompletionListener() {
            @Override
            public void onCompletion(Music music) {
                onComplete.run();
            }
        });
        music.play();
    }

    public void pause() {
        music.pause();
    }

    public void resume() {
        if (!music.isPlaying()) {
            music.play();
        }
    }

    public void stop() {
        music.setOnCompletionListener(null); //prevent firing if stopped manually
        music.stop();
    }

    public void dispose() {
        stop();
        music.dispose();
    }

    @Override
    public void setVolume(float value) {
        music.setVolume(value);
    }
}
