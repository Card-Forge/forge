package forge.ai.simulation;

import java.util.Locale;

import forge.ai.ComputerUtilCost;
import forge.ai.ComputerUtilMana;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Stiflenought: a permanent that sacrifices itself on entering unless a cost is paid (Phyrexian Dreadnought) and a
 * counterspell for abilities (Stifle) that takes that trigger off the stack. A 12/12 trampler for one mana.
 * Read from the oracle text of the cards, not from names.
 */
public final class StifleCombo {
    private StifleCombo() {
    }

    /** "When it enters, sacrifice it unless ...": Phyrexian Dreadnought. */
    public static boolean isSelfSacrificeUnless(final Card c) {
        if (c == null || c.isLand() || !c.isPermanent()) {
            return false;
        }
        final String text = c.getOracleText();
        return text != null && text.toLowerCase(Locale.ROOT).contains("sacrifice it unless");
    }

    /** Stifle and Consign to Memory: counter a triggered ability (Stifle also an activated one). */
    public static boolean isAbilityCounter(final Card c) {
        if (c == null || c.isLand()) {
            return false;
        }
        final String text = c.getOracleText();
        if (text == null) {
            return false;
        }
        final String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("counter target activated or triggered ability")
                || lower.contains("counter target triggered ability");
    }

    private static SpellAbility counterSpell(final Card c) {
        if (!isAbilityCounter(c)) {
            return null;
        }
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isSpell() && sa.getApi() == ApiType.Counter) {
                return sa;
            }
        }
        return null;
    }

    /**
     * The Stifle to cast now, aimed at the AI's own sacrifice trigger that is on top of the stack; null when the top
     * of the stack is anything else or nothing can be cast. The general rules never answer the AI's own abilities.
     */
    public static SpellAbility stifleOwnTrigger(final Player ai, final SpellAbility top) {
        if (ai == null || top == null || !top.isTrigger() || !ai.equals(top.getActivatingPlayer())) {
            return null;
        }
        final Card host = top.getHostCard();
        if (!isSelfSacrificeUnless(host) || !host.isInZone(ZoneType.Battlefield) || !ai.equals(host.getController())) {
            return null;
        }
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            final SpellAbility stifle = counterSpell(c);
            if (stifle == null) {
                continue;
            }
            stifle.setActivatingPlayer(ai);
            try {
                if (stifle.canPlay() && stifle.canTargetSpellAbility(top)) {
                    stifle.resetTargets();
                    stifle.getTargets().add(top);
                    if (ComputerUtilCost.canPayCost(stifle, ai, false)) {
                        return stifle;
                    }
                    stifle.resetTargets();
                }
            } catch (final RuntimeException e) {
                stifle.resetTargets();
            }
        }
        return null;
    }

    /**
     * Whether the cast is the Dreadnought with a Stifle in hand and mana for both (the cost of the Dreadnought and
     * the Stifle together): a play the general rules turn down, as the sacrifice cost cannot be paid.
     */
    public static boolean readyToCast(final Player ai, final SpellAbility cast) {
        if (ai == null || cast == null || !cast.isSpell() || cast.getHostCard() == null || cast.getPayCosts() == null) {
            return false;
        }
        final Card host = cast.getHostCard();
        if (!isSelfSacrificeUnless(host) || !host.isInZone(ZoneType.Hand) || !ai.getGame().getStack().isEmpty()) {
            return false;
        }
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            final SpellAbility stifle = counterSpell(c);
            if (stifle == null || stifle.getPayCosts() == null) {
                continue;
            }
            final ManaCostBeingPaid total = new ManaCostBeingPaid(cast.getPayCosts().getTotalMana());
            total.addManaCost(stifle.getPayCosts().getTotalMana());
            cast.setActivatingPlayer(ai);
            try {
                if (cast.canCastTiming(ai) && cast.canPlay() && ComputerUtilCost.canPayCost(cast, ai, false)
                        && ComputerUtilMana.canPayManaCost(total, cast, ai, false)) {
                    return true;
                }
            } catch (final RuntimeException e) {
                // try the next counter in hand
            }
        }
        return false;
    }

    /**
     * The Dreadnought is in the hand: the Stifle waits for its trigger instead of being spent on whatever the
     * opponent does first (a Saga chapter). True for an ability counter then.
     */
    public static boolean keepsForDreadnought(final Player ai, final SpellAbility counter) {
        if (ai == null || counter == null || !isAbilityCounter(counter.getHostCard())) {
            return false;
        }
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (isSelfSacrificeUnless(c)) {
                return true;
            }
        }
        return false;
    }

    /** The first of the plays that is the Dreadnought ready to be cast with its Stifle ({@link #readyToCast}), or null. */
    public static SpellAbility sureCast(final Player ai, final Iterable<SpellAbility> candidates) {
        if (ai == null || candidates == null) {
            return null;
        }
        for (final SpellAbility sa : candidates) {
            if (readyToCast(ai, sa)) {
                return sa;
            }
        }
        return null;
    }

    /** For tests and the log: why the Dreadnought is or is not ready to be cast. */
    public static String explain(final Player ai, final SpellAbility cast) {
        final Card host = cast == null ? null : cast.getHostCard();
        boolean stifle = false;
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            stifle |= counterSpell(c) != null;
        }
        if (cast != null && cast.getActivatingPlayer() == null) {
            cast.setActivatingPlayer(ai); // the checks below want an activator
        }
        return "sacrificeUnless=" + isSelfSacrificeUnless(host)
                + ", inHand=" + (host != null && host.isInZone(ZoneType.Hand))
                + ", stackEmpty=" + ai.getGame().getStack().isEmpty()
                + ", stifleInHand=" + stifle
                + ", timing=" + (cast != null && cast.canCastTiming(ai))
                + ", canPlay=" + (cast != null && cast.canPlay());
    }

    /** A hand with the Dreadnought, the Stifle and two lands is the deck working: worth a keep. */
    public static int handBonus(final CardCollectionView hand) {
        boolean dread = false;
        boolean stifle = false;
        int lands = 0;
        for (final Card c : hand) {
            if (c.isLand()) {
                lands++;
            } else {
                dread |= isSelfSacrificeUnless(c);
                stifle |= isAbilityCounter(c);
            }
        }
        return dread && stifle && lands >= 2 ? 25 : 0;
    }
}
