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
package forge.gui.deckeditor;

import forge.Command;
import forge.deck.DeckGroup;

/**
 * <p>
 * Gui_DeckEditor_Menu class.
 * </p>
 * 
 * @author Forge
 * @version $Id: DeckEditorCommonMenu.java 13590 2012-01-27 20:46:27Z Max mtg $
 */
public final class MenuLimited extends MenuBase<DeckGroup> {

    /** Constant <code>serialVersionUID=-4037993759604768755L</code>. */
    private static final long serialVersionUID = -4037993759604768755L;

    /**
     * 
     * Menu for Deck Editor.
     * 
     * @param inDisplay
     *            a DeckDisplay
     * @param dckManager
     *            a DeckManager
     * @param exit
     *            a Command
     */
    public MenuLimited(final IDeckController<DeckGroup> ctrl, final Command exit) {
        super( ctrl, exit );
    }

    // deck.setName(currentDeckName);

}
