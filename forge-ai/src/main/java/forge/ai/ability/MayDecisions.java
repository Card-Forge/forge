package forge.ai.ability;

import java.util.ArrayList;
import java.util.List;

import forge.ai.ComputerUtil;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * The AI's answers to the "you may ..." questions of effects that had no answer of their own (the base class said yes
 * to everything): choose new targets, remove a counter, give control away, discard the hand for a fresh one, and how
 * many creatures to give up when it is "any number".
 */
public final class MayDecisions {
    private MayDecisions() {
    }

    // ------------------------------------------------------------------------------------------ new targets

    /** New targets for what an opponent cast or activated, never for what the AI chose its targets for itself. */
    public static boolean redirects(final Player ai, final Iterable<SpellAbility> subjects) {
        boolean any = false;
        for (final SpellAbility s : subjects) {
            any = true;
            final Player activator = s.getActivatingPlayer();
            if (activator == null || activator.isOpponentOf(ai)) {
                return true;
            }
        }
        return !any; // nothing known about it: as before
    }

    public static boolean changeTargets(final Player ai, final SpellAbility sa) {
        try {
            final List<SpellAbility> subjects = new ArrayList<>();
            if (sa.usesTargeting() && sa.getTargets() != null) {
                for (final SpellAbility s : sa.getTargets().getTargetSpells()) {
                    subjects.add(s);
                }
            } else if (sa.hasParam("Defined")) {
                subjects.addAll(AbilityUtils.getDefinedSpellAbilities(sa.getHostCard(), sa.getParam("Defined"), sa));
            }
            return redirects(ai, subjects);
        } catch (final RuntimeException e) {
            return true;
        }
    }

    // ------------------------------------------------------------------------------------------ counters

    /** "You may remove a counter" from itself with nothing in return: not a good counter. */
    public static boolean removeCounter(final Player ai, final SpellAbility sa) {
        final String type = sa.getParam("CounterType");
        if (type == null || sa.getSubAbility() != null || !"Self".equals(sa.getParamOrDefault("Defined", "")) || type.equals("Any")
                || type.equals("All") || type.startsWith("Choose") || type.contains(",")) {
            return true;
        }
        try {
            return ComputerUtil.isNegativeCounter(CounterType.getType(type), sa.getHostCard());
        } catch (final RuntimeException e) {
            return true;
        }
    }

    // ------------------------------------------------------------------------------------------ control

    /** Giving a permanent to an opponent is not something to say yes to without logic for it. */
    public static boolean gainControl(final Player ai, final SpellAbility sa) {
        if (sa.hasParam("AILogic") || !sa.hasParam("NewController")) {
            return true;
        }
        try {
            for (final Player p : AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("NewController"), sa)) {
                if (p.isOpponentOf(ai)) {
                    return false;
                }
            }
        } catch (final RuntimeException e) {
            return true;
        }
        return true;
    }

    // ------------------------------------------------------------------------------------------ wheels

    /**
     * "Each player may discard their hand and draw N": when the hand is worth less than what N new cards are expected
     * to be (a hand with fewer cards it can use soon than the share of a new hand it can use).
     */
    public static boolean discardHandForNew(final Player ai, final SpellAbility sa) {
        try {
            int draw = 0;
            for (SpellAbility sub = sa.getSubAbility(); sub != null; sub = sub.getSubAbility()) {
                if (sub.getApi() == ApiType.Draw) {
                    draw = Math.max(1, AbilityUtils.calculateAmount(sa.getHostCard(), sub.getParamOrDefault("NumCards", "1"), sub));
                }
            }
            final CardCollectionView hand = ai.getCardsIn(ZoneType.Hand);
            if (hand.isEmpty()) {
                return draw > 0;
            }
            if (draw == 0) {
                return false; // nothing comes back
            }
            final int lands = CardLists.count(ai.getCardsIn(ZoneType.Battlefield), CardPredicates.LANDS);
            int keepers = 0;
            int landsKept = 0;
            for (final Card c : hand) {
                if (c.isLand()) {
                    if (lands + landsKept < 5) {
                        landsKept++;
                        keepers++;
                    }
                } else if (c.getCMC() <= lands + 2) {
                    keepers++;
                }
            }
            return keepers < draw * 0.45;
        } catch (final RuntimeException e) {
            return true;
        }
    }

    // ------------------------------------------------------------------------------------------ any number

    /**
     * "Sacrifice any number of creatures, then ...": the AI gave up the maximum. Take the ones the chooser would give
     * up first (worst first) for as long as one more is worth no more than what one more brings (the reward of the
     * steps after the sacrifice is per creature sacrificed: cards drawn, damage dealt, life gained "for each").
     */
    public static CardCollection limitSacrifices(final Player ai, final SpellAbility sa, final CardCollectionView worstFirst) {
        final CardCollection kept = new CardCollection();
        try {
            final int perCreature = SacrificeWorth.reward(sa);
            for (final Card c : worstFirst) {
                if (SacrificeWorth.cost(ai, c) > perCreature) {
                    break;
                }
                kept.add(c);
            }
            return kept;
        } catch (final RuntimeException e) {
            return new CardCollection(worstFirst);
        }
    }
}
