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

import forge.quest.QuestMode;
import forge.quest.data.QuestAssets;

/**
 * This item has special coding.
 * 
 * @author Forge
 * @version $Id: QuestItemElixir.java 13728 2012-02-01 11:13:34Z moomarc $
 */
public class QuestItemPoundFlesh extends QuestItemBasic {

    /**
     * <p>
     * Constructor for QuestItemElixir.
     * </p>
     */
    QuestItemPoundFlesh() {
        super(QuestItemType.POUND_FLESH); // QuestStallManager.ALCHEMIST,
    }

    /** {@inheritDoc} */
    @Override
    public final String getPurchaseDescription(QuestAssets qA) {
        return String.format(super.getPurchaseDescription(qA), getSellingPrice(qA));
    }

    /** {@inheritDoc} */
    @Override
    public final int getBuyingPrice(QuestAssets qA) {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public final int getSellingPrice(QuestAssets qA) {
        int level = qA.getItemLevel(this.getItemType());
        if (qA.getLife(QuestMode.Fantasy) < 2) {
            return 0;
        } else if (level < 5) {
            return this.getBasePrice();
        } else if (level < 10) {
            return this.getBasePrice() * 2;
        } else {
            return this.getBasePrice() * 3;
        }
    }
}
