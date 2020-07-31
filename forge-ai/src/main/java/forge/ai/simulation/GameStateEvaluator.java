package forge.ai.simulation;

import forge.ai.CreatureEvaluator;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class GameStateEvaluator {
    private boolean debugging = false;
    private SimulationCreatureEvaluator eval = new SimulationCreatureEvaluator();

    public void setDebugging(boolean debugging) {
        this.debugging = debugging;
    }
    
    private static void debugPrint(String s) {
        GameSimulator.debugPrint(s);
    }
    
    private static class CombatSimResult {
        public GameCopier copier;
        public Game gameCopy;
    }
    private CombatSimResult simulateUpcomingCombatThisTurn(final Game evalGame) {
        PhaseType phase = evalGame.getPhaseHandler().getPhase();
        if (phase.isAfter(PhaseType.COMBAT_DAMAGE) || evalGame.isGameOver()) {
            return null;
        }
        // If the current player has no creatures in play, there won't be any combat. This avoids
        // an expensive game copy operation.
        // Note: This is is safe to do because the simulation is based on the current game state,
        // so there isn't a chance to play creatures in between.
        if (evalGame.getPhaseHandler().getPlayerTurn().getCreaturesInPlay().isEmpty()) {
            return null;
        }
        GameCopier copier = new GameCopier(evalGame);
        Game gameCopy = copier.makeCopy();
        gameCopy.getPhaseHandler().devAdvanceToPhase(PhaseType.COMBAT_DAMAGE);
        CombatSimResult result = new CombatSimResult();
        result.copier = copier;
        result.gameCopy = gameCopy;
        return result;
    }

    private static String cardToString(Card c) {
        String str = c.getName();
        if (c.isCreature()) {
            str += " " + c.getNetPower() + "/" + c.getNetToughness();
        }
        return str;
    }

    private Score getScoreForGameOver(Game game, Player aiPlayer) {
        if (game.getOutcome().getWinningTeam() == aiPlayer.getTeam() ||
                game.getOutcome().isWinner(aiPlayer.getRegisteredPlayer())) {
            return new Score(Integer.MAX_VALUE);
        }

        return new Score(Integer.MIN_VALUE);
    }
    
    public Score getScoreForGameState(Game game, Player aiPlayer) {
        if (game.isGameOver()) {
            return getScoreForGameOver(game, aiPlayer);
        }
        
        CombatSimResult result = simulateUpcomingCombatThisTurn(game);
        if (result != null) {
            Player aiPlayerCopy = (Player) result.copier.find(aiPlayer);
            if (result.gameCopy.isGameOver()) {
                return getScoreForGameOver(result.gameCopy, aiPlayerCopy);
            }
            return getScoreForGameStateImpl(result.gameCopy, aiPlayerCopy);
        }
        return getScoreForGameStateImpl(game, aiPlayer);
    }

    private Score getScoreForGameStateImpl(Game game, Player aiPlayer) {
        int score = 0;
        // TODO: more than 2 players
        int myCards = 0;
        int theirCards = 0;
        for (Card c : game.getCardsIn(ZoneType.Hand)) {
            if (c.getController() == aiPlayer) {
                myCards++;
            } else {
                theirCards++;
            }
        }
        debugPrint("My cards in hand: " + myCards);
        debugPrint("Their cards in hand: " + theirCards);
        if (myCards > aiPlayer.getMaxHandSize()) {
            // Count excess cards for less.
            score += myCards - aiPlayer.getMaxHandSize();
            myCards = aiPlayer.getMaxHandSize();
        }
        score += 5 * myCards - 4 * theirCards;
        debugPrint("  My life: " + aiPlayer.getLife());
        score += 2 * aiPlayer.getLife();
        int opponentIndex = 1;
        int opponentLife = 0;
        for (Player opponent : game.getPlayers()) {
            if (opponent != aiPlayer) {
                debugPrint("  Opponent " + opponentIndex + " life: -" + opponent.getLife());
                opponentLife += opponent.getLife();
                opponentIndex++;
            }
        }
        score -= 2* opponentLife / (game.getPlayers().size() - 1);
        int summonSickScore = score;
        PhaseType gamePhase = game.getPhaseHandler().getPhase();
        for (Card c : game.getCardsIn(ZoneType.Battlefield)) {
            int value = evalCard(game, aiPlayer, c);
            int summonSickValue = value;
            // To make the AI hold-off on playing creatures before MAIN2 if they give no other benefits,
            // keep track of the score while treating summon sick creatures as having a value of 0.
            if (gamePhase.isBefore(PhaseType.MAIN2) && c.isSick() && c.getController() == aiPlayer) {
                summonSickValue = 0;
            }
            String str = cardToString(c);
            if (c.getController() == aiPlayer) {
                debugPrint("  Battlefield: " + str + " = " + value);
                score += value;
                summonSickScore += summonSickValue;
            } else {
                debugPrint("  Battlefield: " + str + " = -" + value);
                score -= value;
                summonSickScore -= summonSickValue;
            }
            String nonAbilityText = c.getNonAbilityText();
            if (!nonAbilityText.isEmpty()) {
                debugPrint("    "+nonAbilityText.replaceAll("CARDNAME", c.getName()));
            }
        }
        debugPrint("Score = " + score);
        return new Score(score, summonSickScore);
    }
    
    public int evalCard(Game game, Player aiPlayer, Card c) {
        // TODO: These should be based on other considerations - e.g. in relation to opponents state.
        if (c.isCreature()) {
            return eval.evaluateCreature(c);
        } else if (c.isLand()) {
            return 100;
        } else if (c.isEnchantingCard()) {
            // TODO: Should provide value in whatever it's enchanting?
            // Else the computer would think that casting a Lifelink enchantment
            // on something that already has lifelink is a net win.
            return 0;
        } else {
            // e.g. a 5 CMC permanent results in 200, whereas a 5/5 creature is ~225
            int value = 50 + 30 * c.getCMC();
            if (c.isPlaneswalker()) {
                value += 2 * c.getCounters(CounterEnumType.LOYALTY);
            }
            return value;
        }
    }

    private class SimulationCreatureEvaluator extends CreatureEvaluator {
        @Override
        protected int addValue(int value, String text) {
            if (debugging && value != 0) {
                GameSimulator.debugPrint(value + " via " + text);
            }
            return super.addValue(value, text);
        }
    }

    public static class Score {
        public final int value;
        public final int summonSickValue;
        
        public Score(int value) {
            this.value = value;
            this.summonSickValue = value;
        }
        
        public Score(int value, int summonSickValue) {
            this.value = value;
            this.summonSickValue = summonSickValue;
        }
        
        public boolean equals(Score other) {
            if (other == null)
                return false;
            return value == other.value && summonSickValue == other.summonSickValue;
        }

        public String toString() {
            return value + (summonSickValue != value ? " (ss " + summonSickValue + ")" :"");
        }
    }
}
