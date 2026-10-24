package forge.ai.simulation;

import java.util.ArrayList;
import java.util.List;

import forge.ai.ComputerUtilCard;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CounterEnumType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.ability.ApiType;
import forge.game.zone.ZoneType;

/**
 * Strategic terms added to the board evaluation at the end of a rollout.
 * <p>
 * {@link GameStateEvaluator} counts every card in hand as 5 points and every permanent by what it is worth on the
 * table (a creature or a 5 mana permanent is worth 150-250). Played out literally that prefers putting any body on the
 * table over everything else: drawing a card is worth 5, a cantrip nothing, bouncing a creature is as good as killing
 * it. These terms correct that, deck independently, with four small ideas:
 * <ul>
 *   <li><b>Cards in hand</b> are worth part of what they would be worth deployed (a land by how many the AI still
 *       needs, an instant or sorcery less than a permanent, a card it cannot cast for a long time half). So drawing a
 *       card is a gain and casting a cantrip is not a loss, while an opposing card in hand is a debt: making the
 *       opponent replay a bounced creature gains tempo, not a card.</li>
 *   <li><b>Answers</b>: removal or a counterspell the AI can pay for next turn is worth a fraction of the opposing
 *       threat it can answer, so a hand that can answer the opponent's best permanent is better than one that
 *       cannot. The fraction is small enough that using the answer is always better than keeping it.</li>
 *   <li><b>Threat quality</b>: a plain small creature the opponent's board outclasses is worth less than its stats
 *       say, because it neither blocks nor attacks profitably.</li>
 * </ul>
 * Everything is bounded, scaled by {@link FlatMonteCarloPicker.Config#strategicWeightPercent}, and read from the
 * copied game only.
 */
final class StrategicEvaluator {
    /** The evaluator counts 5 points per card in hand for the AI and -4 for an opponent; these terms are on top of it. */
    private static final int BASE_OWN_CARD = 5;
    private static final int BASE_OPPONENT_CARD = 4;
    /** Share of the deployed value that a card in hand is worth. */
    static final int HAND_PERCENT = 60;
    /** What a card in the hand of an opponent is worth to them (unknown card: land or spell). */
    static final int UNKNOWN_CARD_VALUE = 50;
    private static final int OPPONENT_CARDS_COUNTED = 7;
    /** Opposing permanents below this value are not threats worth keeping an answer for. */
    static final int THREAT_THRESHOLD = 130;
    private static final int ANSWER_SHARE_PERCENT = 25;
    private static final int ANSWER_CAP = 60;
    private static final int WEAK_BODY_VALUE = 150;

    private StrategicEvaluator() {
    }

    static int adjust(final Player simAi, final FlatMonteCarloPicker.Config cfg) {
        if (cfg.strategicWeightPercent <= 0) {
            return 0;
        }
        int adj = 0;
        try {
            adj += handTerm(simAi);
            adj += answerTerm(simAi);
            adj += threatQualityTerm(simAi);
        } catch (final RuntimeException e) {
            return 0; // a refinement only: never let it break a rollout
        }
        return (int) Math.round(adj * (cfg.strategicWeightPercent / 100.0));
    }

    /** What the card would be worth if it were deployed, by the same rules of thumb as the board evaluator. */
    static int deployValue(final Card c) {
        if (c.isCreature()) {
            return ComputerUtilCard.evaluateCreature(c);
        }
        if (c.isInstant() || c.isSorcery()) {
            return 40 + 12 * c.getCMC(); // leaves nothing behind
        }
        int value = 50 + 30 * c.getCMC();
        if (c.isPlaneswalker()) {
            value += 2 * c.getCounters(CounterEnumType.LOYALTY);
        }
        return value;
    }

    /** How much one more land is worth to a player with this many in play. */
    static int landNeedValue(final int landsInPlay) {
        if (landsInPlay < 3) {
            return 45;
        }
        if (landsInPlay < 5) {
            return 30;
        }
        if (landsInPlay < 7) {
            return 18;
        }
        return 8;
    }

    private static int landsInPlay(final Player p) {
        int n = 0;
        for (final Card c : p.getCardsIn(ZoneType.Battlefield)) {
            if (c.isLand()) {
                n++;
            }
        }
        return n;
    }

