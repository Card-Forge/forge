package forge.sound;

public interface IAudioClip {
    void play();
    boolean isDone();
    void stop();
    void loop();
}
