package forge.ai.simulation;

import forge.ai.AIDeckStatistics;
import forge.ai.CreatureEvaluator;
import forge.card.mana.ManaAtom;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.cost.CostSacrifice;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.zone.ZoneType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static java.lang.Math.max;
import static java.lang.Math.min;

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
    private CombatSimResult simulateUpcomingCombatThisTurn(final Game evalGame, final Player aiPlayer) {
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
        Game gameCopy = copier.makeCopy(null, aiPlayer);
        gameCopy.getPhaseHandler().devAdvanceToPhase(PhaseType.COMBAT_DAMAGE, new Runnable() {
            @Override
            public void run() {
                GameSimulator.resolveStack(gameCopy, aiPlayer.getWeakestOpponent());
            }
        });
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

        CombatSimResult result = simulateUpcomingCombatThisTurn(game, aiPlayer);
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
        // TODO: try and reuse evaluateBoardPosition
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
        if (!aiPlayer.isUnlimitedHandSize() && myCards > aiPlayer.getMaxHandSize()) {
            // Count excess cards for less.
            score += myCards - aiPlayer.getMaxHandSize();
            myCards = aiPlayer.getMaxHandSize();
        }
        // TODO weight cards in hand more if opponent has discard or if we have looting or can bluff a trick
        score += 5 * myCards - 4 * theirCards;
        debugPrint("  My life: " + aiPlayer.getLife());
        score += 2 * aiPlayer.getLife();
        int opponentIndex = 1;
        int opponentLife = 0;
        for (Player opponent : aiPlayer.getOpponents()) {
            debugPrint("  Opponent " + opponentIndex + " life: -" + opponent.getLife());
            opponentLife += opponent.getLife();
            opponentIndex++;
        }
        score -= 2* opponentLife / (game.getPlayers().size() - 1);

        // evaluate mana base quality
        score += evalManaBase(game, aiPlayer, AIDeckStatistics.fromPlayer(aiPlayer));
        // TODO deal with opponents. Do we want to use perfect information to evaluate their manabase?
        //int opponentManaScore = 0;
        //for (Player opponent : aiPlayer.getOpponents()) {
        //    opponentManaScore += evalManaBase(game, opponent);
        //}
        //score -= opponentManaScore / (game.getPlayers().size() - 1);

        // TODO evaluate holding mana open for counterspells

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

    public int evalManaBase(Game game, Player player, AIDeckStatistics statistics) {
        // TODO should these be fixed quantities or should they be linear out of like 1000/(desired - total)?
        int value = 0;
        // get the colors of mana we can produce and the maximum number of pips
        int max_colored = 0;
        int max_total = 0;
        // this logic taken from ManaCost.getColorShardCounts()
        int[] counts = new int[6]; // in WUBRGC order

        for (Card c : player.getCardsIn(ZoneType.Battlefield)) {
            int max_produced = 0;
            for (SpellAbility m: c.getManaAbilities()) {
                m.setActivatingPlayer(c.getController());
                int mana_cost = m.getPayCosts().getTotalMana().getCMC();
                max_produced = max(max_produced, m.amountOfManaGenerated(true) - mana_cost);
                for (AbilityManaPart mp : m.getAllManaParts()) {
                    for (String part : mp.mana(m).split(" ")) {
                        // TODO handle any
                        int index = ManaAtom.getIndexFromName(part);
                        if (index != -1) {
                            counts[index] += 1;
                        }
                    }
                }
            }
            max_total += max_produced;
        }

        // Compare against the maximums in the deck and in the hand
        // TODO check number of castable cards in hand
        for (int i = 0; i < counts.length; i++) {
            // for each color pip, add 100
            value += Math.min(counts[i], statistics.maxPips[i]) * 100;
        }
        // value for being able to cast all the cards in your deck
        value += min(max_total, statistics.maxCost) * 100;

        // excess mana is valued less than getting enough to use everything
        value += max(0, max_total - statistics.maxCost) * 5;

        return value;
    }

    public int evalCard(Game game, Player aiPlayer, Card c) {
        // TODO: These should be based on other considerations - e.g. in relation to opponents state.
        if (c.isCreature()) {
            return eval.evaluateCreature(c);
        } else if (c.isLand()) {
            return evaluateLand(c);
        } else if (c.isEnchantingCard()) {
            // TODO: Should provide value in whatever it's enchanting?
            // Else the computer would think that casting a Lifelink enchantment
            // on something that already has lifelink is a net win.
            return 0;
        } else {
            // TODO treat cards like Captive Audience negative
            // e.g. a 5 CMC permanent results in 200, whereas a 5/5 creature is ~225
            int value = 50 + 30 * c.getCMC();
            if (c.isPlaneswalker()) {
                value += 2 * c.getCounters(CounterEnumType.LOYALTY);
            }
            return value;
        }
    }

    public static int evaluateLand(Card c) {
        int value = 3;
        // for each mana color a land generates for free, increase the value by one
        // for each mana a land can produce, add one hundred.
        int max_produced = 0;
        Set<String> colors_produced = new HashSet<>();
        for (SpellAbility m: c.getManaAbilities()) {
            m.setActivatingPlayer(c.getController());
            int mana_cost = m.getPayCosts().getTotalMana().getCMC();
            max_produced = max(max_produced, m.amountOfManaGenerated(true) - mana_cost);
            for (AbilityManaPart mp : m.getAllManaParts()) {
                colors_produced.addAll(Arrays.asList(mp.mana(m).split(" ")));
            }
        }
        value += 100 * max_produced;
        int size = max(colors_produced.size(), colors_produced.contains("Any") ? 5 : 0);
        value += size * 3;

        // add a value for each activated ability that the land has that's not an activated ability.
        // The value should be more than the value of having a card in hand, so if a land has an
        // activated ability but not a mana ability, it will still be played.
        for (SpellAbility m: c.getNonManaAbilities()) {
            if (!m.getPayCosts().hasTapCost()) {
                // probably a manland, rate it higher than a rainbow land
                value += 25;
            } else if (m.getPayCosts().hasSpecificCostType(CostSacrifice.class)) {
                // Sacrifice ability, so not repeatable. Less good than a utility land that gets you ahead
                value += 10;
            } else {
                // Repeatable utility land, probably gets you ahead on board over time.
                // big value, probably more than a manland
                value += 50;
            }
        }

        // Add a value for each static ability that the land has
        for (StaticAbility s : c.getStaticAbilities()) {
            // More than the value of having a card in hand. See comment above
            value += 6;
        }

        return value;
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
