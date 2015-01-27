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
package forge.game.card;

import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostParser;

/**
 * <p>
 * Card_Color class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class CardColor  {
    private final byte colorMask;
    public final byte getColorMask() {
        return colorMask;
    }

    private final boolean additional;
    public final boolean isAdditional() {
        return this.additional;
    }

    private final long timestamp;
    public final long getTimestamp() {
        return this.timestamp;
    }

    CardColor(final String colors, final boolean addToColors, final long timestamp) {
        final ManaCost mc = new ManaCost(new ManaCostParser(colors));
        this.colorMask = mc.getColorProfile();
        this.additional = addToColors;
        this.timestamp = timestamp;
    }
}
