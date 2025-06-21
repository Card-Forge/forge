package forge.game.event;

import forge.game.IGame;

public class GameEventSubgameEnd extends GameEvent {
    public final IGame maingame;
    public final String message;

    public GameEventSubgameEnd(IGame game, String message0) {
        maingame = game;
        message = message0;
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
