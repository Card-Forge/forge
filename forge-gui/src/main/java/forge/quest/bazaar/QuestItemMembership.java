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
package forge.quest.bazaar;

import forge.quest.data.QuestAssets;

/**
 * <p>
 * QuestItemEstates class.
 * </p>
 * 
 * @author Forge
 * @version $Id: QuestItemEstates.java 14797 2012-03-18 18:09:02Z Max mtg $
 */
public class QuestItemMembership extends QuestItemBasic {
    /**
     * <p>
     * Constructor for QuestItemEstates.
     * </p>
     */
    QuestItemMembership() {
        super(QuestItemType.MEMBERSHIP_TOKEN); // QuestStallManager.BANKER,
    }

    /** {@inheritDoc} */
    @Override
    public final String getPurchaseDescription(QuestAssets qA) {
        return super.getPurchaseDescription(qA);
    }

    /** {@inheritDoc} */
    @Override
    public final int getBuyingPrice(QuestAssets qA) {
        int level = qA.getItemLevel(this.getItemType());
        return (int) (getBasePrice() * (Math.pow(level + 3, 2)));
    }
}
