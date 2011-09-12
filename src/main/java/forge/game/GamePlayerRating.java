package forge.game;

/**
 * 
 * @author Max
 *
 */
public class GamePlayerRating {
    boolean hasWonTheCoin;

    protected int turnsMade = 0;
    protected int openingHandSize = 7;
    protected int timesMulliganed = 0;

    protected GameLossReason lossReason = GameLossReason.DidNotLoseYet;
    protected String lossSpellName;

    public final GameLossReason getLossReason() {
        return lossReason;
    }
    
    public void setLossReason(GameLossReason loseCondition, String spellName) {
        lossReason = loseCondition;
        lossSpellName = spellName;
    }
    public String getLossSpellName() { 
        return lossSpellName;
    }

    public final int getOpeningHandSize() {
        return openingHandSize;
    }

    public final void notifyHasMulliganed() {
        timesMulliganed++;
    }

    public final int getMulliganCount() {
        return timesMulliganed;
    }

    public final void notifyOpeningHandSize(final int newHand) {
        openingHandSize = newHand;
    }


}
