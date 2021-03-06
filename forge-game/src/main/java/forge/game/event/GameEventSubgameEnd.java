package forge.game.event;

import forge.game.Game;

public class GameEventSubgameEnd extends GameEvent {
    public final Game maingame;
    public final String message;

    public GameEventSubgameEnd(Game game, String message0) {
        maingame = game;
        message = message0;
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
