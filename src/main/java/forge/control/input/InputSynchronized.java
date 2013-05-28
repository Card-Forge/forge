package forge.control.input;

public interface InputSynchronized extends Input {
    void awaitLatchRelease();
    void relaseLatchWhenGameIsOver();
}
