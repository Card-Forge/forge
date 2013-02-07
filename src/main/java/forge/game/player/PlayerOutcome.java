package forge.game.player;

import forge.game.GameLossReason;

/**
 * TODO: Write javadoc for this type.
 */
public class PlayerOutcome {
    /** The alt win source name. */
    public final String altWinSourceName;
    /** The loss state. */
    public final GameLossReason lossState;
    /** The lose condition spell. */
    public final String loseConditionSpell;

    private PlayerOutcome(String altWinSourceName, GameLossReason lossState, String loseConditionSpell) {
        this.altWinSourceName = altWinSourceName;
        this.loseConditionSpell = loseConditionSpell;
        this.lossState = lossState;
    }

    /**
     * TODO: Write javadoc for this method.
     * @param sourceName
     * @return
     */
    public static PlayerOutcome win() {
        return new PlayerOutcome(null, null, null);
    }

    public static PlayerOutcome altWin(String sourceName) {
        return new PlayerOutcome(sourceName, null, null);
    }

    /**
     * TODO: Write javadoc for this method.
     * @param state
     * @param spellName
     * @return
     */
    public static PlayerOutcome loss(GameLossReason state, String spellName) {
        return new PlayerOutcome(null, state, spellName);
    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public static PlayerOutcome concede() {
        return new PlayerOutcome(null, GameLossReason.Conceded, null);
    }

    public boolean hasWon() {
        return lossState == null;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        if ( lossState == null ) {
            if ( altWinSourceName == null )
                return "won because all opponents have lost";
            else 
                return "won due to effect of '" + altWinSourceName + "'";
        }
        switch(lossState){
            case Conceded: return "conceded";
            case Milled: return "lost trying to draw cards from empty library";
            case LifeReachedZero: return "lost because life total reached 0";
            case Poisoned: return "lost because of obtaining 10 poison counters";
            case OpponentWon: return "lost because an opponent has won by spell '" + loseConditionSpell + "'";
            case SpellEffect: return "lost due to effect of spell '" + loseConditionSpell + "'";
        }
        return "lost for unknown reason (this is a bug)";
    }

}
