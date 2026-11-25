package forge.ai.simulation;

import forge.ai.ComputerUtilAbility;
import forge.ai.ComputerUtilMana;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.cost.CostSacrifice;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;

import java.util.ArrayList;
import java.util.List;

/**
 * Landfall timing: a land (or a fetch land cracked) is worth most when a landfall permanent is already in play to
 * see it, and it is wasted when the landfall ability can do nothing.
 * <ul>
 * <li><b>Payoff first.</b> A landfall permanent that is in the hand and can be paid for without the land (Scute Swarm,
 * Lotus Cobra, Omnath, Tireless Tracker, Rampaging Baloths...) is cast before the land drop and before a fetch land is
 * cracked, so the land triggers it. The land is never held back for a spell it is needed to cast: only a payoff that
 * the lands already in play pay for goes first, and the land drop follows in the same turn.</li>
 * <li><b>No wasted fetch.</b> When every landfall permanent in play only pumps itself (Steppe Lynx, Plated Geopede)
 * and none of them can attack this turn (summoning sick, tapped, or the combat is over), cracking a fetch land does
 * nothing for it, so the crack waits for a turn the pump is used. The fetch is still cracked at once when the land
 * is needed to cast a spell in the hand.</li>
 * </ul>
 * A pump on a creature that cannot attack is the only landfall ability that is called useless: tokens, counters,
 * mana, cards and damage need no combat. Reads the real game state; every question falls back to "no objection".
 */
public final class LandfallGuard {
    private LandfallGuard() {
    }

    /** What a landfall ability gives. */
    public enum Kind { NONE, PUMP, VALUE }

    /** The kind of landfall ability a card has: VALUE when any of its landfall triggers does more than pump itself. */
    public static Kind kind(final Card card) {
        if (card == null) {
            return Kind.NONE;
        }
        Kind found = Kind.NONE;
        try {
            for (final Trigger t : card.getTriggers()) {
                if (!isLandfall(t)) {
                    continue;
                }
                final SpellAbility sa = t.ensureAbility();
                final ApiType api = sa == null ? null : sa.getApi();
                final boolean pump = api == ApiType.Pump && "Self".equals(sa.getParam("Defined"))
                        && sa.getSubAbility() == null;
                if (!pump) {
                    return Kind.VALUE;
                }
                found = Kind.PUMP;
            }
        } catch (final RuntimeException e) {
            return Kind.NONE;
        }
        return found;
    }

    private static boolean isLandfall(final Trigger t) {
        if (t == null || !"ChangesZone".equals(t.getParam("Mode")) || !"Battlefield".equals(t.getParam("Destination"))) {
            return false;
        }
        final String valid = t.getParam("ValidCard");
        return valid != null && valid.startsWith("Land") && valid.contains("YouCtrl");
    }

    /** The spells of the landfall permanents in the hand that the lands in play pay for and that the land should follow. */
    public static List<SpellAbility> payoffsFirst(final Player ai) {
        final List<SpellAbility> out = new ArrayList<>();
        if (ai == null || ai.getGame() == null || !ai.getGame().getStack().isEmpty()
                || !ai.getGame().getPhaseHandler().is(PhaseType.MAIN1, ai)) {
            return out;
        }
        try {
            for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
                if (c.isLand() || !c.isPermanent()) {
                    continue;
                }
                final Kind kind = kind(c);
                if (kind == Kind.NONE || (kind == Kind.PUMP && !c.hasKeyword("Haste"))) {
                    continue;
                }
                for (final SpellAbility sa : ComputerUtilAbility.getSpellAbilities(new CardCollection(c), ai)) {
                    if (sa.isSpell() && ComputerUtilMana.canPayManaCost(sa, ai, 0, false)) {
                        out.add(sa);
                    }
                }
            }
        } catch (final RuntimeException e) {
            out.clear();
        }
        return out;
    }

    /** Whether a landfall permanent in the hand goes before the land drop (the land waits one priority). */
    public static boolean payoffWaiting(final Player ai) {
        return !payoffsFirst(ai).isEmpty();
    }

    /** Whether this ability cracks a fetch land: sacrifice itself, a land from the library onto the battlefield. */
    public static boolean isFetch(final SpellAbility sa) {
        final Card host = sa == null ? null : sa.getHostCard();
        if (host == null || !host.isLand() || !sa.isAbility() || sa.getApi() != ApiType.ChangeZone) {
            return false;
        }
        final String origin = sa.getParam("Origin");
        final String dest = sa.getParam("Destination");
        return origin != null && origin.contains("Library") && "Battlefield".equals(dest)
                && sa.getPayCosts() != null && sa.getPayCosts().hasSpecificCostType(CostSacrifice.class);
    }

    /** "landfall-payoff-first" or "landfall-pump-useless" when the fetch land should not be cracked now, else null. */
    public static String fetchProblem(final Player ai, final SpellAbility sa) {
        if (ai == null || !isFetch(sa)) {
            return null;
        }
        try {
            if (payoffWaiting(ai)) {
                return "landfall-payoff-first";
            }
            if (!onlyUselessPumps(ai) || landNeededForMana(ai)) {
                return null;
            }
            return "landfall-pump-useless";
        } catch (final RuntimeException e) {
            return null;
        }
    }

    /** Whether the AI has landfall permanents in play and every one only pumps a creature that cannot attack now. */
    static boolean onlyUselessPumps(final Player ai) {
        boolean any = false;
        final forge.game.phase.PhaseHandler ph = ai.getGame().getPhaseHandler();
        final boolean beforeCombat = ph.isPlayerTurn(ai) && ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS);
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            final Kind kind = kind(c);
            if (kind == Kind.NONE) {
                continue;
            }
            if (kind == Kind.VALUE) {
                return false;
            }
            any = true;
            final boolean canAttack = beforeCombat && c.isCreature() && !c.isTapped()
                    && (!c.isSick() || c.hasKeyword("Haste")) && !c.hasKeyword("Defender");
            if (canAttack) {
                return false;
            }
        }
        return any;
    }

    /** Whether a spell in the hand costs one more than the AI can pay now, so the fetched land is what casts it. */
    static boolean landNeededForMana(final Player ai) {
        final int sources = ComputerUtilMana.getAvailableManaSources(ai, true).size();
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (c.isLand()) {
                continue;
            }
            final int cmc = c.getCMC();
            if (cmc > sources && cmc <= sources + 1) {
                return true;
            }
        }
        return false;
    }
}
