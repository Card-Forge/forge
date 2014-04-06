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

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.badlogic.gdx.math.Vector2;

import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinColor.Colors;

/**
 * Base cell renderer class for item tables
 */
public class ItemCellRenderer {
    public static final FSkinFont FONT = FSkinFont.get(14);
    public static final FSkinColor FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT);

    public void draw(Graphics g, Object value, Vector2 loc, float itemWidth, float itemHeight) {
        String text = value.toString();
        g.drawText(text, FONT, FORE_COLOR, loc.x, loc.y, itemWidth, itemHeight, false, HAlignment.LEFT, false);
    }
}
