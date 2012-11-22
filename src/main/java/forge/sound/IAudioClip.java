package forge.sound;

public interface IAudioClip { 
    public void play();
    public boolean isDone();
    public void stop();
    public void loop();
}