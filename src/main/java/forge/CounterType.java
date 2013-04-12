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

package forge;

/**
 * The class Counters.
 * 
 * @author Clemens Koza
 * @version V0.0 17.02.2010
 */
public enum CounterType {

    M1M1("-1/-1"),
    P1P1("+1/+1"),
    LOYALTY,

    AGE(),

    ARROW(),

    ARROWHEAD(),

    AWAKENING(),

    BLAZE(),

    BLOOD(),

    BOUNTY(),

    BRIBERY(),

    CARRION(),

    CHARGE(),

    CORPSE(),

    CREDIT(),

    CUBE(),

    CURRENCY(),

    DEATH(),

    DELAY(),

    DEPLETION(),

    DESPAIR(),

    DEVOTION(),

    DIVINITY(),

    DOOM(),

    ELIXIR(),

    ENERGY(),

    EON(),

    EYEBALL(),

    FADE(),

    FATE(),

    FEATHER(),

    FILIBUSTER(),

    FLOOD(),

    FUNGUS(),

    FUSE(),

    GLYPH(),

    GOLD(),

    GROWTH(),

    HATCHLING(),

    HEALING(),

    HOOFPRINT(),

    HOURGLASS(),

    ICE(),

    INFECTION(),

    INTERVENTION(),

    JAVELIN(),

    KI(),

    LEVEL("Level"),

    LORE(),

    LUCK(),

    M0M1("-0/-1"),

    M0M2("-0/-2"),

    M1M0("-1/-0"),

    M2M1("-2/-1"),

    M2M2("-2/-2"),

    MANA(),

    MANNEQUIN(),

    MATRIX(),

    MINE(),

    MINING(),

    MIRE(),

    MUSIC(),

    MUSTER(),

    OMEN(),

    ORE(),

    PAGE(),

    PAIN(),

    PARALYZATION(),

    PETAL(),

    PETRIFICATION(),

    PIN(),

    PLAGUE(),

    PRESSURE(),

    PHYLACTERY,

    POLYP(),

    PUPA(),

    P0P1("+0/+1"),

    P1P0("+1/+0"),

    P1P2("+1/+2"),

    P2P2("+2/+2"),

    QUEST(),

    SCREAM(),

    SCROLL(),

    SHELL(),

    SHIELD(),

    SHRED(),

    SLEEP(),

    SLEIGHT(),

    SLIME(),

    SOOT(),

    SPORE(),

    STORAGE(),

    STRIFE(),

    STUDY(),

    THEFT(),

    TIDE(),

    TIME(),

    TOWER("tower"),

    TRAINING(),

    TRAP(),

    TREASURE(),

    VELOCITY(),

    VERSE(),

    VITALITY(),

    WAGE(),

    WINCH(),

    WIND(),

    WISH();

    private String name;

    /**
     * <p>
     * Constructor for Counters.
     * </p>
     */
    private CounterType() {
        this.name = this.name().substring(0, 1).toUpperCase() + this.name().substring(1).toLowerCase();
    }

    /**
     * <p>
     * Constructor for Counters.
     * </p>
     * 
     * @param name
     *            a {@link java.lang.String} object.
     */
    private CounterType(final String nameIn) {
        this.name = nameIn;
    }

    /**
     * <p>
     * Getter for the field <code>name</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public String getName() {
        return this.name;
    }

    /**
     * <p>
     * getType.
     * </p>
     * 
     * @param name
     *            a {@link java.lang.String} object.
     * @return a {@link forge.CounterType} object.
     */
    public static CounterType getType(final String name) {
        final String replacedName = name.replace("/", "").replaceAll("\\+", "p").replaceAll("\\-", "m").toUpperCase();
        return Enum.valueOf(CounterType.class, replacedName);
    }
}
