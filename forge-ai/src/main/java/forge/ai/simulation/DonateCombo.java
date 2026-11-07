package forge.ai.simulation;

import java.util.List;

import forge.ai.ComputerUtilCost;
import forge.ai.ability.PermanentAi;
import forge.game.ability.ApiType;
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
public final class DonateCombo {
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

    /**
     * Priority for a permanent deliberately given to an opponent. Removing Illusions is especially important because
     * its leave trigger makes its current controller lose 20 life. A lethal target outranks all ordinary targets.
     */
    static int targetRank(final Player ai, final Card card) {
        if (!isGiftedIllusions(ai, card)) {
            return 0;
        }
        final Player opponent = card.getController();
        if (opponent == null || !opponent.canLoseLife() || opponent.cantLose()
                || opponent.cantLoseForZeroOrLessLife()) {
            return 0;
        }
        return opponent.getLife() <= 20 ? 50000 : 20000;
    }

    /**
     * A currently payable, single-target play that makes donated Illusions leave the battlefield is a direct win when
     * it causes the only opponent to lose 20 life. This is checked before FlatMC compares value plays such as Gush.
     * The spell is only returned after its target, timing, restrictions and full cost (including X) have been checked.
     */
    public static SpellAbility sureFinish(final Player ai, final List<SpellAbility> choices) {
        if (ai == null || choices == null || choices.isEmpty() || ai.getOpponents().size() != 1) {
            return null;
        }
        final Player opponent = ai.getWeakestOpponent();
        if (opponent == null || !opponent.canLoseLife() || opponent.cantLose()
                || opponent.cantLoseForZeroOrLessLife() || opponent.getLife() > 20) {
            return null;
        }

        for (final Card gifted : opponent.getCardsIn(ZoneType.Battlefield)) {
            if (!isGiftedIllusions(ai, gifted)) {
                continue;
            }
            for (final SpellAbility spell : choices) {
                if (spell == null || (!spell.isSpell() && !spell.isActivatedAbility()) || spell.getHostCard() == null) {
                    continue;
                }
                spell.setActivatingPlayer(ai);
                final SpellAbility targeting = FlatMonteCarloPicker.singleTargetingAbility(spell);
                if (targeting == null || !removesBattlefieldPermanent(targeting, gifted)) {
                    continue;
                }

                // Repeal uses X to match the target's mana value. Do not guess X for unrelated X spells.
                final boolean hasXCost = spell.costHasManaX();
                final Integer previousX = spell.getXManaCostPaid();
                if (hasXCost) {
                    final String validTargets = targeting.getTargetRestrictions() == null ? ""
                            : String.join(",", targeting.getTargetRestrictions().getValidTgts());
                    if (!validTargets.contains("cmcEQX") && !validTargets.contains("cmcLEQX")) {
                        continue;
                    }
                    spell.setXManaCostPaid(gifted.getCMC());
                }

                if (!FlatMonteCarloPicker.applyTarget(spell, gifted)) {
                    if (hasXCost) {
                        spell.setXManaCostPaid(previousX);
                    }
                    continue;
                }
                try {
                    if (spell.canPlay() && ComputerUtilCost.canPayCost(spell, ai, false)) {
                        return spell; // Keep the selected target and X value for the real cast.
                    }
                } catch (final RuntimeException ignored) {
                    // An unusual cost or restriction is not a reason to interrupt the normal AI decision.
                }
                targeting.resetTargets();
                if (hasXCost) {
                    spell.setXManaCostPaid(previousX);
                }
            }
        }
        return null;
    }

    private static boolean isGiftedIllusions(final Player ai, final Card card) {
        return isGiftedTarget(ai, card) && "Illusions of Grandeur".equals(card.getName());
    }

    /** Only a target effect that actually removes the permanent counts as the Illusions finisher. */
    private static boolean removesBattlefieldPermanent(final SpellAbility targeting, final Card target) {
        final ApiType api = targeting.getApi();
        if (api == ApiType.Destroy) {
            return target.canBeDestroyed();
        }
        if (api == ApiType.Sacrifice) {
            return target.canBeSacrificedBy(targeting, true);
        }
        if (api != ApiType.ChangeZone || !targeting.getParamOrDefault("Origin", "").contains("Battlefield")) {
            return false;
        }
        final String destination = targeting.getParamOrDefault("Destination", "");
        if (destination.isEmpty() || destination.contains(ZoneType.Battlefield.toString())) {
            return false;
        }
        return !destination.contains("Exile") || target.canExiledBy(targeting, true);
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
