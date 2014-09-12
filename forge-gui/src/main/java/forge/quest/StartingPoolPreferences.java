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

    /**
     * The constructor.
     * @param random
     *  true = use completely random pool without filter restrictions
     *  (Note that this does NOT bypass card rarity restrictions!)
     * @param preference
     *  preferred color/COLORLESS (ALL_COLORS = no preference)
     */
    public StartingPoolPreferences(final boolean random, final byte preference, final boolean completeSet) {
        randomPool = random;
        preferredColor = preference;
		this.completeSet = completeSet;
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
	
}
