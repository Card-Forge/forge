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
public enum Counters {

    /** The AGE. */
    AGE(),

    /** The ARROW. */
    ARROW(),

    /** The ARROWHEAD. */
    ARROWHEAD(),

    /** The AWAKENING. */
    AWAKENING(),

    /** The BLAZE. */
    BLAZE(),

    /** The BLOOD. */
    BLOOD(),

    /** The BOUNTY. */
    BOUNTY(),

    /** The BRIBERY. */
    BRIBERY(),

    /** The CARRION. */
    CARRION(),

    /** The CHARGE. */
    CHARGE(),

    /** The CORPSE. */
    CORPSE(),

    /** The CREDIT. */
    CREDIT(),

    /** The CUBE. */
    CUBE(),

    /** The CURRENCY. */
    CURRENCY(),

    /** The DEATH. */
    DEATH(),

    /** The DELAY. */
    DELAY(),

    /** The DEPLETION. */
    DEPLETION(),

    /** The DESPAIR. */
    DESPAIR(),

    /** The DEVOTION. */
    DEVOTION(),

    /** The DIVINITY. */
    DIVINITY(),

    /** The DOOM. */
    DOOM(),

    /** The ELIXIR. */
    ELIXIR(),

    /** The ENERGY. */
    ENERGY(),

    /** The EON. */
    EON(),

    /** The EYEBALL. */
    EYEBALL(),

    /** The FADE. */
    FADE(),

    /** The FATE. */
    FATE(),

    /** The FEATHER. */
    FEATHER(),

    /** The FLOOD. */
    FLOOD(),

    /** The FUNGUS. */
    FUNGUS(),

    /** The FUSE. */
    FUSE(),

    /** The GLYPH. */
    GLYPH(),

    /** The GOLD. */
    GOLD(),

    /** The GROWTH. */
    GROWTH(),

    /** The HATCHLING. */
    HATCHLING(),

    /** The HEALING. */
    HEALING(),

    /** The HOOFPRINT. */
    HOOFPRINT(),

    /** The HOURGLASS. */
    HOURGLASS(),

    /** The ICE. */
    ICE(),

    /** The INFECTION. */
    INFECTION(),

    /** The INTERVENTION. */
    INTERVENTION(),

    /** The JAVELIN. */
    JAVELIN(),

    /** The KI. */
    KI(),

    /** The LEVEL. */
    LEVEL("Level"),

    /** The LORE. */
    LORE(),

    /** The LOYALTY. */
    LOYALTY(),

    /** The LUCK. */
    LUCK(),

    /** The M0 m1. */
    M0M1("-0/-1"),

    /** The M0 m2. */
    M0M2("-0/-2"),

    /** The M1 m0. */
    M1M0("-1/-0"),

    /** The M1 m1. */
    M1M1("-1/-1"),

    /** The M2 m1. */
    M2M1("-2/-1"),

    /** The M2 m2. */
    M2M2("-2/-2"),

    /** The MANA. */
    MANA(),

    /** The MANNEQUIN. */
    MANNEQUIN(),

    /** The MATRIX. */
    MATRIX(),

    /** The MINE. */
    MINE(),

    /** The MINING. */
    MINING(),

    /** The MIRE. */
    MIRE(),

    /** The OMEN. */
    OMEN(),

    /** The ORE. */
    ORE(),

    /** The PAGE. */
    PAGE(),

    /** The PAIN. */
    PAIN(),

    /** The PARALYZATION. */
    PARALYZATION(),

    /** The PETAL. */
    PETAL(),

    /** The PETRIFICATION. */
    PETRIFICATION(),

    /** The PIN. */
    PIN(),

    /** The PLAGUE. */
    PLAGUE(),

    /** The PRESSURE. */
    PRESSURE(),

    /** The PHYLACTERY. */
    PHYLACTERY,

    /** The POLYP. */
    POLYP(),

    /** The PUPA. */
    PUPA(),

    /** The P0 p1. */
    P0P1("+0/+1"),

    /** The P1 p0. */
    P1P0("+1/+0"),

    /** The P1 p1. */
    P1P1("+1/+1"),

    /** The P1 p2. */
    P1P2("+1/+2"),

    /** The P2 p2. */
    P2P2("+2/+2"),

    /** The QUEST. */
    QUEST(),

    /** The SCREAM. */
    SCREAM(),

    /** The SHELL. */
    SHELL(),

    /** The SHIELD. */
    SHIELD(),

    /** The SHRED. */
    SHRED(),

    /** The SLEEP. */
    SLEEP(),

    /** The SLEIGHT. */
    SLEIGHT(),

    /** The SLIME. */
    SLIME(),

    /** The SOOT. */
    SOOT(),

    /** The SPORE. */
    SPORE(),

    /** The STORAGE. */
    STORAGE(),

    /** The STRIFE. */
    STRIFE(),

    /** The STUDY. */
    STUDY(),

    /** The THEFT. */
    THEFT(),

    /** The TIDE. */
    TIDE(),

    /** The TIME. */
    TIME(),

    /** The TOWER. */
    TOWER("tower"),

    /** The TRAINING. */
    TRAINING(),

    /** The TRAP. */
    TRAP(),

    /** The TREASURE. */
    TREASURE(),

    /** The VELOCITY. */
    VELOCITY(),

    /** The VERSE. */
    VERSE(),

    /** The VITALITY. */
    VITALITY(),

    /** The WAGE. */
    WAGE(),

    /** The WIND. */
    WIND(),

    /** The WISH. */
    WISH();

    private String name;

    /**
     * <p>
     * Constructor for Counters.
     * </p>
     */
    private Counters() {
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
    private Counters(final String nameIn) {
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
     * @return a {@link forge.Counters} object.
     */
    public static Counters getType(final String name) {
        final String replacedName = name.replace("/", "").replaceAll("\\+", "p").replaceAll("\\-", "m").toUpperCase();
        return Enum.valueOf(Counters.class, replacedName);
    }
}
