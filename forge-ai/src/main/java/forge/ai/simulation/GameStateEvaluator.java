package forge.ai.simulation;

import forge.ai.AiDeckStatistics;
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

        gameCopy.getPhaseHandler().devAdvanceToPhase(PhaseType.COMBAT_DAMAGE, () -> GameSimulator.resolveStack(gameCopy, aiPlayer.getWeakestOpponent()));
        CombatSimResult result = new CombatSimResult();
        result.copier = copier;
        return result;
    }

    private static String cardToString(Card c) {
        String str = c.getName();
        if (c.isCreature()) {
            str += " " + c.getNetPower() + "/" + c.getNetToughness();
        }
        return str;
    }

    private static boolean phasesInNormally(Card card) {
        if (card.isWontPhaseInNormal()) {
            return false;
        }
        Card attachedTo = card.getAttachedTo();
        return card.isDirectlyPhasedOut() || attachedTo != null && phasesInNormally(attachedTo);
    }

    // Winning and losing are still the extremes of the range, but a win twenty turns from now is
    // worth less than a win this turn, and a loss we can put off is better than one we cannot.
    // GameSimulator returns Integer.MIN_VALUE to mean "this line could not be simulated", so the
    // very bottom of the range is left alone and a real loss is scored just above it.
    private static final int GAME_OVER_TURN_PENALTY = 1000;
    private static final int MAX_DISCOUNTED_TURNS = 1000000;

    /** True for any score that means the AI has won, however far off that win is. */
    public static boolean isWinning(int score) {
        return score > Integer.MAX_VALUE - (long) MAX_DISCOUNTED_TURNS * GAME_OVER_TURN_PENALTY;
    }

    private Score getTerminalScore(Game game, Player ai) {
        // a game long enough to overflow the discount is not one we can reason about anyway
        int turn = Math.min(game.getPhaseHandler().getTurn(), MAX_DISCOUNTED_TURNS);
        if (ai.hasWon()) {
            // prefer winning sooner
            // TODO should consider recursion depth for the less complex combos more likely to succeed
            return new Score(Integer.MAX_VALUE - turn * GAME_OVER_TURN_PENALTY);
        }

        // prefer losing later, staying clear of MIN_VALUE so a loss is never mistaken for a failed simulation
        return new Score(Integer.MIN_VALUE + 1 + turn * GAME_OVER_TURN_PENALTY);
    }

    public Score getScoreForGameState(Game game, Player aiPlayer) {
        if (!aiPlayer.isInGame()) {
            return getTerminalScore(game, aiPlayer);
        }

        CombatSimResult result = simulateUpcomingCombatThisTurn(game, aiPlayer);
        if (result != null) {
            Player aiPlayerCopy = (Player) result.copier.find(aiPlayer);
            if (!aiPlayerCopy.isInGame()) {
                return getTerminalScore(result.copier.getCopiedGame(), aiPlayerCopy);
            }
            return getScoreForGameStateImpl(result.copier.getCopiedGame(), aiPlayerCopy);
        }
        return getScoreForGameStateImpl(game, aiPlayer);
    }

    private Score getScoreForGameStateImpl(Game game, Player aiPlayer) {
        // TODO: try and reuse evaluateBoardPosition
        int score = 0;
        int aiCards = 0;
        int teammateCards = 0;
        int opponentCards = 0;
        int teamLife = 0;
        int teamPlayers = 0;
        int teammates = 0;
        int opponentLife = 0;
        int opponents = 0;
        for (Player player : game.getPlayers()) {
            int cards = player.getCardsIn(ZoneType.Hand).size();
            if (player.isOpponentOf(aiPlayer)) {
                score -= 4 * cards;
                opponentCards += cards;
                opponentLife += player.getLife();
                debugPrint("  Opponent " + (++opponents) + " life: -" + player.getLife());
            } else {
                int fullValueCards = player.isUnlimitedHandSize()
                        ? cards : min(cards, player.getMaxHandSize());
                score += cards + 4 * fullValueCards;
                teamLife += player.getLife();
                teamPlayers++;
                if (player == aiPlayer) {
                    aiCards = cards;
                    debugPrint("  My life: " + player.getLife());
                } else {
                    teammateCards += cards;
                    debugPrint("  Ally " + (++teammates) + " life: " + player.getLife());
                }
            }
        }
        debugPrint("My cards in hand: " + aiCards);
        if (teammates > 0) {
            debugPrint("Allied cards in hand: " + teammateCards);
        }
        debugPrint("Their cards in hand: " + opponentCards);
        // TODO weight cards in hand more if opponent has discard or if we have looting or can bluff a trick
        score += 2 * teamLife / teamPlayers;
        if (opponents > 0) {
            score -= 2 * opponentLife / opponents;
        }

        // evaluate mana base quality
        AiDeckStatistics statistics = AiDeckStatistics.fromPlayer(aiPlayer);
        int availableManaScore = evalManaBase(aiPlayer, statistics, false);
        int strategicManaScore = aiPlayer.getCardsIn(ZoneType.Battlefield, false).stream()
                .anyMatch(c -> c.isPhasedOut() && phasesInNormally(c) && !c.getManaAbilities().isEmpty())
                ? evalManaBase(aiPlayer, statistics, true) : availableManaScore;
        int availableScore = score + availableManaScore;
        score += strategicManaScore;
        // TODO deal with opponents. Do we want to use perfect information to evaluate their manabase?
        //int opponentManaScore = 0;
        //for (Player opponent : aiPlayer.getOpponents()) {
        //    opponentManaScore += evalManaBase(game, opponent);
        //}
        //score -= opponentManaScore / (game.getPlayers().size() - 1);

        // TODO evaluate holding mana open for counterspells

        PhaseType gamePhase = game.getPhaseHandler().getPhase();
        for (Card c : game.getCardsIncludePhasingIn(ZoneType.Battlefield)) {
            boolean phasedOut = c.isPhasedOut();
            if (phasedOut && !phasesInNormally(c)) {
                continue;
            }
            int value = evalCard(game, aiPlayer, c);
            int availableValue = value;
            // To make the AI hold-off on playing creatures before MAIN2 if they give no other benefits,
            // keep track of the score while treating summon sick creatures as having a value of 0.
            if (phasedOut || (gamePhase.isBefore(PhaseType.MAIN2) && c.isSick() && c.getController() == aiPlayer)) {
                availableValue = 0;
            }
            String str = cardToString(c);
            int multiplier = c.getController().isOpponentOf(aiPlayer) ? -1 : 1;
            debugPrint("  Battlefield: " + str + " = " + (multiplier < 0 ? "-" : "") + value);
            score += multiplier * value;
            availableScore += multiplier * availableValue;
            String nonAbilityText = c.getNonAbilityText();
            if (!nonAbilityText.isEmpty()) {
                debugPrint("    "+nonAbilityText.replaceAll("CARDNAME", c.getName()));
            }
        }

        debugPrint("Score = " + score);
        return new Score(score, availableScore);
    }

    private int evalManaBase(Player player, AiDeckStatistics statistics, boolean includeNormalPhasing) {
        // TODO should these be fixed quantities or should they be linear out of like 1000/(desired - total)?
        int value = 0;
        // get the colors of mana we can produce and the maximum number of pips
        int max_total = 0;
        // this logic taken from ManaCost.getColorShardCounts()
        int[] counts = new int[6]; // in WUBRGC order

        // Strategic evaluation includes lands guaranteed to phase in normally; availability does not.
        for (Card c : player.getCardsIn(ZoneType.Battlefield, false)) {
            if (c.isPhasedOut() && (!includeNormalPhasing || !phasesInNormally(c))) {
                continue;
            }
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
            if (m.isLandAbility()) {
                // Land Ability has no extra Score
                continue;
            } if (!m.getPayCosts().hasTapCost()) {
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
        /** Long-term value, including permanents guaranteed to phase in normally. */
        public final int value;
        /** Value currently available for mana, abilities, and the next combat. */
        public final int availableValue;
        
        public Score(int value) {
            this.value = value;
            this.availableValue = value;
        }

        public Score(int value, int availableValue) {
            this.value = value;
            this.availableValue = availableValue;
        }

        public boolean equals(Score other) {
            if (other == null)
                return false;
            return value == other.value && availableValue == other.availableValue;
        }

        public String toString() {
            return value + (availableValue != value ? " (available " + availableValue + ")" : "");
        }
    }
}
