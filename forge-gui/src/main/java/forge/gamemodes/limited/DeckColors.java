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
package forge.gamemodes.limited;

import java.util.List;

import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.item.IPaperCard;

public class DeckColors {

    protected ColorSet chosen;
    protected int colorMask;

    public int MAX_COLORS = 2;

    public ColorSet getChosenColors() {
        if (null == chosen) {
            chosen = ColorSet.fromMask(colorMask);
        }
        return chosen;
    }

    public void addColorsOf(final IPaperCard pickedCard) {
        final ColorSet colorsCanAdd = chosen.inverse();
        final ColorSet toAdd = colorsCanAdd.getSharedColors(pickedCard.getRules().getColor());

        int cntColorsAssigned = getChosenColors().countColors();
        final boolean haveSpace = cntColorsAssigned < MAX_COLORS;
        if (!haveSpace || toAdd.isColorless()) {
            return;
        }

        for (final byte color : MagicColor.WUBRG) {
            if (toAdd.hasAnyColor(color)) {
                colorMask |= color;
                chosen = null; // invalidate color set
                cntColorsAssigned++;
            }
            if (cntColorsAssigned >= MAX_COLORS) {
                break;
            }
        }
    }

    public void setColorsByList(final List<Byte> colors) {
        colorMask = 0;
        for (final Byte col : colors) {
            colorMask |= col.byteValue();
        }
        chosen = null;
    }

    public boolean canChoseMoreColors() {
        return getChosenColors().countColors() < MAX_COLORS;
    }

}
