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

    /** Strategic value for a DonateMe permanent that the AI owns but an opponent now controls. */
    private static final int GIFTED_TARGET_BONUS = 160;

    /** Why the play breaks the Illusions-Donate pair, or null: "donate-combo-incomplete" or "donate-nothing-to-give". */
    static String problem(final Player ai, final SpellAbility sa) {
        if (ai == null || sa == null || !sa.isSpell() || sa.getHostCard() == null) {
            return null;
        }
        final Card host = sa.getHostCard();
        // Deploy cost reducers first. FlatMC can discover AI:RemoveDeck medallions, but without this guard it may
        // take the immediate life gain from Illusions before it has deployed the reducer that makes Donate safer.
        // Keep the script's low-life emergency exception: below four life, the twenty life may be needed immediately.
        if ("Illusions of Grandeur".equals(host.getName()) && ai.getLife() >= 4) {
            for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
                if (PermanentAi.isBlueSpellCostReducer(c)) {
                    return "donate-setup-first";
                }
            }
        }
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

    /**
     * Whether a DonateMe permanent owned by the AI has actually been given to an opponent. Ownership is important:
     * an opponent's own Illusions of Grandeur remains a threat, while the same card after our Donate is the intended
     * payoff and must not be scored as an opposing threat.
     */
    static boolean isGiftedTarget(final Player ai, final Card card) {
        return ai != null && card != null && card.hasSVar("DonateMe") && card.getOwner() == ai
                && card.getController() != null && card.getController().isOpponentOf(ai);
    }

    /**
     * A completed Donate play must be worth more than keeping the Donate spell in hand. Otherwise the rollout sees
     * only the permanent changing sides; the delayed downside (cumulative upkeep, a drawback trigger, etc.) is beyond
     * its horizon. Cap the term so multiple gifted permanents cannot dominate the rest of the board evaluation.
     */
    static int term(final Player ai) {
        if (ai == null) {
            return 0;
        }
        int gifts = 0;
        for (final Player opp : ai.getOpponents()) {
            for (final Card c : opp.getCardsIn(ZoneType.Battlefield)) {
                if (isGiftedTarget(ai, c)) {
                    gifts++;
                }
            }
        }
        return Math.min(gifts, 2) * GIFTED_TARGET_BONUS;
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
