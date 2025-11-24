package forge.sound;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;
import forge.Forge;

public class AudioMusic implements IAudioMusic {
    private final Music music;
    private final FileHandle file;

    private boolean started = false;
    private float pauseTimestamp = 0f;

    public AudioMusic(String filename) {
        file = Gdx.files.absolute(filename);
        music = Forge.getAssets().getMusic(file);
    }

    public void play(final Runnable onComplete) {
        if (music == null)
            return;
        started = true;
        music.setOnCompletionListener(music -> {
            if (music.isPlaying())
                onComplete.run(); //Pausing a track can cause OpenAL to still fire its completion listener after a bit.
        });
        music.play();
    }

    public void pause() {
        if (music == null)
            return;
        if (music.isPlaying()) {
            pauseTimestamp = music.getPosition();
            music.pause();
        }
    }

    public void resume() {
        if (music == null)
            return;
        if(!started) {
            //Resumed without playing. Completion listener won't be set up right.
            System.err.println("Audio " + file.name() + " resumed without a call to AudioMusic.play()");
        }
        if (!music.isPlaying()) {
            music.setPosition(pauseTimestamp);
            music.play();
        }
    }

    public void stop() {
        if (music == null)
            return;
        started = false;
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

    @Override
    public boolean isPlaying() {
        return music.isPlaying();
    }
}
