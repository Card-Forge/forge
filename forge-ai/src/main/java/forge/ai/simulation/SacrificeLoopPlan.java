package forge.ai.simulation;

import forge.ai.RunawayLoop;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Loops that go round a sacrifice outlet with creatures that come back.
 * <ul>
 * <li><b>Persist.</b> Melira, Sylvok Outcast stops the -1/-1 counter of persist, so a Murderous Redcap sacrificed to
 * Viscera Seer is back at once with the same power and its enter trigger hits for two again. Melira also gives the persist
 * creature the SacMe mark, which makes the Seer pay with the Redcap.</li>
 * <li><b>Undying.</b> Yawgmoth, Thran Physician puts a -1/-1 counter on a creature as it sacrifices another: two undying
 * creatures keep each other going. One is sacrificed and comes back with a +1/+1 counter; the next activation sacrifices
 * the other and puts its -1/-1 counter on the one that came back, which cancels the +1/+1 and makes it sacrificeable again.
 * Every round is a creature that dies (and one that enters), a card drawn and a life paid. Run only with something that
 * pays for it: a Blood Artist, a Zulaport Cutthroat or a kin that drains one for each death, which gives back the
 * life, or a Geralf's Messenger that takes two from the opponent each time it comes back.</li>
 * </ul>
 * Each step is the next activation; the guards are the activation limit and the opponent's life (the loop stops when
 * the caller has no opponent left alive).
 */
public final class SacrificeLoopPlan {
    private static final int LIMIT = LoopRunner.MAX_ACTIVATIONS;

    private SacrificeLoopPlan() {
    }

    static SpellAbility next(final Player ai, final List<SpellAbility> candidates, final Predicate<SpellAbility> approve) {
        final SpellAbility melira = meliraAndRedcap(ai, candidates, approve);
        return melira != null ? melira : yawgmothAndUndying(ai, candidates, approve);
    }

    private static SpellAbility meliraAndRedcap(final Player ai, final List<SpellAbility> candidates,
                                                final Predicate<SpellAbility> approve) {
        final Card melira = LoopRunner.inPlay(ai, "Melira, Sylvok Outcast");
        final Card redcap = LoopRunner.inPlay(ai, "Murderous Redcap");
        final Card seer = LoopRunner.inPlay(ai, "Viscera Seer");
        if (melira == null || redcap == null || seer == null || !redcap.hasKeyword(Keyword.PERSIST)
                || redcap.getCounters(CounterEnumType.M1M1) > 0) {
            return null;
        }
        final SpellAbility scry = LoopRunner.activated(candidates, seer, ApiType.Scry);
        if (scry == null) {
            return null;
        }
        scry.setActivatingPlayer(ai);
        scry.resetTargets();
        if (RunawayLoop.exceeded(scry, LIMIT) || !approve.test(scry)) {
            return null;
        }
        VetoLog.note(ai, "sacrifice-loop | Seer sacrifices the Redcap");
        return scry;
    }

    /** What the opponent loses, and the AI gets back, for each creature that dies or enters: the payoffs in play. */
    static int drainPerDeath(final Player ai) {
        int n = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            switch (c.getName()) {
                case "Blood Artist":
                case "Zulaport Cutthroat":
                case "Falkenrath Noble":
                case "Cruel Celebrant":
                case "Syr Konrad, the Grim":
                    n++;
                    break;
                default:
                    break;
            }
        }
        return n;
    }

    /** The undying creatures that are the AI's, other than the outlet. */
    private static List<Card> undying(final Player ai, final Card outlet) {
        final List<Card> out = new ArrayList<>();
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c != outlet && c.isCreature() && c.hasKeyword(Keyword.UNDYING)) {
                out.add(c);
            }
        }
        return out;
    }

    private static SpellAbility yawgmothAndUndying(final Player ai, final List<SpellAbility> candidates,
                                                   final Predicate<SpellAbility> approve) {
        final Card yawgmoth = LoopRunner.inPlay(ai, "Yawgmoth, Thran Physician");
        if (yawgmoth == null) {
            return null;
        }
        final List<Card> fodder = undying(ai, yawgmoth);
        Card spare = null;   // no counter: it is the one that is sacrificed next
        Card counted = null; // has the +1/+1 counter: it takes the -1/-1 counter
        for (final Card c : fodder) {
            if (c.getCounters(CounterEnumType.P1P1) > 0) {
                counted = c;
            } else {
                spare = c;
            }
        }
        final int drain = drainPerDeath(ai);
        final boolean messenger = fodder.stream().anyMatch(c -> c.getName().equals("Geralf's Messenger"));
        if (fodder.size() < 2 || spare == null || (drain < 1 && !messenger) || ai.getLife() < 2) {
            return null;
        }
        if (drain < 1) {
            int oppLife = 0;
            for (final Player opp : ai.getOpponents()) {
                oppLife = Math.max(oppLife, opp.getLife());
            }
            if (ai.getLife() <= (oppLife + 1) / 2 + 1) {
                return null; // a life for every two that the opponent loses: the AI would be dead first
            }
        }
        final SpellAbility put = LoopRunner.activated(candidates, yawgmoth, ApiType.PutCounter);
        if (put == null) {
            return null;
        }
        put.setActivatingPlayer(ai);
        put.resetTargets();
        if (RunawayLoop.exceeded(put, LIMIT)) {
            return null;
        }
        if (counted != null) {
            if (!put.canTarget(counted)) {
                return null;
            }
            put.getTargets().add(counted);
        }
        if (!approve.test(put)) {
            return null;
        }
        // the payment of the sacrifice picks the creature marked for it: the one that has no counter, nothing else
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c == spare) {
                c.setSVar("SacMe", "6");
            } else if (c.hasSVar("SacMe") && c.getSVar("SacMe").equals("6")) {
                c.removeSVar("SacMe");
            }
        }
        VetoLog.note(ai, "sacrifice-loop | Yawgmoth sacrifices " + spare.getName());
        return put;
    }
}