    private static int handTerm(final Player simAi) {
        int adj = 0;
        final int lands = landsInPlay(simAi);
        int landsInHand = 0;
        final CardCollectionView hand = simAi.getCardsIn(ZoneType.Hand);
        for (final Card c : hand) {
            if (c.isLand()) {
                // the second land in hand matters less than the first: only one can be played a turn
                final int need = landNeedValue(lands + landsInHand);
                landsInHand++;
                adj += Math.max(0, need - (landsInHand > 1 ? need / 3 : 0)) - BASE_OWN_CARD;
                continue;
            }
            int value = deployValue(c) * HAND_PERCENT / 100;
            if (c.getCMC() > lands + landsInHandCount(hand) + 1) {
                value /= 2; // far from castable
            }
            adj += value - BASE_OWN_CARD;
        }
        final Player opp = simAi.getWeakestOpponent();
        if (opp != null) {
            final int cards = Math.min(OPPONENT_CARDS_COUNTED, opp.getCardsIn(ZoneType.Hand).size());
            adj -= cards * (UNKNOWN_CARD_VALUE - BASE_OPPONENT_CARD);
        }
        return adj;
    }

    private static int landsInHandCount(final CardCollectionView hand) {
        int n = 0;
        for (final Card c : hand) {
            if (c.isLand()) {
                n++;
            }
        }
        return n;
    }

    /** The most valuable thing the opponent has on the table that an answer could deal with. */
    static int biggestThreat(final Player opp) {
        int best = 0;
        for (final Card c : opp.getCardsIn(ZoneType.Battlefield)) {
            if (c.isCreature()) {
                best = Math.max(best, ComputerUtilCard.evaluateCreature(c));
            } else if (c.isPlaneswalker()) {
                best = Math.max(best, 150 + 2 * c.getCounters(CounterEnumType.LOYALTY));
            }
        }
        return best;
    }

    private static int answerTerm(final Player simAi) {
        final Player opp = simAi.getWeakestOpponent();
        if (opp == null) {
            return 0;
        }
        final int threat = biggestThreat(opp);
        if (threat < THREAT_THRESHOLD) {
            return 0;
        }
        final int mana = landsInPlay(simAi) + 1;
        final int oppCards = opp.getCardsIn(ZoneType.Hand).size();
        final List<Card> answers = new ArrayList<>();
        for (final Card c : simAi.getCardsIn(ZoneType.Hand)) {
            if (c.isLand() || c.getCMC() > mana) {
                continue;
            }
            final FlatMonteCarloPicker.HoldKind kind = FlatMonteCarloPicker.holdKind(c);
            if (kind == FlatMonteCarloPicker.HoldKind.REMOVAL
                    || kind == FlatMonteCarloPicker.HoldKind.COUNTER && oppCards > 0) {
                answers.add(c);
            }
        }
        final int share = Math.min(ANSWER_CAP, threat * ANSWER_SHARE_PERCENT / 100);
        // a second answer is worth half as much again, never more: there is only so much to answer
        return answers.isEmpty() ? 0 : answers.size() == 1 ? share : share * 3 / 2;
    }

    private static int threatQualityTerm(final Player simAi) {
        final Player opp = simAi.getWeakestOpponent();
        int adj = 0;
        for (final Card c : simAi.getCardsIn(ZoneType.Battlefield)) {
            if (!c.isCreature()) {
                continue;
            }
            final int value = ComputerUtilCard.evaluateCreature(c);
            if (value >= WEAK_BODY_VALUE || !isPlainBody(c)) {
                continue;
            }
            boolean outclassed = false;
            if (opp != null) {
                for (final Card o : opp.getCreaturesInPlay()) {
                    if (o.getNetPower() >= c.getNetToughness() && o.getNetToughness() > c.getNetPower()) {
                        outclassed = true;
                        break;
                    }
                }
            }
            adj -= value * (outclassed ? 30 : 10) / 100;
        }
        return adj;
    }

    /** No keywords, triggers, static or activated abilities: its stats are all it is. */
    private static boolean isPlainBody(final Card c) {
        return c.getKeywords().isEmpty() && c.getTriggers().isEmpty() && c.getStaticAbilities().isEmpty()
                && c.getNonManaAbilities().stream().noneMatch(a -> a.isActivatedAbility());
    }

    /** Small value of seeing more of the library: scry, surveil and dig improve the next draws. */
    static int selectionBonus(final SpellAbility sa, final FlatMonteCarloPicker.Config cfg) {
        if (sa == null || cfg.strategicWeightPercent <= 0) {
            return 0;
        }
        int bonus = 0;
        for (SpellAbility a = sa; a != null; a = a.getSubAbility()) {
            final ApiType api = a.getApi();
            if (api == ApiType.Scry || api == ApiType.Surveil) {
                bonus += 8;
            } else if (api == ApiType.Dig) {
                bonus += 10;
            }
        }
        return bonus * cfg.strategicWeightPercent / 100;
    }
}
