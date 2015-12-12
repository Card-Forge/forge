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

import com.google.common.base.Function;

public enum CardRarity {
    BasicLand("L", "Basic Land"),
    Common("C", "Common"),
    Uncommon("U", "Uncommon"),
    Rare("R", "Rare"),
    MythicRare("M", "Mythic Rare"),
    Special("S", "Special"), // Timeshifted
    Unknown("?", "Unknown"); // In development

    public static final CardRarity[] FILTER_OPTIONS = new CardRarity[] {
        Common, Uncommon, Rare, MythicRare, Special
    };

    private final String shortName, longName;

    private CardRarity(final String shortName0, final String longName0) {
        shortName = shortName0;
        longName = longName0;
    }

    @Override
    public String toString() {
        return shortName;
    }

    public static CardRarity smartValueOf(String input) {
        for (CardRarity r : CardRarity.values()) {
            if (r.name().equalsIgnoreCase(input) || r.shortName.equalsIgnoreCase(input) || r.longName.equalsIgnoreCase(input)) {
                return r;
            }
        }
        return Unknown;
    }

    public static final Function<CardRarity, String> FN_GET_LONG_NAME = new Function<CardRarity, String>() {
        @Override
        public String apply(final CardRarity rarity) {
            return rarity.longName;
        }
    };
}
