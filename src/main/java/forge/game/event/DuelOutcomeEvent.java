package forge.game.event;

import java.util.List;

import forge.game.GameOutcome;

public class DuelOutcomeEvent extends Event {
    public final GameOutcome result;
    public final List<GameOutcome> history;

    public DuelOutcomeEvent(GameOutcome lastOne, List<GameOutcome> history) {
        this.result = lastOne;
        this.history = history;
    }
}
