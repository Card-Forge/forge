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

import forge.AllZone;
import forge.gui.toolbox.FSkin;
import forge.quest.data.bazaar.QuestStallManager;

/**
 * This item has special coding.
 * 
 * @author Forge
 * @version $Id: QuestItemElixir.java 13728 2012-02-01 11:13:34Z moomarc $
 */
public class QuestItemPoundFlesh extends QuestItemAbstract {

    /**
     * <p>
     * Constructor for QuestItemElixir.
     * </p>
     */
    QuestItemPoundFlesh() {
        super("Pound of Flesh", QuestStallManager.ALCHEMIST, 29);
    }

    /** {@inheritDoc} */
    @Override
    public final String getPurchaseDescription() {
        return "The Alchemist welcomes contributions to his famous Elixer.\n"
                + "But beware, you may build an immunity to its effects...\n"
                + "\nEffect: Alchemist gives you " + getSellingPrice() + " credits."
                + "\nEffect: Reduces maximum life by 1.";
    }

    /** {@inheritDoc} */
    @Override
    public final ImageIcon getIcon() {
        return FSkin.getIcon(FSkin.QuestIcons.ICO_BREW);
    }

    /** {@inheritDoc} */
    @Override
    public final int getBuyingPrice() {
        return 0;
    }

    /** {@inheritDoc} */
    public final int getSellingPrice() {
        if (AllZone.getQuestData().getLife() < 2) {
            return 0;
        } else if (this.getLevel() < 5) {
            return 250;
        } else if (this.getLevel() < 10) {
            return 500;
        } else {
            return 750;
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void onPurchase()  {
        if (AllZone.getQuestData().getLife() > 1) {
            super.onPurchase();
            AllZone.getQuestData().removeLife(1);
        }
    }
}
