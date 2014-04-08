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
package forge.gui;

import javax.swing.*;

import forge.match.MatchConstants;

@SuppressWarnings("serial")
public abstract class ForgeAction extends AbstractAction {
    public ForgeAction(MatchConstants property) {
        super(property.button);
        this.putValue("buttonText", property.button);
        this.putValue("menuText", property.menu);
    }

    public <T extends AbstractButton> T setupButton(final T button) {
        button.setAction(this);
        button.setText((String) this.getValue(button instanceof JMenuItem ? "menuText" : "buttonText"));
        return button;
    }
}
