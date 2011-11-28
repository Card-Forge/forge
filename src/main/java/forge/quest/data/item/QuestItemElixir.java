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

import forge.AllZone;
import forge.quest.data.bazaar.QuestStallManager;

/**
 * This item has special coding.
 * 
 * @author Forge
 * @version $Id$
 */
public class QuestItemElixir extends QuestItemAbstract {

    /**
     * <p>
     * Constructor for QuestItemElixir.
     * </p>
     */
    QuestItemElixir() {
        super("Elixir of Life", QuestStallManager.ALCHEMIST, 15);
    }

    /** {@inheritDoc} */
    @Override
    public final String getPurchaseDescription() {
        return "Gives +1 to maximum life<br>Current Life: " + AllZone.getQuestData().getLife();
    }

    /** {@inheritDoc} */
    @Override
    public final String getImageName() {
        return "ElixirIcon.png";
    }

    /** {@inheritDoc} */
    @Override
    public final int getPrice() {
        if (this.getLevel() < 5) {
            return 250;
        } else if (this.getLevel() < 10) {
            return 500;
        } else {
            return 750;
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void onPurchase() {
        AllZone.getQuestData().addLife(1);
    }

}
