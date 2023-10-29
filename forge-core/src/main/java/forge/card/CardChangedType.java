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

import java.util.Set;

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
    private final Set<RemoveType> remove;

    public CardChangedType(final CardType addType0, final CardType removeType0, final boolean addAllCreatureTypes0,
            final Set<RemoveType> remove0) {
        addType = addType0;
        removeType = removeType0;
        addAllCreatureTypes = addAllCreatureTypes0;
        remove = remove0;
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
        return remove.contains(RemoveType.SuperTypes);
    }

    public final boolean isRemoveCardTypes() {
        return remove.contains(RemoveType.CardTypes);
    }

    public final boolean isRemoveSubTypes() {
        return remove.contains(RemoveType.SubTypes);
    }

    public final boolean isRemoveLandTypes() {
        return remove.contains(RemoveType.LandTypes);
    }

    public final boolean isRemoveCreatureTypes() {
        return remove.contains(RemoveType.CreatureTypes);
    }

    public final boolean isRemoveArtifactTypes() {
        return remove.contains(RemoveType.ArtifactTypes);
    }

    public final boolean isRemoveEnchantmentTypes() {
        return remove.contains(RemoveType.EnchantmentTypes);
    }
}
