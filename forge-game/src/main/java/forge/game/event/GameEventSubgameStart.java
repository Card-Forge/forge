package forge.game.event;

import forge.game.Game;

public class GameEventSubgameStart extends GameEvent {
    public final Game subgame;
    public final String message;

    public GameEventSubgameStart(Game subgame0, String message0) {
        subgame = subgame0;
        message = message0;
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
