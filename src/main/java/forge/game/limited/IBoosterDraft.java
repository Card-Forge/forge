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
package forge.game.limited;

import forge.deck.Deck;
import forge.item.CardPrinted;
import forge.item.ItemPoolView;

/**
 * <p>
 * BoosterDraft interface.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public interface IBoosterDraft {
    /**
     * <p>
     * nextChoice.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    ItemPoolView<CardPrinted> nextChoice();

    /**
     * <p>
     * setChoice.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    void setChoice(CardPrinted c);

    /**
     * <p>
     * hasNextChoice.
     * </p>
     * 
     * @return a boolean.
     */
    boolean hasNextChoice();

    /**
     * <p>
     * getDecks.
     * </p>
     * 
     * @return an array of {@link forge.deck.Deck} objects.
     */
    Deck[] getDecks(); // size 7, all the computers decks

    /** Constant <code>LandSetCode="{}"</code>. */
    String[] LAND_SET_CODE = { "" };

    /**
     * Called when drafting is over - to upload picks.
     */
    void finishedDrafting();

}
