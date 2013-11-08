package forge.gui.input;

public interface InputSynchronized extends Input {
    void awaitLatchRelease();
    void relaseLatchWhenGameIsOver();
}
