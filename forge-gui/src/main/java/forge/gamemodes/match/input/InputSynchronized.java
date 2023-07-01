package forge.gamemodes.match.input;

public interface InputSynchronized extends Input {
    void awaitLatchRelease();
    void relaseLatchWhenGameIsOver();
}
