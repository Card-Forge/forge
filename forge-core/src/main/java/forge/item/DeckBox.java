/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Nate
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
package forge.item;

import java.util.Map.Entry;

import com.google.common.base.Predicate;

import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;

/**
 * A deck box containing a deck
 * 
 */
public class DeckBox implements InventoryItem {
    private final Deck deck;

    @Override
    public String getName() {
        return this.deck.getName();
    }

    @Override
    public String getItemType() {
        return "Deck";
    }

    public DeckBox(final Deck deck0) {
        this.deck = deck0;
    }

    //create predicate that applys a card predicate to all cards in deck
    public static final Predicate<DeckBox> createPredicate(final Predicate<PaperCard> cardPredicate) {
        return new Predicate<DeckBox>() {
            @Override
            public boolean apply(DeckBox input) {
                for (Entry<DeckSection, CardPool> deckEntry : input.deck) {
                    switch (deckEntry.getKey()) {
                    case Main:
                    case Sideboard:
                    case Commander:
                        for (Entry<PaperCard, Integer> poolEntry : deckEntry.getValue()) {
                            if (!cardPredicate.apply(poolEntry.getKey())) {
                                return false; //all cards in deck must pass card predicate to pass deck predicate
                            }
                        }
                        break;
                    default:
                        break; //ignore other sections
                    }
                }
                return true;
            }
        };
    }
}
