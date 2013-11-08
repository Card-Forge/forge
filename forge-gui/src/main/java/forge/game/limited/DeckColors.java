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

import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.mana.ManaCost;
import forge.item.IPaperCard;

/**
 * Created by IntelliJ IDEA. User: dhudson Date: 6/24/11 Time: 8:42 PM To change
 * this template use File | Settings | File Templates.
 */
class DeckColors {

    private ColorSet chosen;
    private int colorMask;
    public final static int MAX_COLORS = 2;
    // public String Splash = "none";

    public ColorSet getChosenColors() {
        if ( null == chosen )
            chosen = ColorSet.fromMask(colorMask);
        return chosen; 
    }

    /**
     * TODO: Write javadoc for this method.
     * @param pickedCard
     */
    public void addColorsOf(IPaperCard pickedCard) {
        
        ManaCost colorsInCard = pickedCard.getRules().getManaCost();

        int colorsCanAdd = MagicColor.ALL_COLORS & ~getChosenColors().getColor();
        int colorsWantAdd = colorsInCard.getColorProfile() & colorsCanAdd;
        ColorSet toAdd = ColorSet.fromMask(colorsWantAdd);

        int cntColorsAssigned = getChosenColors().countColors();
        boolean haveSpace = cntColorsAssigned < MAX_COLORS;
        if( !haveSpace || toAdd.isColorless() )
            return;

        for(int i = 0; i < MagicColor.NUMBER_OR_COLORS && cntColorsAssigned < MAX_COLORS; i++ )
        if (( colorsWantAdd & MagicColor.WHITE << i ) > 0) {
            colorMask |= MagicColor.WHITE << i;
            chosen = null; // invalidate color set
            cntColorsAssigned++;
        }
    }


    public boolean canChoseMoreColors() {
        return getChosenColors().countColors() < MAX_COLORS;
    }

}
