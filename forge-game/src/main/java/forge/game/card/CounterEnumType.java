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

    M1M1("-1/-1", "-1/-1", 255, 110, 106, CounterAiCategory.Negative),
    P1P1("+1/+1", "+1/+1", 96, 226, 23, CounterAiCategory.Positive),

    LOYALTY("LOYAL", 198, 198, 198, CounterAiCategory.Positive),

    AGE("AGE", 255, 137, 57, CounterAiCategory.Negative),

    CHARGE("CHARG", 246, 192, 0, CounterAiCategory.Positive),

    DEFENSE("DEF", 164, 23, 32, CounterAiCategory.Positive),

    DREAM("DREAM", 190, 189, 255, CounterAiCategory.Positive),

    FADE("FADE", 159, 209, 192, CounterAiCategory.Positive),

    FINALITY("FINAL", 255, 255, 255, CounterAiCategory.Negative),

    LEVEL("LEVEL", 60, 222, 185, CounterAiCategory.Positive),

    LORE("LORE", 209, 198, 161, CounterAiCategory.Positive),

    MANABOND("MANA", 0, 255, 0, CounterAiCategory.Positive),

    SHIELD("SHLD", 202, 198, 186, CounterAiCategory.Positive),

    STUN("STUN", 226, 192, 165, CounterAiCategory.Negative),

    TIME("TIME", 255, 121, 255, CounterAiCategory.Positive),

    M0M1("-0/-1", "-0/-1", 255, 110, 106, CounterAiCategory.Negative),

    M0M2("-0/-2", "-0/-2", 255, 110, 106, CounterAiCategory.Negative),

    M1M0("-1/-0", "-1/-0", 255, 110, 106, CounterAiCategory.Negative),

    M2M1("-2/-1", "-2/-1", 255, 110, 106, CounterAiCategory.Negative),

    M2M2("-2/-2", "-2/-2", 255, 110, 106, CounterAiCategory.Negative),

    P0P1("+0/+1", "+0/+1", 96, 226, 23, CounterAiCategory.Positive),

    P0P2("+0/+2", "+0/+2", 96, 226, 23, CounterAiCategory.Positive),

    P1P0("+1/+0", "+1/+0", 96, 226, 23, CounterAiCategory.Positive),

    P1P2("+1/+2", "+1/+2", 96, 226, 23, CounterAiCategory.Positive),

    P2P0("+2/+0", "+2/+0", 96, 226, 23, CounterAiCategory.Positive),

    P2P2("+2/+2", "+2/+2", 96, 226, 23, CounterAiCategory.Positive),

    // Player Counters

    ENERGY("ENRGY", CounterAiCategory.Positive),

    EXPERIENCE("EXP", CounterAiCategory.Positive),

    POISON("POISN", CounterAiCategory.Negative),

    RAD("RAD", CounterAiCategory.Neutral),

    TICKET("TICKET", CounterAiCategory.Positive),

    ;

    private String name, counterOnCardDisplayName;
    private int red, green, blue;
    private CounterAiCategory aiCategory;

    CounterEnumType(final String counterOnCardDisplayName, CounterAiCategory aiCategory) {
        this(counterOnCardDisplayName, 255, 255, 255, aiCategory);
    }

    CounterEnumType(final String counterOnCardDisplayName, final int red, final int green, final int blue, CounterAiCategory aiCategory) {
        this.name = this.name().substring(0, 1).toUpperCase() + this.name().substring(1).toLowerCase();
        this.counterOnCardDisplayName = counterOnCardDisplayName;
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.aiCategory = aiCategory;
    }

    CounterEnumType(final String name, final String counterOnCardDisplayName, final int red, final int green, final int blue, CounterAiCategory aiCategory) {
        this(counterOnCardDisplayName, red, green, blue, aiCategory);
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public int getRed() {
        return red;
    }

    @Override
    public int getGreen() {
        return green;
    }

    @Override
    public int getBlue() {
        return blue;
    }

    @Override
    public CounterAiCategory getAiCategory() {
        return aiCategory;
    }

    @Override
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
}
