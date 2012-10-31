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
package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.List;


import forge.Card;

import forge.card.abilityfactory.SpellEffect;
import forge.game.player.Player;
import forge.gui.GuiChoose;

/**
 * <p>
 * AbilityFactory_Reveal class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public abstract class RevealEffectBase extends SpellEffect {


    // *************************************************************************
    // ************************* Dig *******************************************
    // *************************************************************************

    /**
     * Gets the revealed list.
     *
     * @param player the player
     * @param valid the valid
     * @param max the max
     * @param anyNumber a boolean
     * @return the revealed list
     */
    public static List<Card> getRevealedList(final Player player, final List<Card> valid, final int max, boolean anyNumber) {
        final List<Card> chosen = new ArrayList<Card>();
        final int validamount = Math.min(valid.size(), max);
    
        if (anyNumber && player.isHuman() && validamount > 0) {
            final List<Card> selection = GuiChoose.getOrderChoices("Choose Which Cards to Reveal", "Revealed", -1, valid, null, null);
            for (final Object o : selection) {
                if (o != null && o instanceof Card) {
                    chosen.add((Card) o);
                }
            }
        } else {
            for (int i = 0; i < validamount; i++) {
                if (player.isHuman()) {
                    final Card o = GuiChoose.one("Choose card(s) to reveal", valid);
                    if (o != null) {
                        chosen.add(o);
                        valid.remove(o);
                    } else {
                        break;
                    }
                } else { // Computer
                    chosen.add(valid.get(0));
                    valid.remove(valid.get(0));
                }
            }
        }
        return chosen;
    }
}
