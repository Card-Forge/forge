package forge.sound;

public interface IAudioMusic {
    void play(final Runnable onComplete);
    void pause();
    void resume();
    void stop();
    void dispose();
}
