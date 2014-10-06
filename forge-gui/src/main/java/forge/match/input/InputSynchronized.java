package forge.match.input;

public interface InputSynchronized extends Input {
    void awaitLatchRelease();
    void relaseLatchWhenGameIsOver();
}
