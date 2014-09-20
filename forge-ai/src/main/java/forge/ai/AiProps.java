/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2013  Forge Team
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
package forge.ai;

/** 
 * AI personality profile settings identifiers, and their default values.
 * When this class is instantiated, these enum values are used
 * in a map that is populated with the current AI profile settings
 * from the text file.
 */
public enum AiProps { /** */
    DEFAULT_MAX_PLANAR_DIE_ROLLS_PER_TURN ("1"), /** */
    DEFAULT_MIN_TURN_TO_ROLL_PLANAR_DIE ("3"), /** */
    DEFAULT_PLANAR_DIE_ROLL_CHANCE ("50"), /** */
    MULLIGAN_THRESHOLD ("5"), /** */
    PLANAR_DIE_ROLL_HESITATION_CHANCE ("10"),
    CHEAT_WITH_MANA_ON_SHUFFLE ("false"),
    MOVE_EQUIPMENT_TO_BETTER_CREATURES ("from_useless_only"),
    PRIORITIZE_MOVE_EQUIPMENT_IF_USELESS ("true"),
    PREDICT_SPELLS_FOR_MAIN2 ("true"), /** */
    RESERVE_MANA_FOR_MAIN2_CHANCE ("0"); /** */

    private final String strDefaultVal;

    /** @param s0 &emsp; {@link java.lang.String} */
    AiProps(final String s0) {
        this.strDefaultVal = s0;
    }

    /** @return {@link java.lang.String} */
    public String getDefault() {
        return strDefaultVal;
    }
}

