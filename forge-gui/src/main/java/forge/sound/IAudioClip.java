package forge.sound;

import forge.properties.NewConstants;

public interface IAudioClip {
    public static final String PathToSound = NewConstants._RES_ROOT+"sound";

    public void play();
    public boolean isDone();
    public void stop();
    public void loop();
}
