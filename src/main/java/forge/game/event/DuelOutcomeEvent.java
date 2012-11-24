package forge.game.event;

public class DuelOutcomeEvent extends Event {
    public final boolean humanWonTheDuel;

    public DuelOutcomeEvent(boolean humanWon) {
        humanWonTheDuel = humanWon;
    }
}
