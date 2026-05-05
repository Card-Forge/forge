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

package forge.game.card;

import java.util.Locale;

/**
 * The class Counters.
 */
public enum CounterEnumType implements CounterType {

    M1M1("-1/-1", "-1/-1", 255, 110, 106),
    P1P1("+1/+1", "+1/+1", 96, 226, 23),

    LOYALTY("LOYAL", 198, 198, 198),

    AGE("AGE", 255, 137, 57),

    AWAKENING("AWAKE", 0, 231, 79),

    BLAZE("BLAZE", 255, 124, 82),

    BRIBERY("BRIBE", 172, 201, 235),

    CHARGE("CHARG", 246, 192, 0),

    DEFENSE("DEF", 164, 23, 32),

    DEPLETION("DPLT", 185, 201, 208),

    DOOM("DOOM", 255, 104, 118),

    DREAM("DREAM", 190, 189, 255),

    FADE("FADE", 159, 209, 192),

    FINALITY("FINAL", 255, 255, 255),

    FLAVOR("FLAVOR", 208, 152, 97), ///adventure only

    GHOSTFORM("GHSTF", 223, 0, 254),

    GOLD("GOLD", 248, 191, 0),

    ICE("ICE", 0, 239, 255),

    INCARNATION("INCRN", 247, 206, 64),

    LEVEL("LEVEL", 60, 222, 185),

    LORE("LORE", 209, 198, 161),

    MANABOND("MANA", 0, 255, 0),

    M0M1("-0/-1", "-0/-1", 255, 110, 106),

    M0M2("-0/-2", "-0/-2", 255, 110, 106),

    M1M0("-1/-0", "-1/-0", 255, 110, 106),

    M2M1("-2/-1", "-2/-1", 255, 110, 106),

    M2M2("-2/-2", "-2/-2", 255, 110, 106),

    MANIFESTATION("MNFST", 104, 225, 8),

    MUSIC("MUSIC", 255, 138, 255),

    PARALYZATION("PRLYZ", 220, 201, 0),

    PETRIFICATION("PETRI", 185, 201, 208),

    PUPA("PUPA", 0, 223, 203),

    P0P1("+0/+1", "+0/+1", 96, 226, 23),

    P0P2("+0/+2", "+0/+2", 96, 226, 23),

    P1P0("+1/+0", "+1/+0", 96, 226, 23),

    P1P2("+1/+2", "+1/+2", 96, 226, 23),

    P2P0("+2/+0", "+2/+0", 96, 226, 23),

    P2P2("+2/+2", "+2/+2", 96, 226, 23),

    QUEST("QUEST", 251, 189, 0),

    RUST("RUST", 255, 181, 116),

    SHELL("SHELL", 190, 207, 111),

    SHIELD("SHLD", 202, 198, 186),

    SLEEP("SLEEP", 178, 192, 255),

    SLUMBER("SLMBR", 178, 205, 255),

    SLEIGHT("SLGHT", 185, 174, 255),

    STUN("STUN", 226, 192, 165),

    TIME("TIME", 255, 121, 255),

    TRAINING("TRAIN", 220, 201, 0),

    WAGE("WAGE", 242, 190, 106),

    // Player Counters

    ENERGY("ENRGY"),

    EXPERIENCE("EXP"),

    POISON("POISN"),

    RAD("RAD"),

    TICKET("TICKET"),

    ;

    private String name, counterOnCardDisplayName;
    private int red, green, blue;

    CounterEnumType(final String counterOnCardDisplayName) {
        this(counterOnCardDisplayName, 255, 255, 255);
    }

    CounterEnumType(final String counterOnCardDisplayName, final int red, final int green, final int blue) {
        this.name = this.name().substring(0, 1).toUpperCase() + this.name().substring(1).toLowerCase();
        this.counterOnCardDisplayName = counterOnCardDisplayName;
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    CounterEnumType(final String name, final String counterOnCardDisplayName, final int red, final int green, final int blue) {
        this(counterOnCardDisplayName, red, green, blue);
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public int getRed() {
        return red;
    }

    public int getGreen() {
        return green;
    }

    public int getBlue() {
        return blue;
    }

    public String getCounterOnCardDisplayName() {
        return counterOnCardDisplayName;
    }

    public static CounterEnumType getType(final String name) {
        final String replacedName = name.replace("/", "").replaceAll("\\+", "p").replaceAll("\\-", "m").toUpperCase(Locale.ROOT);
        return Enum.valueOf(CounterEnumType.class, replacedName);
    }

    @Override
    public boolean is(CounterEnumType eType) {
        return this == eType;
    }

    @Override
    public boolean isKeywordCounter() {
        return false;
    }
}
