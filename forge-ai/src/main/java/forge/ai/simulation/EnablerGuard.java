package forge.ai.simulation;

import forge.ai.ComputerUtilMana;
import forge.card.mana.ManaCost;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Spells that only set something up for a later spell of the same turn, and are a wasted card when nothing follows.
 * <p>
 * <b>Channel</b> (an Effect that lets its controller pay 1 life for {C} until end of turn) is the case that was
 * reported: Flat Monte Carlo liked the spell, because the rollouts play the turn on after it, but the life-for-mana
 * ability is reserved for the payoff of a catalogued win combo ({@link ComboCatalog#canUseCommandManaSourceFor}), so
 * with no payoff to spend it on the AI paid GG and its Channel was gone for nothing. Every spell with the same shape
 * (a spell whose effect gives a mana ability that works from the Command zone for a life cost) is read the same way
 * from its script, so a card of the same kind needs no code.
 * <p>
 * The spell is cast only when a card in the hand is a payoff of a combo the Channel is a piece of, the lands left
 * after paying for the spell pay its coloured mana, and the lands left together with the life the AI can spare (down to
 * the {@code AILifeThreshold} of the script) pay the rest. A payoff with X (Fireball, Banefire) has to be able to kill:
 * a smaller one is burn the AI casts without Channel when it wants to.
 * <p>
 * Reads the real game state only; every question falls back to "no objection" on an unexpected state.
 */
public final class EnablerGuard {
    /** The life the AI keeps when the script says nothing (the Channel script says 5). */
    private static final int DEFAULT_LIFE_FLOOR = 5;

    private EnablerGuard() {
    }

    /** "enabler-no-payoff" when the spell would only set up a turn it has no card for, else null. */
    public static String problem(final Player ai, final SpellAbility sa) {
        if (ai == null || sa == null) {
            return null;
        }
        try {
            final Card host = sa.getHostCard();
            if (host == null || !sa.isSpell() || !host.isInZone(ZoneType.Hand) || host.isPermanent()) {
                return null;
            }
            final String script = lifeManaScript(sa, host);
            if (script == null) {
                return null;
            }
            return hasPayoff(ai, host, lifeFloor(ai, script)) ? null : "enabler-no-payoff";
        } catch (final RuntimeException e) {
            return null; // a refinement only: never let it break a turn
        }
    }

    /** Whether the spell is of the Channel kind: an effect that gives a Command-zone mana ability paid with life. */
    public static boolean grantsLifeMana(final SpellAbility sa) {
        final Card host = sa == null ? null : sa.getHostCard();
        return host != null && lifeManaScript(sa, host) != null;
    }

    /** The script of the mana ability the spell gives, or null for any other spell. */
    private static String lifeManaScript(final SpellAbility sa, final Card host) {
        if (sa.getApi() != ApiType.Effect || !sa.hasParam("Abilities")) {
            return null;
        }
        for (final String name : sa.getParam("Abilities").split(",")) {
            final String script = host.getSVar(name.trim());
            if (script != null && script.contains("ActivationZone$ Command") && script.contains("AB$ Mana")
                    && script.contains("PayLife")) {
                return script;
            }
        }
        return null;
    }

    private static int lifeFloor(final Player ai, final String script) {
        if (ai.cantLoseForZeroOrLessLife()) {
            return 0;
        }
        final int at = script.indexOf("AILifeThreshold$");
        if (at < 0) {
            return DEFAULT_LIFE_FLOOR;
        }
        final String rest = script.substring(at + "AILifeThreshold$".length()).trim();
        final StringBuilder digits = new StringBuilder();
        for (int i = 0; i < rest.length() && Character.isDigit(rest.charAt(i)); i++) {
            digits.append(rest.charAt(i));
        }
        return digits.length() == 0 ? DEFAULT_LIFE_FLOOR : Integer.parseInt(digits.toString());
    }

    /** Whether a card of the hand is a payoff the life-for-mana ability can pay for, after the spell is cast. */
    private static boolean hasPayoff(final Player ai, final Card enabler, final int floor) {
        final int life = ai.getLife() - floor;
        if (life <= 0) {
            return false;
        }
        final int lands = Math.max(0, ComputerUtilMana.getAvailableManaEstimate(ai) - enabler.getCMC());
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (c == enabler || c.isLand()) {
                continue;
            }
            final SpellAbility cast = c.getFirstSpellAbility();
            if (cast == null || !cast.isSpell() || !ComboCatalog.canUseCommandManaSourceFor(ai, enabler, cast)) {
                continue;
            }
            if (affordable(ai, c, lands, life)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The life pays generic mana only: the coloured part of the cost must be there in lands, and the rest of the cost
     * (with an X that kills, for a damage spell) must be covered by the lands left and the life that can be spared.
     */
    private static boolean affordable(final Player ai, final Card payoff, final int lands, final int life) {
        final ManaCost cost = payoff.getManaCost();
        if (cost == null || cost.isNoCost()) {
            return true;
        }
        final int coloured = cost.getCMC() - cost.getGenericCost();
        if (lands < coloured) {
            return false;
        }
        int needed = cost.getCMC();
        if (cost.countX() > 0) {
            needed += lethalX(ai, payoff);
        }
        return lands + life >= needed;
    }

    /** The X a damage spell needs to kill the opponent with the least life; one for any other X spell. */
    private static int lethalX(final Player ai, final Card payoff) {
        boolean damage = false;
        for (final SpellAbility a : payoff.getSpellAbilities()) {
            damage |= a.getApi() == ApiType.DealDamage;
        }
        if (!damage) {
            return 1;
        }
        int least = Integer.MAX_VALUE;
        for (final Player opp : ai.getOpponents()) {
            least = Math.min(least, opp.getLife());
        }
        return least == Integer.MAX_VALUE ? 1 : Math.max(1, least);
    }
}
