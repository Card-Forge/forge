package forge.control.input;

public interface InputSynchronized extends Input {
    
    public void awaitLatchRelease();
}
