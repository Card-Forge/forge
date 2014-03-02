package forge.sound;

public interface IAudioClip {
    public static final String PathToSound = "res/sound";

    public void play();
    public boolean isDone();
    public void stop();
    public void loop();
}
