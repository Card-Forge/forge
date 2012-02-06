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
package forge.quest.data.item;

import javax.swing.ImageIcon;

import forge.quest.data.bazaar.QuestStallManager;
import forge.view.toolbox.FSkin;

/**
 * <p>
 * QuestItemMap class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class QuestItemMap extends QuestItemAbstract {
    /**
     * <p>
     * Constructor for QuestItemMap.
     * </p>
     */
    QuestItemMap() {
        super("Map", QuestStallManager.GEAR);
    }

    /** {@inheritDoc} */
    @Override
    public final String getPurchaseName() {
        return "Adventurer's Map";
    }

    /** {@inheritDoc} */
    @Override
    public final String getPurchaseDescription() {
        return "These ancient charts should facilitate navigation during your travels significantly.\n"
                + "\nEffect: Quest assignments become available more frequently.";
    }

    /** {@inheritDoc} */
    @Override
    public final ImageIcon getIcon() {
        return FSkin.getIcon(FSkin.QuestIcons.ICO_MAP);
    }

    /** {@inheritDoc} */
    @Override
    public final int getBuyingPrice() {
        return 2000;
    }

    /** {@inheritDoc} */
    @Override
    public final int getSellingPrice() {
        return 0;
    }
}
