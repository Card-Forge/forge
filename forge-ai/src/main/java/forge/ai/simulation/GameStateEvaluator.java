package forge.ai.simulation;

import forge.ai.AiAttackController;
import forge.ai.CreatureEvaluator;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.combat.Combat;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class GameStateEvaluator {
    private boolean debugging = false;
    private boolean ignoreTempBoosts = false;
    private SimulationCreatureEvaluator eval = new SimulationCreatureEvaluator();

    public void setDebugging(boolean debugging) {
        this.debugging = debugging;
    }
    
    private static void debugPrint(String s) {
        //System.err.println(s);
        GameSimulator.debugPrint(s);
    }
    
    private Combat simulateUpcomingCombatThisTurn(Game game) {
        PhaseHandler handler = game.getPhaseHandler();
        if (handler.getPhase().isAfter(PhaseType.COMBAT_DAMAGE)) {
            return null;
        }
        AiAttackController aiAtk = new AiAttackController(handler.getPlayerTurn());
        Combat combat = new Combat(handler.getPlayerTurn());
        aiAtk.declareAttackers(combat);
        return combat;
    }

    public Score getScoreForGameState(Game game, Player aiPlayer) {
        if (game.isGameOver()) {
            return game.getOutcome().getWinningPlayer() == aiPlayer ? new Score(Integer.MAX_VALUE) : new Score(Integer.MIN_VALUE);
        }
        
        Combat combat = simulateUpcomingCombatThisTurn(game);

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
            int value = evalCard(game, aiPlayer, c, combat);
            int summonSickValue = value;
            // To make the AI hold-off on playing creatures in MAIN1 if they give no other benefits,
            // keep track of the score while treating summon sick creatures as having a value of 0.
            if (gamePhase == PhaseType.MAIN1 && c.isSick() && c.getController() == aiPlayer) {
                summonSickValue = 0;
            }
            String str = c.getName();
            if (c.isCreature()) {
                str += " " + c.getNetPower() + "/" + c.getNetToughness();
            }
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

    protected int evalCard(Game game, Player aiPlayer, Card c, Combat combat) {
        // TODO: These should be based on other considerations - e.g. in relation to opponents state.
        if (c.isCreature()) {
            // Ignore temp boosts post combat, since it's a waste.
            // TODO: Make this smarter. Right now, it only looks if the creature will attack - but
            // does not consider things like blocks or the outcome of combat.
            // Also, sometimes temp boosts post combat could be useful - e.g. if you then want to make your
            // creature fight another, etc.
            ignoreTempBoosts = true;
            if (combat != null && combat.isAttacking(c)) {
                ignoreTempBoosts = false;
            }
            int result = eval.evaluateCreature(c);
            return result;
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
                value += 2 * c.getCounters(CounterType.LOYALTY);
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

        @Override
        protected int getEffectivePower(final Card c) {
            if (ignoreTempBoosts) {
                Card.StatBreakdown breakdown = c.getNetToughnessBreakdown();
                return breakdown.getTotal() - breakdown.tempBoost;
            }
            return c.getNetCombatDamage();
        }
        @Override
        protected int getEffectiveToughness(final Card c) {
            if (ignoreTempBoosts) {
                Card.StatBreakdown breakdown = c.getNetToughnessBreakdown();
                return breakdown.getTotal() - breakdown.tempBoost;
            }
            return c.getNetToughness();
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
    }
}
