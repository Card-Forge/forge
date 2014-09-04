package forge.match.input;

import forge.interfaces.IGuiBase;

public interface InputSynchronized extends Input {
    void awaitLatchRelease();
    void relaseLatchWhenGameIsOver();
    IGuiBase getGui();
}
