package forge.game.event;

import java.util.List;

import forge.game.GameOutcome;

public class GameEventDuelOutcome extends GameEvent {
    public final GameOutcome result;
    public final List<GameOutcome> history;

    public GameEventDuelOutcome(GameOutcome lastOne, List<GameOutcome> history) {
        this.result = lastOne;
        this.history = history;
    }
}