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

public class PackData {
    private final String edition;
    public final String getEdition() {
        return edition;
    }

    private final String landEdition;
    public final String getLandEdition() {
        return landEdition == null ? edition : landEdition;
    }

    private final int cntLands;
    public int getCntLands() {
        return cntLands;
    }

    public PackData(String edition0, String landEdition0, int nBasicLands)
    {
        edition = edition0;
        landEdition = landEdition0;
        cntLands = nBasicLands;
    }

    public static final Function<? super PackData, String> FN_GET_CODE = new Function<PackData, String>() {
        @Override
        public String apply(PackData arg1) {
            return arg1.edition;
        }
    };
}
