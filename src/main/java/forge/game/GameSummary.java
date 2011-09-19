package forge.game;

import java.util.HashMap;
import java.util.Map;


/**
 * <p>GameInfo class.</p>
 *
 * @author Forge
 * @version $Id$
 */

// This class might be divided in two parts: the very summary (immutable with only getters) and
// GameObserver class - who should be notified of any considerable ingame event
public final class GameSummary {
    protected String playerWinner = "Nobody";
    protected String playerGotFirstTurn = "Nobody";
    protected int lastTurnNumber = 0;
    
    protected GameEndReason winCondition;
    protected String spellEffectWin;
    protected final Map<String, GamePlayerRating> playerRating = new HashMap<String, GamePlayerRating>();

    public GameSummary(String... names)
    {
        for (String n : names) { 
            playerRating.put(n, new GamePlayerRating());
        }
    }
    
    public final void end( final GameEndReason condition, String winner, String spellEffect )
    {
        winCondition = condition;
        playerWinner = winner;
        spellEffectWin = spellEffect;
    }
    public final boolean isDraw() { return null == playerWinner; }
    public final boolean isWinner(String name) { return name != null && name.equals(playerWinner); }
    public String getWinner() { return playerWinner; }

    public GameEndReason getWinCondition() { return winCondition; }
    public GamePlayerRating getPlayerRating(String name) { return playerRating.get(name); }

    public int getTurnGameEnded() {
        return lastTurnNumber;
    }
    
    public final void setPlayerWhoGotFirstTurn(String playerName)
    {
        playerGotFirstTurn = playerName;
    }

    public void notifyNextTurn() {
        lastTurnNumber++;
    }

    public String getWinSpellEffect() {
        return spellEffectWin;
    }




}
