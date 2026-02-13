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

import com.google.common.collect.Lists;

import forge.card.CardType.CoreType;
import forge.util.IterableUtil;

public record CardChangedType(CardTypeView addType, CardTypeView removeType, boolean addAllCreatureTypes, Set<RemoveType> remove) implements ICardChangedType {

    public final boolean isRemoveSuperTypes() {
        return remove.contains(RemoveType.SuperTypes);
    }

    public final boolean isRemoveCardTypes() {
        return remove.contains(RemoveType.CardTypes);
    }

    public final boolean isRemoveSubTypes() {
        return remove.contains(RemoveType.SubTypes);
    }

    @Override
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

    @Override
    public CardType applyChanges(CardType newType) {
        if (isRemoveCardTypes()) {
            // 205.1a However, an object with either the instant or sorcery card type retains that type.
            newType.coreTypes.retainAll(CoreType.spellTypes);
        }
        if (isRemoveSuperTypes()) {
            newType.supertypes.clear();
        }
        if (isRemoveSubTypes()) {
            newType.subtypes.clear();
        } else if (!newType.subtypes.isEmpty()) {
            if (isRemoveLandTypes()) {
                newType.subtypes.removeIf(CardType::isALandType);
            }
            if (isRemoveCreatureTypes()) {
                newType.subtypes.removeIf(CardType::isACreatureType);
                // need to remove AllCreatureTypes too when removing creature Types
                newType.allCreatureTypes = false;
            }
            if (isRemoveArtifactTypes()) {
                newType.subtypes.removeIf(CardType::isAnArtifactType);
            }
            if (isRemoveEnchantmentTypes()) {
                newType.subtypes.removeIf(CardType::isAnEnchantmentType);
            }
        }
        if (removeType() != null) {
            newType.removeAll(removeType());
        }
        if (addType() != null) {
            newType.addAll(addType());
            if (addType().hasAllCreatureTypes()) {
                newType.allCreatureTypes = true;
            }
        }
        if (addAllCreatureTypes()) {
            newType.allCreatureTypes = true;
        }
        // remove specific creature types from all creature types
        if (removeType() != null && newType.allCreatureTypes) {
            newType.excludedCreatureSubtypes.addAll(Lists.newArrayList(IterableUtil.filter(removeType().getSubtypes(), CardType::isACreatureType)));
        }
        return newType;
    }
}
