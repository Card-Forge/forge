package forge.game.event;

import forge.game.Game;

public record GameEventSubgameEnd(Game maingame, String message) implements GameEvent {
    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
