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

import org.apache.commons.lang.NullArgumentException;

import com.google.common.base.Function;

public class PackData {
    private final String edition;
    private final String landEdition;
    private final int    cntLands;
    
    public final String getEdition() {
        return edition;
    }

    public final String getLandEdition() {
        return landEdition == null ? edition : landEdition;
    }

    public int getCntLands() {
        return cntLands;
    }
    
    public PackData(String edition0, String landEdition0, int nBasicLands)
    {
        if (null == edition0) { throw new NullArgumentException("edition0"); }
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
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        PackData other = (PackData)obj;
        return edition.equals(other.edition);
    }

    @Override
    public int hashCode() {
        return edition.hashCode();
    }
}
