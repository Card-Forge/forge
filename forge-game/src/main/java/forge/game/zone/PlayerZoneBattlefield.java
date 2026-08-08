/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.game.zone;

import forge.card.GamePieceType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.StackedTokenCard;
import forge.game.player.Player;

import java.util.ArrayList;
import java.util.List;

public class PlayerZoneBattlefield extends PlayerZone {
    private static final long serialVersionUID = 5750837078903423978L;

    private boolean trigger = true;
    private CardCollection meldedCards = new CardCollection();
    private final List<StackedTokenCard> stackedTokens = new ArrayList<>();

    public PlayerZoneBattlefield(final ZoneType zone, final Player player) {
        super(zone, player);
    }

    public final void addToMelded(final Card c) {
        c.getZone().remove(c);
        c.setZone(this);
        meldedCards.add(c);
    }

    public final void removeFromMelded(final Card c) {
        meldedCards.remove(c);
    }

    @Override
    public final void add(final Card c, final Integer position, final Card latestState) {
        if (c == null) {
            throw new RuntimeException("PlayerZoneComesInto Play : add() object is null");
        }

        super.add(c, position, latestState);

        if (trigger) {
            c.setSickness(true);
        }
    }

    public final void setTriggers(final boolean b) {
        trigger = b;
    }

    @Override
    public void removeAllCards(boolean forcedWithoutEvents) {
        stackedTokens.clear();
        super.removeAllCards(forcedWithoutEvents);
    }

    /**
     * Attempts to merge the given token into an existing stack, or creates a new
     * stack entry if no compatible stack exists. The individual Card is removed
     * from cardList so it won't appear as a distinct game object, but the Card
     * reference itself remains valid for any bookkeeping that already holds it.
     *
     * @return true if the token was stacked (either merged or created new stack)
     */
    public final boolean tryStackToken(Card c) { // doc:1b DONE
        if (c == null || c.getGamePieceType() != GamePieceType.TOKEN) return false;
        for (StackedTokenCard stack : stackedTokens) {
            if (stack.canMerge(c)) {
                cardList.remove(c);
                stack.addQuantity(1);
                return true;
            }
        }
        // No compatible stack — start a new one
        cardList.remove(c);
        stackedTokens.add(new StackedTokenCard(c, 1));
        return true;
    }

    /**
     * Materializes any stacked tokens into individual Card objects in cardList.
     * Call this before any read that requires distinct Card references.
     */
    public final void expandStacks() {
        if (stackedTokens.isEmpty()) return;
        // Snapshot + clear first so events fired below cannot re-enter
        // expandStacks() while the stacks are still pending.
        List<StackedTokenCard> stacks = new ArrayList<>(stackedTokens);
        stackedTokens.clear();
        final boolean oldTrigger = trigger;
        trigger = false; // promote() already carried over the prototype's entry state // doc:1g DONE
        // REFORGE COMMANDER EXTENSION
        // Each add(copy) below fires updateZoneForView → the view re-reads the whole
        // battlefield (O(K²) for K copies). Suppress per-copy refreshes like the
        // TokenEffectBase burst pattern and push one refresh at the end; zone-change
        // events still fire either way.
        final boolean viewWasSuppressed = setSuppressViewUpdate(true);
        try {
            for (StackedTokenCard stack : stacks) {
                if (stack.isEmpty()) continue;
                // Route promoted copies through Zone.add() so zone-change events
                // fire; single final view refresh below replaces the per-copy ones.
                for (Card copy : stack.promoteAll()) {
                    add(copy);
                }
            }
        } finally {
            trigger = oldTrigger;
            setSuppressViewUpdate(viewWasSuppressed);
            if (!viewWasSuppressed) {
                getPlayer().updateZoneForView(this);
            }
        }
    }

    /**
     * Expands stacks before returning cards to ensure the engine sees distinct references.
     * Without this, mass-removal and trigger systems would malfunction on stacked tokens.
     */
    @Override
    public final CardCollectionView getCards(final boolean filter) {
        expandStacks();
        return super.getCards(filter);
    }

    @Override
    public java.util.Iterator<Card> iterator() {
        expandStacks();
        return super.iterator();
    }

    /** All battlefield cards WITHOUT expanding stacked tokens: one prototype Card per stack, not N cards. */
    // ponytail: O(S) where S = number of stacks, not N = total token count.
    // Melded cards excluded (same as getCards). Upgrade if melded cards gain static abilities needing evaluation.
    // doc:1c DONE
    public final CardCollectionView getCardsUnexpanded() {
        if (stackedTokens.isEmpty()) {
            return cardList;
        }
        CardCollection result = new CardCollection(cardList);
        for (StackedTokenCard stack : stackedTokens) {
            if (!stack.isEmpty()) {
                result.add(stack.getPrototype());
            }
        }
        return result;
    }

    public final List<StackedTokenCard> getStackedTokens() {
        return stackedTokens;
    }

    public final CardCollection getMeldedCards() {
        return meldedCards;
    }
}
