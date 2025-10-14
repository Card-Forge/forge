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
package forge.item;

import forge.util.ITranslatable;

/**
 * Interface to define a player's inventory may hold. Should include
 * CardPrinted, Booster, Pets, Plants... etc
 */
public interface InventoryItem extends ITranslatable {
    String getItemType();
    String getImageKey(boolean altState);

    /**
     * Supplies the user-facing name of this item. Usually the same as `getName()`, but may be overwritten in cases such
     * as flavor names for cards.
     */
    default String getDisplayName() {
        return getName();
    }

    /**
     * @return true if this item's display name is different from its actual name. False otherwise.
     */
    default boolean hasFlavorName() {
        return false;
    }

    @Override
    default String getUntranslatedType() {
        return getItemType();
    }
}
