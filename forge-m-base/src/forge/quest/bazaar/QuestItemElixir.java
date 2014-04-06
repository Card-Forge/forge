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
 * This item has special coding.
 * 
 * @author Forge
 * @version $Id$
 */
public class QuestItemElixir extends QuestItemBasic {

    /**
     * <p>
     * Constructor for QuestItemElixir.
     * </p>
     */
    QuestItemElixir() {
        super(QuestItemType.ELIXIR_OF_LIFE); // QuestStallManager.ALCHEMIST,
    }

    /** {@inheritDoc} */
    @Override
    public final int getBuyingPrice(QuestAssets qA) {
        int level = qA.getItemLevel(this.getItemType());
        if (level < 5) {
            return super.getBasePrice();
        } else if (level < 10) {
            return super.getBasePrice() * 2;
        } else if (level <= this.getMaxLevel()) {
            return super.getBasePrice() * 3;
        } else {
            return 0;
        }
    }
}
