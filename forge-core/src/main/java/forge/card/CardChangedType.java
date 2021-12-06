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
package forge.card;

/**
 * <p>
 * CardType class.
 * </p>
 * 
 * @author Forge
 * @version $Id: CardType.java 24393 2014-01-21 06:27:36Z Max mtg $
 */
public class CardChangedType {
    // takes care of individual card types
    private final CardType addType;
    private final CardType removeType;
    private final boolean addAllCreatureTypes;
    private final boolean removeSuperTypes;
    private final boolean removeCardTypes;
    private final boolean removeSubTypes;
    private final boolean removeLandTypes;
    private final boolean removeCreatureTypes;
    private final boolean removeArtifactTypes;
    private final boolean removeEnchantmentTypes;

    public CardChangedType(final CardType addType0, final CardType removeType0, final boolean addAllCreatureTypes0,
            final boolean removeSuperType0, final boolean removeCardType0, final boolean removeSubType0,
            final boolean removeLandType0, final boolean removeCreatureType0, final boolean removeArtifactType0, final boolean removeEnchantmentTypes0) {
        addType = addType0;
        removeType = removeType0;
        addAllCreatureTypes = addAllCreatureTypes0;
        removeSuperTypes = removeSuperType0;
        removeCardTypes = removeCardType0;
        removeSubTypes = removeSubType0;
        removeLandTypes = removeLandType0;
        removeCreatureTypes = removeCreatureType0;
        removeArtifactTypes = removeArtifactType0;
        removeEnchantmentTypes = removeEnchantmentTypes0;
    }

    public final CardType getAddType() {
        return addType;
    }

    public final CardType getRemoveType() {
        return removeType;
    }

    public final boolean isAddAllCreatureTypes() {
        return addAllCreatureTypes;
    }

    public final boolean isRemoveSuperTypes() {
        return removeSuperTypes;
    }

    public final boolean isRemoveCardTypes() {
        return removeCardTypes;
    }

    public final boolean isRemoveSubTypes() {
        return removeSubTypes;
    }

    public final boolean isRemoveLandTypes() {
        return removeLandTypes;
    }

    public final boolean isRemoveCreatureTypes() {
        return removeCreatureTypes;
    }

    public final boolean isRemoveArtifactTypes() {
        return removeArtifactTypes;
    }

    public final boolean isRemoveEnchantmentTypes() {
        return removeEnchantmentTypes;
    }
}
