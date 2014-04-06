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
package forge.itemmanager.views;

import forge.Forge.Graphics;
import forge.assets.FImage;
import forge.assets.FSkinImage;
import forge.item.IPaperCard;
import forge.utils.CardPreferences;

import com.badlogic.gdx.math.Vector2;

/**
 * Displays favorite icons
 */
public class StarRenderer extends ItemCellRenderer {
    @Override
    public void draw(Graphics g, Object value, Vector2 loc, float itemWidth, float itemHeight) {
        IPaperCard card;
        if (value instanceof IPaperCard) {
            card = (IPaperCard) value;
        }
        else {
            return;
        }

        FImage image;
        if (CardPreferences.getPrefs(card.getName()).getStarCount() == 0) {
            image = FSkinImage.STAR_OUTINE;
        }
        else { //TODO: consider supporting more than 1 star
            image = FSkinImage.STAR_FILLED;
        }

        float size = 15;
        g.drawImage(image, loc.x, loc.y, size, size);
    }
}
