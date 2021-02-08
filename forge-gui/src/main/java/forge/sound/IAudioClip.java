package forge.sound;

public interface IAudioClip {
    void play(float value);
    boolean isDone();
    void stop();
    void loop();
}
