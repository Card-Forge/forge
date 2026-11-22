package forge.ai;

import forge.card.MagicColor;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Abilities that only tap a target land (Rishadan Port, Rishadan Dockhand): used on the opponent's upkeep against the land
 * that hurts most to lose, and at the start of their combat against a land that has been turned into an attacker when
 * the AI is in danger. The target is read from what the land produces, so it works for any land.
 */
public final class ManaDenial {
    private static final int COLOR_BASE = 20;
    private static final int COLOR_LOCK = 100;
    private static final int EXTRA_MANA = 60;
    /** What a land has to be worth to spend the mana on it, by the number of lands the opponent has. */
    private static final int WORTH_FEW = 40;
    private static final int WORTH = 90;
    private static final int WORTH_MANY = 110; // only a colour that land alone makes (120) gets through
    private static final int FEW_LANDS = 3;
    private static final int MANY_LANDS = 7;
    /** A land the evaluator rates this high (Gaea's Cradle, Tolarian Academy) is always worth the mana. */
    private static final int HIGH_PRIORITY = 150;

    private ManaDenial() {
    }

    /**
     * An activated ability that only taps a single target land, for a cost without X.
     */
    public static boolean isLandTapper(final SpellAbility sa) {
        if (sa == null || sa.getApi() != ApiType.Tap || !sa.isActivatedAbility() || sa.isSpell() || !sa.usesTargeting()
                || sa.costHasManaX() || sa.getTargetRestrictions() == null) {
            return false;
        }
        final String[] valid = sa.getTargetRestrictions().getValidTgts();
        if (valid == null || valid.length == 0) {
            return false;
        }
        for (final String v : valid) {
            if (!v.startsWith("Land")) {
                return false;
            }
        }
        return sa.getTargetRestrictions().getMaxTargets(sa.getHostCard(), sa) == 1;
    }

    /**
     * The land to tap now, or null when this is not the moment or nothing is worth the mana.
     */
    public static Card choose(final Player ai, final SpellAbility sa) {
        try {
            if (!isLandTapper(sa)) {
                return null;
            }
            final PhaseHandler ph = ai.getGame().getPhaseHandler();
            if (ph.isPlayerTurn(ai)) {
                return null; // the land untaps before it matters
            }
            sa.setActivatingPlayer(ai);
            if (ph.is(PhaseType.UPKEEP)) {
                return sparesMana(ai, sa) ? bestManaLand(ai, sa) : null;
            }
            if (ph.is(PhaseType.COMBAT_BEGIN) && ComputerUtil.aiLifeInDanger(ai, false, 0)) {
                return bestManland(ai, sa);
            }
            return null;
        } catch (final RuntimeException e) {
            return null; // a timing refinement only: never let it break a turn
        }
    }

    /** For tests and debugging: why {@link #choose} says no (or "ok"), one check at a time. */
    static String why(final Player ai, final SpellAbility sa) {
        try {
            if (!isLandTapper(sa)) {
                return "not-a-land-tapper api=" + sa.getApi() + " targeting=" + sa.usesTargeting()
                        + " activated=" + sa.isActivatedAbility() + " x=" + sa.costHasManaX();
            }
            final PhaseHandler ph = ai.getGame().getPhaseHandler();
            if (ph.isPlayerTurn(ai)) {
                return "own-turn phase=" + ph.getPhase();
            }
            if (!ph.is(PhaseType.UPKEEP) && !ph.is(PhaseType.COMBAT_BEGIN)) {
                return "wrong-phase phase=" + ph.getPhase() + " turn=" + ph.getPlayerTurn();
            }
            sa.setActivatingPlayer(ai);
            if (ph.is(PhaseType.UPKEEP) && !sparesMana(ai, sa)) {
                return "mana-not-spare";
            }
            final CardCollection lands = untappedLands(ai, sa);
            if (lands.isEmpty()) {
                return "no-untapped-targetable-land opponents=" + ai.getOpponents().getCardsIn(ZoneType.Battlefield);
            }
            final Card pick = ph.is(PhaseType.UPKEEP) ? bestManaLand(ai, sa) : bestManland(ai, sa);
            return pick == null ? "nothing-worth-the-mana lands=" + lands : "ok " + pick;
        } catch (final RuntimeException e) {
            return "exception " + e;
        }
    }

    private static CardCollection untappedLands(final Player ai, final SpellAbility sa) {
        final CardCollection lands = CardLists.filter(ai.getOpponents().getCardsIn(ZoneType.Battlefield),
                CardPredicates.LANDS, CardPredicates.UNTAPPED);
        // not a land that something on the stack is already tapping (a second Port aims at another land)
        final CardCollection targetable = CardLists.getTargetableCards(lands, sa);
        targetable.removeAll(ComputerUtilAbility.getCardsTargetedWithApi(ai, targetable, sa, ApiType.Tap));
        return targetable;
    }

    private static Card bestManaLand(final Player ai, final SpellAbility sa) {
        Card best = null;
        int bestScore = Integer.MIN_VALUE;
        for (final Card land : untappedLands(ai, sa)) {
            final CardCollection theirs = CardLists.filter(land.getController().getCardsIn(ZoneType.Battlefield),
                    CardPredicates.LANDS, CardPredicates.UNTAPPED);
            final int denial = denial(land, theirs);
            final int priority = ComputerUtilCard.evaluateLandRemovalPriority(ai, land, sa);
            if (denial < worth(theirs.size()) && priority < HIGH_PRIORITY) {
                continue; // plenty of lands and no colour or extra mana to cut off
            }
            final int score = denial + priority;
            if (score > bestScore) {
                bestScore = score;
                best = land;
            }
        }
        return best;
    }

