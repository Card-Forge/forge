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
package forge.quest.data.bazaar;

/**
 * This interface defines a thing that can be sold at the Bazaar.
 * 
 * @author Forge
 * @version $Id$
 */
public interface QuestStallPurchasable extends Comparable<Object> {
    /**
     * <p>
     * getPurchaseName.
     * </p>
     * 
     * @return The Name of the item
     */
    String getPurchaseName();

    /**
     * <p>
     * getPurchaseDescription.
     * </p>
     * 
     * @return an HTML formatted item description
     */
    String getPurchaseDescription();

    /**
     * <p>
     * getImageName.
     * </p>
     * 
     * @return the name of the image that is displayed in the bazaar
     */
    String getImageName();

    /**
     * <p>
     * getPrice.
     * </p>
     * 
     * @return the cost of the item in credits
     */
    int getPrice();

    /**
     * Returns if the item is available for purchase;.
     * 
     * @return <code>true</code> if the item can be displayed in a store
     *         <code>false</code> if the item should not be displayed in store
     *         since, for example, prerequisites are not met
     */
    boolean isAvailableForPurchase();

    /**
     * Executed when the item is bought.
     */
    void onPurchase();

    /**
     * <p>
     * getStallName.
     * </p>
     * 
     * @return the name of the stall form which this item can be purchased
     */
    String getStallName();
}
