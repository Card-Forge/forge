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
package forge.gui.deckeditor.tables;

import javax.swing.table.DefaultTableCellRenderer;

import forge.Singletons;
import forge.card.EditionCollection;

/**
 * A wrapper to show explanatory tooltips for edition set abbreviations.
 */
@SuppressWarnings("serial")
public class SetCodeRenderer extends DefaultTableCellRenderer implements AlwaysShowToolTip {
    @Override
    public String getToolTipText() {
        String setAbbrev   = getText();
        String setFullName = "Unknown set";
        
        EditionCollection editions = Singletons.getModel().getEditions();
        
        if (null != setAbbrev && editions.contains(setAbbrev)) {
            setFullName = editions.get(setAbbrev).getName();
        }
        
        return String.format("%s (%s)", setFullName, setAbbrev);
    }
}
