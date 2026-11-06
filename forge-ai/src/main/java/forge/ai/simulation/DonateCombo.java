package forge.ai.simulation;

import forge.ai.ability.PermanentAi;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Illusions of Grandeur and Donate: the enchantment gains twenty life and takes twenty when it leaves, so it is a
 * combo only when Donate gives it to the opponent, who then loses the twenty (the cumulative upkeep sees to the
 * "leaves"). Cast alone it is a loan the AI repays with interest.
 *
 * <p>The scripts say so (Illusions of Grandeur: MustHaveInHand Donate, a second copy never, mana for both, or life low
 * enough that the twenty are needed), and the heuristics follow them. Flat Monte Carlo does not: the plays the
 * heuristics turn down come back as discovered plays, and a rollout sees only the twenty life gained, since the
 * repayment lies beyond what it plays out. This states the script's rule for the plays discovery adds and the rollouts
 * choose, and the other half of the pair: Donate with nothing worth giving away.
 */
final class DonateCombo {
    private DonateCombo() {
    }

    /** Why the play breaks the Illusions-Donate pair, or null: "donate-combo-incomplete" or "donate-nothing-to-give". */
    static String problem(final Player ai, final SpellAbility sa) {
        if (ai == null || sa == null || !sa.isSpell() || sa.getHostCard() == null) {
            return null;
        }
        final Card host = sa.getHostCard();
        if (host.isPermanent() && needsItsPartner(host) && PermanentAi.castPreferenceSaysNo(ai, host)) {
            return "donate-combo-incomplete";
        }
        if ("DonateTargetPlayer".equals(sa.getParam("AILogic")) && !hasSomethingToGive(ai)) {
            return "donate-nothing-to-give";
        }
        return null;
    }

    /** A permanent whose script says it is only cast with another card in hand (Illusions of Grandeur, Embargo). */
    private static boolean needsItsPartner(final Card host) {
        return host.hasSVar("AICastPreference") && host.getSVar("AICastPreference").contains("MustHaveInHand$");
    }

    /** Whether the AI controls a permanent that is worth giving to the opponent (the scripts mark them DonateMe). */
    private static boolean hasSomethingToGive(final Player ai) {
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c.hasSVar("DonateMe")) {
                return true;
            }
        }
        return false;
    }
}