    /**
     * An opposing land that is a creature now and could attack (the one the AI most needs not to face), or, when the AI
     * has nothing untapped to block with, one that can still become a creature: tapped before it is animated, it cannot attack.
     */
    private static Card bestManland(final Player ai, final SpellAbility sa) {
        Card best = null;
        int bestEval = Integer.MIN_VALUE;
        for (final Card land : untappedLands(ai, sa)) {
            if (!land.isCreature() || !CombatUtil.canAttack(land)) {
                continue;
            }
            final int eval = ComputerUtilCard.evaluateCreature(land);
            if (eval > bestEval) {
                bestEval = eval;
                best = land;
            }
        }
        if (best != null || ai.getCreaturesInPlay().anyMatch(CardPredicates.UNTAPPED)) {
            return best;
        }
        // nothing to block with: a land that can still be animated is the attacker to stop, tapped before it becomes one
        int bestPriority = Integer.MIN_VALUE;
        for (final Card land : untappedLands(ai, sa)) {
            if (land.isCreature() || !canAnimate(land)) {
                continue;
            }
            final int priority = ComputerUtilCard.evaluateLandRemovalPriority(ai, land, sa);
            if (priority > bestPriority) {
                bestPriority = priority;
                best = land;
            }
        }
        return best;
    }

    /**
     * How much losing this land hurts the controller: colours it alone makes (or few lands make), and extra mana.
     */
    static int denial(final Card land, final CardCollection theirs) {
        int score = 0;
        for (final byte color : MagicColor.WUBRG) {
            if (!produces(land, color)) {
                continue;
            }
            int sources = 0;
            for (final Card other : theirs) {
                if (produces(other, color)) {
                    sources++;
                }
            }
            score += COLOR_BASE + COLOR_LOCK / Math.max(1, sources);
        }
        score += Math.max(0, land.getMaxManaProduced() - 1) * EXTRA_MANA;
        return score;
    }

    private static boolean produces(final Card land, final byte color) {
        for (final SpellAbility m : ComputerUtilMana.getAIPlayableMana(land)) {
            m.setActivatingPlayer(land.getController());
            if (m.canProduce(MagicColor.toShortString(color))) {
                return true;
            }
        }
        return false;
    }

    /** What a land has to be worth to spend the mana on it: little against a handful of lands, a lot against many. */
    private static int worth(final int opponentLands) {
        return opponentLands <= FEW_LANDS ? WORTH_FEW : opponentLands >= MANY_LANDS ? WORTH_MANY : WORTH;
    }

    private static boolean canAnimate(final Card land) {
        for (final SpellAbility ability : land.getNonManaAbilities()) {
            if (ability.getApi() == ApiType.Animate && ability.isActivatedAbility()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the mana the ability costs is spare on the opponent's turn: the lands untap before the AI's own turn, so
     * a creature or a sorcery in the hand does not need it, but whatever the AI may cast or activate at instant speed
     * before then does (a held counterspell or trick, a regeneration shield, a manland that blocks). Each is asked
     * whether it can still be paid for once the ability's mana, and the land of the ability itself, are set aside.
     */
    private static boolean sparesMana(final Player ai, final SpellAbility sa) {
        try {
            final int cost = sa.getPayCosts() == null || sa.getPayCosts().getTotalMana() == null ? 0
                    : sa.getPayCosts().getTotalMana().getCMC();
            final Card host = sa.getHostCard();
            // a land that taps for the ability is no source of mana for the others
            final int reserved = cost + (host != null && host.isLand() && !host.getManaAbilities().isEmpty() ? 1 : 0);
            for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
                if (c.isLand() || !(c.isInstant() || c.hasKeyword(forge.game.keyword.Keyword.FLASH))) {
                    continue;
                }
                for (final SpellAbility spell : c.getSpellAbilities()) {
                    if (spell.isSpell() && needsTheMana(ai, spell, reserved)) {
                        return false;
                    }
                }
            }
            for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
                for (final SpellAbility ability : c.getNonManaAbilities()) {
                    if (ability == sa || !ability.isActivatedAbility() || ability.isSpell() || isLandTapper(ability)
                            || ability.getPayCosts() == null || ability.getPayCosts().getTotalMana() == null
                            || ability.getPayCosts().getTotalMana().getCMC() == 0) {
                        continue;
                    }
                    ability.setActivatingPlayer(ai);
                    if (ability.canPlay() && needsTheMana(ai, ability, reserved)) {
                        return false;
                    }
                }
            }
            return true;
        } catch (final RuntimeException e) {
            return false; // not sure it is safe: keep the mana
        }
    }

    /** Payable now, and no longer once {@code reserved} mana is gone. */
    private static boolean needsTheMana(final Player ai, final SpellAbility other, final int reserved) {
        other.setActivatingPlayer(ai);
        return ComputerUtilMana.canPayManaCost(other, ai, 0, false)
                && !ComputerUtilMana.canPayManaCost(other, ai, reserved, false);
    }
}
