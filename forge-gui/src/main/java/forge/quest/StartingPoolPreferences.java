/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Nate
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
package forge.quest;

import java.util.List;

/**
 * This class is used to store the Quest starting pool preferences.
 * (It could be expanded to store other Quest starting preferences as well,
 * in order to reduce the number of parameters that need to be passed to
 * QuestController.newGame from CSubmenuQuestData)
 *
 */
public final class StartingPoolPreferences {

    public enum PoolType {
        /** Cards of the selected colors will be given equal odds during generation. */
        BALANCED,
        /** Anything goes. Selected colors are ignored and what goes in the pool is almost entirely random. */
        RANDOM,
        /** Same as BALANCED, except this picks colors for you without telling you what they are. */
        RANDOM_BALANCED
    }

    private final PoolType poolType;
    private final List<Byte> preferredColors;
    private final boolean includeArtifacts;
    private final boolean completeSet;
    private final boolean allowDuplicates;

    /**
     * Creates a new StartingPoolPreferences instance.
     * @param poolType The type of card pool to generate.
     * @param preferredColors A list of preferred colors to use when generating the card pool.
     *                        See {@link forge.card.MagicColor} for allowed values.
     * @param includeArtifacts If true, artifacts will be included in the pool regardless of selected colors. This
     *                         mimics the old quest pool generation.
     * @param completeSet If true, four of each card in the starting pool will be generated.
     * @param allowDuplicates If true, multiples of each card will be allowed to be generated.
     */
    public StartingPoolPreferences(final PoolType poolType, final List<Byte> preferredColors, final boolean includeArtifacts, final boolean completeSet, final boolean allowDuplicates) {
        this.poolType = poolType;
        this.preferredColors = preferredColors;
        this.includeArtifacts = includeArtifacts;
        this.completeSet = completeSet;
        this.allowDuplicates = allowDuplicates;
    }

    /**
     * @return The PoolType to use for this starting pool.
     */
    public PoolType getPoolType() {
        return poolType;
    }

    /**
     * @return A list of colors to use when generating the card pool. See {@link forge.card.MagicColor} for allowed values.
     */
    public List<Byte> getPreferredColors() {
        return preferredColors;
    }

    public boolean includeArtifacts() {
        return includeArtifacts;
    }

    public boolean grantCompleteSet() {
        return completeSet;
    }

    public boolean allowDuplicates() {
        return allowDuplicates;
    }

}
