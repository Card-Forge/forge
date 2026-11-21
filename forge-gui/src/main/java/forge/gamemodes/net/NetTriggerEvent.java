package forge.gamemodes.net;

import forge.util.ITriggerEvent;

import java.io.Serializable;

/** A click as the wire carries it, because the peer may not have the sending platform's own event class. */
public record NetTriggerEvent(int button, int x, int y) implements ITriggerEvent, Serializable {
    public static NetTriggerEvent of(final ITriggerEvent event) {
        return event == null ? null : new NetTriggerEvent(event.getButton(), event.getX(), event.getY());
    }

    @Override
    public int getButton() {
        return button;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }
}
