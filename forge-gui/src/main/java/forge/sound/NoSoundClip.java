package forge.sound;

// This class is used as a stub for case when sound is off or a needed sound file is missing
public class NoSoundClip implements IAudioClip {

    @Override
    public void play() { }
    @Override
    public boolean isDone() { return false; }


    @Override
    public void stop() { }

    @Override
    public void loop() { }

}
