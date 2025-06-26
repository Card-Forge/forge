package forge.game.event;

import forge.game.IGame;

public class GameEventSubgameStart extends GameEvent {
    public final IGame subgame;
    public final String message;

    public GameEventSubgameStart(IGame subgame0, String message0) {
        subgame = subgame0;
        message = message0;
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
