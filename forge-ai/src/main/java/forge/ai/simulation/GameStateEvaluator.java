package forge.ai.simulation;

import forge.ai.CreatureEvaluator;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class GameStateEvaluator {
    private boolean debugging = false;
    private SimulationCreatureEvaluator eval = new SimulationCreatureEvaluator();

    public void setDebugging(boolean debugging) {
        this.debugging = debugging;
    }

    public int getScoreForGameState(Game game, Player aiPlayer) {
        if (game.isGameOver()) {
            return game.getOutcome().getWinningPlayer() == aiPlayer ? Integer.MAX_VALUE : Integer.MIN_VALUE;
        }
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
        GameSimulator.debugPrint("My cards in hand: " + myCards);
        GameSimulator.debugPrint("Their cards in hand: " + theirCards);
        if (myCards > aiPlayer.getMaxHandSize()) {
            // Count excess cards for less.
            score += myCards - aiPlayer.getMaxHandSize();
            myCards = aiPlayer.getMaxHandSize();
        }
        score += 3 * myCards - 3 * theirCards;
        for (Card c : game.getCardsIn(ZoneType.Battlefield)) {
            int value = evalCard(game, aiPlayer, c);
            String str = c.getName();
            if (c.isCreature()) {
                str += " " + c.getNetPower() + "/" + c.getNetToughness();
            }
            if (c.getController() == aiPlayer) {
                GameSimulator.debugPrint("  Battlefield: " + str + " = " + value);
                score += value;
            } else {
                GameSimulator.debugPrint("  Battlefield: " + str + " = -" + value);
                score -= value;
            }
            String nonAbilityText = c.getNonAbilityText();
            if (!nonAbilityText.isEmpty()) {
                GameSimulator.debugPrint("    "+nonAbilityText.replaceAll("CARDNAME", c.getName()));
            }
        }
        GameSimulator.debugPrint("  My life: " + aiPlayer.getLife());
        score += aiPlayer.getLife();
        int opponentIndex = 1;
        int opponentLife = 0;
        for (Player opponent : game.getPlayers()) {
            if (opponent != aiPlayer) {
                GameSimulator.debugPrint("  Opponent " + opponentIndex + " life: -" + opponent.getLife());
                opponentLife += opponent.getLife();
                opponentIndex++;
            }
        }
        score -= opponentLife / (game.getPlayers().size() - 1);
        GameSimulator.debugPrint("Score = " + score);
        return score;
    }

    protected int evalCard(Game game, Player aiPlayer, Card c) {
        // TODO: These should be based on other considerations - e.g. in relation to opponents state.
        if (c.isCreature()) {
            // Ignore temp boosts post combat, since it's a waste.
            // TODO: Make this smarter. Temp boosts pre-combat are also useless if there's no plan to attack
            // with that creature - or if you're just temporarily pumping down.
            // Also, sometimes temp boosts post combat could be useful - e.g. if you then want to make your
            // creature fight another, etc.
            boolean ignoreTempBoosts = game.getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DAMAGE);
            eval.setIgnoreTempBoosts(ignoreTempBoosts);
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
            return 50 + 30 * c.getCMC();
        }
    }

    private class SimulationCreatureEvaluator extends CreatureEvaluator {
        private boolean ignoreTempBoosts;
        
        public void setIgnoreTempBoosts(boolean value) {
            this.ignoreTempBoosts = value;
        }
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
}
