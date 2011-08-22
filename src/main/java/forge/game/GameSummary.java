package forge.game;


/**
 * <p>GameInfo class.</p>
 *
 * @author Forge
 * @version $Id$
 */

// This should be divided into two: the very summary (immutable with only getters) and
// GameObserver class - who should be notified of any considerable ingame event
public class GameSummary {
    protected int playerWinner = PlayerIndex.UNDEFINED;
    protected int playerGotFirstTurn = PlayerIndex.UNDEFINED;
    protected int lastTurnNumber = 0;
    
    protected GameEndReason winCondition;
    protected String spellEffectWin;
    protected GamePlayerRating playerRating[] = new GamePlayerRating[2/*players*/];

    public GameSummary()
    {
        playerRating[PlayerIndex.AI] = new GamePlayerRating();
        playerRating[PlayerIndex.HUMAN] = new GamePlayerRating();
    }
    
    public final void end( final GameEndReason condition, int iPlayerWinner, String spellEffectWin )
    {
        winCondition = condition;
        playerWinner = iPlayerWinner;
        this.spellEffectWin = spellEffectWin;
    }

    public final boolean isHumanWinner() { return PlayerIndex.HUMAN == playerWinner; }
    public final boolean isAIWinner() { return PlayerIndex.AI == playerWinner; }
    public final boolean isDraw() { return PlayerIndex.DRAW == playerWinner; }

    public GameEndReason getWinCondition() { return winCondition; }
    public GamePlayerRating getPlayerRating(int iPlayer) {
        if (iPlayer >= 0 && iPlayer < playerRating.length) {
            return playerRating[iPlayer];
        }
        return null;
    }

    public int getTurnGameEnded() {
        return lastTurnNumber;
    }
    
    public final void setPlayerWhoGotFirstTurn(int iPlayer)
    {
        playerGotFirstTurn = iPlayer;
    }

    public void notifyNextTurn() {
        lastTurnNumber++;
    }

    public String getWinSpellEffect() {
        return spellEffectWin;
    }




}
