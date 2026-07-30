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
    public final boolean tryStackToken(Card c) {
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
        for (StackedTokenCard stack : stackedTokens) {
            if (stack.isEmpty()) continue;
            // promoteAll returns individual copies; add them to cardList
            cardList.addAll(stack.promoteAll());
        }
        stackedTokens.clear();
    }

    /**
     * Compresses groups of identical tokens into StackedTokenCard entries.
     * Run after batch token creation to defer Card object allocation.
     *
     * expandStacks() must be called before any operation that iterates cards
     * and expects distinct Card references (targeting, destruction, events).
     */
    public final void compressTokens() {
        CardCollection toRemove = new CardCollection();
        for (Card c : cardList) {
            if (c.getGamePieceType() != GamePieceType.TOKEN || c.isPhasedOut()) {
                continue;
            }
            boolean merged = false;
            for (StackedTokenCard stack : stackedTokens) {
                if (stack.canMerge(c)) {
                    stack.addQuantity(1);
                    toRemove.add(c);
                    merged = true;
                    break;
                }
            }
            if (!merged) {
                stackedTokens.add(new StackedTokenCard(c, 1));
                toRemove.add(c);
            }
        }
        cardList.removeAll(toRemove);
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

    public final List<StackedTokenCard> getStackedTokens() {
        return stackedTokens;
    }

    public final CardCollection getMeldedCards() {
        return meldedCards;
    }
}
