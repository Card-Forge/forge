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

import forge.card.CardEdition;
import forge.model.FModel;
import forge.util.TextUtil;

/**
 * A wrapper to show explanatory tooltips for edition set abbreviations.
 */
@SuppressWarnings("serial")
public class SetCodeRenderer extends ItemCellRenderer {
    @Override
    public boolean alwaysShowTooltip() {
        return true;
    }

    @Override
    public String getToolTipText() {
        String setAbbrev   = getText();
        String setFullName = "Unknown set";
        
        CardEdition.Collection editions = FModel.getMagicDb().getEditions();
        
        if (null != setAbbrev && editions.contains(setAbbrev)) {
            setFullName = editions.get(setAbbrev).getName();
        }
        
        return TextUtil.concatWithSpace(setFullName, TextUtil.enclosedParen(setAbbrev));
    }
}
