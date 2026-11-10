package forge.ai.simulation;

import forge.game.card.Card;
import forge.game.cost.Cost;
import forge.game.keyword.Keyword;
import forge.game.keyword.KeywordInterface;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Special actions that are not spells: plotting, foretelling and suspending a card from the hand, and turning a
 * face-down creature face up.
 * <p>
 * Plot, foretell and suspend exile the card now to cast it later for less (free for plot and suspend, the foretell
 * cost for foretell). The evaluator counts a card in hand and nothing for the exile, so a rollout only saw a card
 * lost and the mana spent: the heuristics alone played them, and only in the second main phase. Here the saving is a
 * bonus for the rollouts to weigh against casting the card, or anything else, with the same mana.
 * <p>
 * Turning a creature face up is a trick like a pump: the heuristics did it at the first chance, before the opponent had
 * any decision to make about the face-down creature. It is kept for the moments that make it a surprise (the blocks are
 * down, the opponent's end step) while the creature could still be in the AI's combat.
 */
public final class SpecialActions {
    /** Evaluator points for a mana that the deferred cast will not have to pay; a card is worth 100-250 in play. */
    static final int POINTS_PER_MANA_SAVED = 20;
    /** Mana a suspend is worth for a card without a mana cost: it cannot be cast any other way, so the wait costs nothing. */
    static final int NO_COST_SAVING = 3;

    /** Evaluator points lost for each turn a suspended card waits for its counters. */
    static final int POINTS_PER_TURN_WAITED = 15;

    private SpecialActions() {
    }

    /** Plot, foretell or suspend, of a card in the hand: the card goes to exile to be cast later. */
    public static boolean isDeferral(final SpellAbility sa) {
        final Card host = sa == null ? null : sa.getHostCard();
        return host != null && host.isInZone(ZoneType.Hand) && !host.isLand()
                && (sa.isPlotting() || sa.isForetelling() || sa.isKeyword(Keyword.SUSPEND));
    }

    /** Mana of the later cast that the action saves, less the turns waited for (in mana), or 0. */
    public static int saving(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final int cmc = host.getCMC();
        if (sa.isPlotting()) {
            return cmc; // cast for free on a later turn, at sorcery speed
        }
        if (sa.isForetelling()) {
            final int cost = keywordCost(host, "Foretell:", 1);
            return cost < 0 ? 0 : Math.max(0, cmc - cost);
        }
        final String suspend = keywordOriginal(host, "Suspend:");
        if (suspend == null) {
            return 0;
        }
        if (host.getManaCost().isNoCost()) {
            return NO_COST_SAVING; // Restore Balance, Ancestral Vision: suspending is the only way the card is ever cast
        }
        final String[] parts = suspend.split(":");
        int turns = 1;
        try {
            turns = Integer.parseInt(parts[1].trim());
        } catch (final RuntimeException e) {
            // the keyword has no number: count one turn
        }
        final int waited = turns * POINTS_PER_TURN_WAITED / POINTS_PER_MANA_SAVED;
        return Math.max(0, cmc - Math.max(0, waited));
    }

    private static String keywordOriginal(final Card host, final String start) {
        for (final KeywordInterface k : host.getKeywords()) {
            final String original = k.getOriginal();
            if (original != null && original.startsWith(start)) {
                return original;
            }
        }
        return null;
    }

    /** The mana value of the cost in the keyword ("Foretell:{1}{U}" is 2), or -1 when there is none to read. */
    private static int keywordCost(final Card host, final String start, final int part) {
        final String original = keywordOriginal(host, start);
        if (original == null) {
            return -1;
        }
        try {
            return new Cost(original.split(":")[part], true).getTotalMana().getCMC();
        } catch (final RuntimeException e) {
            return -1;
        }
    }

    /**
     * Whether discovery may offer the action: in the second main phase, as the heuristics did, with the mana spare.
     * The rollouts then decide against casting the card now and against the other plays.
     */
    public static boolean isDiscoverable(final Player ai, final SpellAbility sa) {
        if (!isDeferral(sa) || saving(sa) <= 0) {
            return false;
        }
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        return ph.isPlayerTurn(ai) && !ph.getPhase().isBefore(PhaseType.MAIN2)
                && FlatMonteCarloPicker.sparesMana(ai, sa);
    }

    /** A face-down creature's turn-face-up ability, a special action of the battlefield. */
    public static boolean isTurnFaceUp(final SpellAbility sa) {
        final Card host = sa == null ? null : sa.getHostCard();
        return host != null && sa.isTurnFaceUp() && host.isFaceDown() && host.isInZone(ZoneType.Battlefield);
    }

    /**
     * The creature could still attack this turn and the blocks are not down: turning it face up now only tells the
     * opponent what it is. Kept for after the blocks (see {@link FlatMonteCarloPicker#isCombatTrick}) or the second main.
     */
    public static boolean waitsForCombat(final Player ai, final SpellAbility sa) {
        if (!isTurnFaceUp(sa)) {
            return false;
        }
        final Card host = sa.getHostCard();
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !host.isCreature() || host.isTapped() || host.isSick()
                || !host.getController().equals(ai)) {
            return false;
        }
        return ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_BLOCKERS);
    }

    /** Value of the play, in evaluator points, added to the rollouts' value of the position. */
    static int bonus(final SpellAbility sa, final FlatMonteCarloPicker.Config cfg) {
        if (sa == null || cfg.strategicWeightPercent <= 0 || !isDeferral(sa)) {
            return 0;
        }
        try {
            return saving(sa) * POINTS_PER_MANA_SAVED * cfg.strategicWeightPercent / 100;
        } catch (final RuntimeException e) {
            return 0; // a refinement only
        }
    }
}
