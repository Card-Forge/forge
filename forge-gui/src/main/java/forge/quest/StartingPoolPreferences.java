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

/**
 * This class is used to store the Quest starting pool preferences.
 * (It could be expanded to store other Quest starting preferences as well,
 * in order to reduce the number of parameters that need to be passed to
 * QuestController.newGame from CSubmenuQuestData)
 *
 */
public final class StartingPoolPreferences {

    private final boolean randomPool;
    private final byte preferredColor;
    private final boolean completeSet;
    private final boolean allowDuplicates;

    /**
     * Creates a new StartingPoolPreferences instance.
     * @param random If true, a completely random pool will be generated without filter restrictions. This does not
     *               bypass rarity restrictions.
     * @param preference Preferred color or colorless. All colors == no preference. See {@link forge.card.MagicColor}.
     * @param completeSet If true, four of each card in the starting pool will be generated.
     * @param allowDuplicates If true, multiples of each card will be allowed to be generated.
     */
    public StartingPoolPreferences(final boolean random, final byte preference, final boolean completeSet, final boolean allowDuplicates) {
        randomPool = random;
        preferredColor = preference;
        this.completeSet = completeSet;
        this.allowDuplicates = allowDuplicates;
    }

    /**
     * Is the starting pool completely random?
     * @return boolean, true if the starting pool is completely random (except for rarity)
     */
    public boolean useRandomPool() {
        return randomPool;
    }

    /**
     * Get the preferred starting pool color.
     * Return ALL_COLORS if no preference set.
     * @return MagicColor
     */
    public byte getPreferredColor() {
        return preferredColor;
    }

    public boolean grantCompleteSet() {
        return completeSet;
    }

    public boolean allowDuplicates() {
        return allowDuplicates;
    }

}
