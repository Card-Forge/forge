package forge.sound;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;

public class AudioMusic implements IAudioMusic {
    private Music music;

    public AudioMusic(String filename) {
        music = Gdx.audio.newMusic(Gdx.files.absolute(filename));
    }

    public void play() {
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
        music.stop();
    }

    public void dispose() {
        music.stop();
        music.dispose();
    }
}
