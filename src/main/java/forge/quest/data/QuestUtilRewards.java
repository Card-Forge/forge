package forge.quest.data;

import forge.MyRandom;
import forge.game.GameLossReason;
import forge.game.GamePlayerRating;
import forge.game.GameSummary;
import forge.game.PlayerIndex;

/** 
 * Helper class to deal with rewards given in quest.
 */
public class QuestUtilRewards {
    private QuestData q;
    public QuestUtilRewards(final QuestData qd) { q = qd; }

    public int getCreditsRewardForAltWin(final GameLossReason whyAiLost) {
        int rewardAltWinCondition = 0;
        switch (whyAiLost) {
            case LifeReachedZero: return 0; // nothing special here, ordinary kill
            case Milled: return QuestPreferences.getMatchRewardMilledWinBonus();
            case Poisoned: return QuestPreferences.getMatchRewardPoisonWinBonus();
            case DidNotLoseYet: return QuestPreferences.getMatchRewardAltWinBonus(); // felidar, helix pinnacle and like this
            case SpellEffect: return QuestPreferences.getMatchRewardAltWinBonus(); // Door to Nothingness or something like this
            default: // this .checkstyle forces us to write some idiotic code
                rewardAltWinCondition = 0;
        }
        return rewardAltWinCondition;
    }

    public int getCreditsRewardForWinByTurn(final int iTurn) {
        if (iTurn == 1) { return QuestPreferences.getMatchRewardWinFirst();
        } else if (iTurn <= 5) { return QuestPreferences.getMatchRewardWinByFifth();
        } else if (iTurn <= 10) { return QuestPreferences.getMatchRewardWinByTen();
        } else if (iTurn <= 15) { return QuestPreferences.getMatchRewardWinByFifteen();
        }
        return 0;
    }

    /**
     * <p>getCreditsToAdd.</p>
     *
     * @param matchState a {@link forge.quest.data.QuestMatchState} object.
     * @return a long.
     */
    public long getCreditsToAdd(final QuestMatchState matchState) {
        long creds = (long) (QuestPreferences.getMatchRewardBase()
                + (QuestPreferences.getMatchRewardTotalWins() * q.getWin()));
    
        boolean hasNeverLost = true;
        for (GameSummary game : matchState.getGamesPlayed()) {
            if (game.isAIWinner()) {
                hasNeverLost = true;
                continue; // no rewards for losing a game
            }
    
            GamePlayerRating aiRating = game.getPlayerRating(PlayerIndex.AI);
            GamePlayerRating humanRating = game.getPlayerRating(PlayerIndex.HUMAN);
            GameLossReason whyAiLost = aiRating.getLossReason();
    
            creds += getCreditsRewardForAltWin(whyAiLost);
            creds += getCreditsRewardForWinByTurn(game.getTurnGameEnded());
    
            int cntCardsHumanStartedWith = humanRating.getOpeningHandSize();
            if (0 == cntCardsHumanStartedWith) {
                creds += QuestPreferences.getMatchMullToZero();
            }
        }
    
        if (hasNeverLost) {
            creds += QuestPreferences.getMatchRewardNoLosses();
        }
    
        switch(q.inventory.getItemLevel("Estates")) {
            case 1: creds *= 1.1; break;
            case 2: creds *= 1.15; break;
            case 3: creds *= 1.2; break;
            default: break;
        }
    
        return creds;
    }

    //add cards after a certain number of wins or losses
    public boolean willGiveBooster(final boolean didWin) {
        int cntOutcomes = didWin ? q.getWin() : q.getLost();
        return cntOutcomes % QuestPreferences.getWinsForBooster(q.getDifficultyIndex()) == 0;
    }

    // this is a chance check, but used for random rare only by now
    public boolean getLuckyCoinResult() {
        boolean hasCoin = q.inventory.getItemLevel("Lucky Coin") >= 1;

        return MyRandom.random.nextFloat() <= (hasCoin ? 0.65f : 0.5f);
    }

}
