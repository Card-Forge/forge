package forge.ai.simulation;

import forge.ai.ComputerUtilCost;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.cost.CostDiscard;
import forge.game.cost.CostSacrifice;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Breakthrough with Lion's Eye Diamond, the turn one line of Legacy Dredge. Breakthrough for X=0 draws four cards and
 * then throws the whole hand away; every one of those draws is replaced by a dredge when a dredger is in the
 * graveyard. Cracking Lion's Eye Diamond while Breakthrough is on the stack puts the dredgers of the hand into the
 * graveyard first, so all four draws become dredges and a large part of the library is milled in one go.
 * <p>
 * The picker never plays a mana ability on its own, so this is answered here, the way the tutor storm solver answers
 * its own Infernal Tutor: {@link #response} is asked when a spell of the AI is on the stack and gives the mana ability
 * of the Diamond when cracking it is worth it. Casting Breakthrough (X stays 0: an unset X is paid as 0) and the
 * Diamond is left to Flat Monte Carlo, which is helped by {@link #setupBonus}.
 */
public final class DredgeLine {
    /** Four dredges mill 20 to 25 cards: the library must not be the limit. */
    public static final int MIN_LIBRARY = 24;

    private DredgeLine() {
    }

    /** Lion's Eye Diamond and its kin: a mana ability that is paid with the hand and the permanent itself. */
    public static boolean isHandDiscardMana(final SpellAbility sa) {
        return sa != null && sa.isManaAbility() && sa.getPayCosts() != null
                && sa.getPayCosts().hasSpecificCostType(CostDiscard.class)
                && sa.getPayCosts().hasSpecificCostType(CostSacrifice.class);
    }

    /** The mana ability of the card that discards the hand, or null for any other card. */
    static SpellAbility handDiscardMana(final Card c) {
        for (final SpellAbility sa : c.getManaAbilities()) {
            if (isHandDiscardMana(sa)) {
                return sa;
            }
        }
        return null;
    }

    /** Whether the card is a draw spell that throws the hand away (Breakthrough). */
    static boolean isRefill(final Card c) {
        return c != null && AiCardLists.has(AiCardLists.Role.DREDGE_REFILL, c.getName());
    }

    /** Whether the spell on top of the stack is a refill spell of this player's own. */
    static boolean refillOnStack(final Player ai) {
        final Game game = ai.getGame();
        if (game.getStack().isEmpty()) {
            return false;
        }
        final SpellAbility top = game.getStack().peekAbility();
        return top != null && top.isSpell() && ai.equals(top.getActivatingPlayer()) && isRefill(top.getHostCard());
    }

    /**
     * The mana ability to play now, or null: a refill spell of the AI is on the stack, the deck is a dredge deck, the
     * library can take the dredges, a dredger is in the hand or already in the graveyard and something that belongs in
     * the graveyard is in the hand, and Lion's Eye Diamond is on the battlefield and can be paid.
     */
    public static SpellAbility response(final Player ai, final ComboPlan combos) {
        if (ai == null || combos == null || !combos.has(ComboPlan.Kind.DREDGE) || !refillOnStack(ai)
                || ai.getCardsIn(ZoneType.Library).size() < MIN_LIBRARY) {
            return null;
        }
        boolean dump = false;
        boolean dredger = false;
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            dump |= ComboPlan.isDredgeHandPiece(c);
            dredger |= c.hasKeyword(Keyword.DREDGE);
        }
        for (final Card c : ai.getCardsIn(ZoneType.Graveyard)) {
            dredger |= c.hasKeyword(Keyword.DREDGE);
        }
        if (!dump || !dredger) {
            return null; // nothing the Diamond would put where it belongs
        }
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            final SpellAbility mana = handDiscardMana(c);
            if (mana == null) {
                continue;
            }
            mana.setActivatingPlayer(ai);
            if (mana.canPlay() && ComputerUtilCost.canPayCost(mana, ai, false)) {
                return mana;
            }
        }
        return null;
    }

    /**
     * What holding the line is worth in evaluator points: a refill spell and a dredger in the hand, and the Diamond
     * (in hand or already in play) to crack for it. The refill spell alone still throws the hand into the graveyard,
     * which the rollouts see for themselves; the Diamond is what the rollouts cannot see, because the picker does not
     * play it, so it is worth a bonus that makes casting it first the natural start.
     */
    static int setupBonus(final Player ai) {
        if (ai.getCardsIn(ZoneType.Library).size() < MIN_LIBRARY) {
            return 0;
        }
        boolean refill = false;
        boolean dredger = false;
        boolean diamond = false;
        boolean diamondInPlay = false;
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            refill |= isRefill(c);
            dredger |= c.hasKeyword(Keyword.DREDGE);
            diamond |= handDiscardMana(c) != null;
        }
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (handDiscardMana(c) != null) {
                diamond = true;
                diamondInPlay = true;
            }
        }
        if (!refill || !dredger) {
            return 0;
        }
        return diamondInPlay ? DIAMOND_IN_PLAY_BONUS : diamond ? DIAMOND_IN_HAND_BONUS : 30;
    }

    /** The Diamond in play is ready to crack, the one in hand still has to be cast first: that is what the gap is for. */
    static final int DIAMOND_IN_PLAY_BONUS = 110;
    static final int DIAMOND_IN_HAND_BONUS = 60;

    /**
     * Whether the refill spell may be cast now. Cast alone it only throws the hand away, which the rollouts like
     * (the graveyard fills), so it is held back until the line is complete: the library can take the dredges, a
     * dredger and something for the graveyard are in the hand or the graveyard, and a Diamond is in play to crack
     * for it. Other cards and other decks are never held back.
     */
    public static boolean mustWait(final Player ai, final ComboPlan combos, final SpellAbility sa) {
        if (ai == null || combos == null || sa == null || !sa.isSpell() || !isRefill(sa.getHostCard())
                || !combos.has(ComboPlan.Kind.DREDGE)) {
            return false;
        }
        if (ai.getCardsIn(ZoneType.Library).size() < MIN_LIBRARY) {
            return true;
        }
        boolean dump = false;
        boolean dredger = false;
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            dump |= ComboPlan.isDredgeHandPiece(c);
            dredger |= c.hasKeyword(Keyword.DREDGE);
        }
        for (final Card c : ai.getCardsIn(ZoneType.Graveyard)) {
            dredger |= c.hasKeyword(Keyword.DREDGE);
        }
        if (!dump || !dredger) {
            return true;
        }
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (handDiscardMana(c) != null) {
                return false;
            }
        }
        return true;
    }
}
