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

import forge.quest.data.bazaar.QuestStallManager;

/**
 * <p>
 * QuestItemEstates class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class QuestItemEstates extends QuestItemAbstract {
    /**
     * <p>
     * Constructor for QuestItemEstates.
     * </p>
     */
    QuestItemEstates() {
        super("Estates", QuestStallManager.BANKER, 3);
    }

    /** {@inheritDoc} */
    @Override
    public final String getPurchaseDescription() {
        return String.format("Gives a bonus of <b>%d%%</b> to match winnings.<br>"
                + "Improves sell percentage by <b>%.2f%%</b>.", (10 + (this.getLevel() * 5)),
                (1 + (this.getLevel() * 0.75)));
    }

    /** {@inheritDoc} */
    @Override
    public final String getImageName() {
        return "GoldIconLarge.png";
    }

    /** {@inheritDoc} */
    @Override
    public final int getPrice() {
        if (this.getLevel() == 0) {
            return 500;
        } else if (this.getLevel() == 1) {
            return 750;
        } else {
            return 1000;
        }
    }

}
