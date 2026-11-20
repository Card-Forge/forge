package forge.ai.ability;

import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * "You may sacrifice a creature. If you do, ..." (Crovax, Flues, Altar's Reap and about fifty more): is what comes
 * back worth what goes? The reward is the sum of what the steps after the sacrifice give (a card, a counter, a token,
 * life, damage, mana, a card put onto the battlefield), plus what a step that only happens when nothing was sacrificed
 * would have cost (Crovax loses a counter), against the value of the permanent the sacrifice would give up: the one
 * the AI's own chooser picks, which is the one that will go. The numbers are in the units of the creature evaluation,
 * where a vanilla 2/2 is a bit over one hundred.
 */
public final class SacrificeWorth {
    static final int CARD = 110;
    static final int COUNTER_P1P1 = 80;
    static final int COUNTER_OTHER = 30;
    static final int TOKEN = 90;
    static final int LIFE = 4;
    static final int DAMAGE = 12;
    static final int MANA = 35;
    static final int TO_BATTLEFIELD = 160;
    static final int TO_HAND = 110;
    static final int PUMP = 40;
    static final int UNKNOWN = 70;

    private SacrificeWorth() {
    }

    /** Whether this is an optional sacrifice of the AI's permanents that has no logic of its own. */
    public static boolean applies(final SpellAbility sa) {
        return sa != null && !sa.hasParam("AILogic") && sa.getActivatingPlayer() != null
                && !"Self".equals(sa.getParamOrDefault("SacValid", "Self"))
                && (sa.getHostCard() == null || !"Scapeshift".equals(sa.getHostCard().getName()));
    }

    /** The answer to "do you want to sacrifice?". */
    public static boolean confirm(final Player ai, final SpellAbility sa) {
        try {
            final CardCollection battlefield = new CardCollection(ai.getCardsIn(ZoneType.Battlefield));
            final CardCollection valid = CardLists.filter(
                    AbilityUtils.filterListByType(battlefield, sa.getParam("SacValid"), sa),
                    CardPredicates.canBeSacrificedBy(sa, true));
            if (valid.isEmpty()) {
                return false;
            }
            final CardCollection chosen = ComputerUtil.choosePermanentsToSacrifice(ai, valid, 1, sa, false, true);
            if (chosen == null || chosen.isEmpty()) {
                return false;
            }
            if (diesWithoutSacrificing(ai, sa)) {
                return true; // the sacrifice is what keeps the AI alive: whatever the chooser gives up
            }
            int cost = 0;
            for (final Card c : chosen) {
                cost += cost(ai, c);
            }
            return reward(sa) >= cost;
        } catch (final RuntimeException e) {
            return true;
        }
    }

    /**
     * A step after the sacrifice makes the AI lose at least all the life it has (Pillar Tombs of Aku, Fandaniel):
     * the same test the chooser makes for a sacrifice that an opponent's card asks for.
     */
    static boolean diesWithoutSacrificing(final Player ai, final SpellAbility sa) {
        if (!ai.canLoseLife() || ai.cantLoseForZeroOrLessLife()) {
            return false;
        }
        for (SpellAbility sub = sa.getSubAbility(); sub != null; sub = sub.getSubAbility()) {
            if (sub.getApi() != ApiType.LoseLife) {
                continue;
            }
            final String defined = sub.getParamOrDefault("Defined", "");
            if ((defined.contains("OppNon") || defined.contains("Opponent") || defined.contains("TriggeredPlayer"))
                    && amount(sub, "LifeAmount") >= ai.getLife()) {
                return true;
            }
        }
        return false;
    }

    /** What the steps after the sacrifice give, and what the step for "you did not" would have taken. */
    public static int reward(final SpellAbility sa) {
        int total = 0;
        for (SpellAbility sub = sa.getSubAbility(); sub != null; sub = sub.getSubAbility()) {
            total += value(sub);
        }
        return total;
    }

    /** What an ability gives altogether: its own effect and the steps after it (for an activated ability's cost). */
    public static int rewardWithRoot(final SpellAbility sa) {
        return value(sa) + reward(sa);
    }

    /** What the permanent is worth to its controller. */
    public static int cost(final Player ai, final Card c) {
        if (c.hasSVar("SacMe") || ComputerUtilCard.hasActiveUndyingOrPersist(c)) {
            return 0;
        }
        if (c.isCreature()) {
            final int eval = ComputerUtilCard.evaluateCreature(c);
            return c.isToken() ? eval * 7 / 10 : eval;
        }
        if (c.isLand()) {
            final int lands = CardLists.count(ai.getCardsIn(ZoneType.Battlefield), CardPredicates.LANDS);
            return lands >= 6 ? 50 : 140;
        }
        return c.isToken() ? 30 : 40 + 30 * c.getCMC();
    }

    private static int value(final SpellAbility node) {
        final ApiType api = node.getApi();
        if (api == null) {
            return 0;
        }
        switch (api) {
            case Cleanup:
            case Bookkeeping:
            case Nothing:
            case TargetOnly:
                return 0;
            case Draw:
                return CARD * amount(node, "NumCards");
            case PutCounter:
            case RemoveCounter:
                return ("P1P1".equals(node.getParam("CounterType")) ? COUNTER_P1P1 : COUNTER_OTHER) * amount(node, "CounterNum");
            case Token:
                return TOKEN * amount(node, "TokenAmount");
            case GainLife:
                return LIFE * amount(node, "LifeAmount");
            case LoseLife:
                return 10 * amount(node, "LifeAmount");
            case DealDamage:
                return DAMAGE * amount(node, "NumDmg");
            case Mana:
                return MANA * amount(node, "Amount");
            case ChangeZone:
            case ChangeZoneAll:
            case Dig:
                return "Battlefield".equals(node.getParam("Destination")) || "Battlefield".equals(node.getParam("DestinationZone"))
                        ? TO_BATTLEFIELD : TO_HAND;
            case Pump:
            case PumpAll:
                return PUMP;
            default:
                return UNKNOWN;
        }
    }

    /** A number of a step, one when it depends on something that is not known yet (the number sacrificed, an X). */
    private static int amount(final SpellAbility node, final String param) {
        try {
            return Math.max(1, AbilityUtils.calculateAmount(node.getHostCard(), node.getParamOrDefault(param, "1"), node));
        } catch (final RuntimeException e) {
            return 1;
        }
    }
}
