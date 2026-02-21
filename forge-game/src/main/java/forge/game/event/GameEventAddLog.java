package forge.game.event;

import forge.game.GameLogEntryType;

public record GameEventAddLog(GameLogEntryType type, String message) implements GameEvent {

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
