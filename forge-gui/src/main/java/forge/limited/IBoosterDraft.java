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
package forge.limited;

import forge.card.CardEdition;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.item.PaperCard;

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
     * @return a {@link CardPool} object.
     */
    CardPool nextChoice();

    /**
     * <p>
     * setChoice.
     * </p>
     *
     * @param c
     *            a {@link forge.game.card.Card} object.
     */
    void setChoice(PaperCard c);

    /**
     * <p>
     * hasNextChoice.
     * </p>
     *
     * @return a boolean.
     */
    boolean hasNextChoice();
    boolean isRoundOver();

    /**
     * <p>
     * getDecks.
     * </p>
     *
     * @return an array of {@link forge.deck.Deck} objects.
     */
    Deck[] getDecks(); // size 7, all the computers decks

    /** Constant <code>LandSetCode="{}"</code>. */
    CardEdition[] LAND_SET_CODE = { null };

    String[] CUSTOM_RANKINGS_FILE = { null };

    boolean isPileDraft();

}
