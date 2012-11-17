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

import java.util.Map;
import java.util.TreeMap;

import com.google.common.base.Function;

import forge.util.StorageView;

public final class EditionCollection extends StorageView<CardEdition> {

    private final Map<String, CardEdition> aliasToEdition = new TreeMap<String, CardEdition>();

    public EditionCollection() {
        super(new CardEdition.Reader("res/blockdata/setdata.txt"));

        for (CardEdition ee : this) {

            String alias = ee.getAlias();
            if (null != alias) {
                aliasToEdition.put(alias, ee);
            }
        }
    }

    /**
     * Gets the sets the by code.
     * 
     * @param code
     *            the code
     * @return the sets the by code
     */
    @Override
    public CardEdition get(final String code) {
        CardEdition baseResult = super.get(code);
        return baseResult == null ? aliasToEdition.get(code) : baseResult;
    }

    /**
     * Gets the sets the by code or throw.
     * 
     * @param code
     *            the code
     * @return the sets the by code or throw
     */
    public CardEdition getEditionByCodeOrThrow(final String code) {
        final CardEdition set = this.get(code);
        if (null == set) {
            throw new RuntimeException(String.format("Edition with code '%s' not found", code));
        }
        return set;
    }

    // used by image generating code
    /**
     * Gets the code2 by code.
     * 
     * @param code
     *            the code
     * @return the code2 by code
     */
    public String getCode2ByCode(final String code) {
        final CardEdition set = this.get(code);
        return set == null ? "" : set.getCode2();
    }

    public final Function<String, CardEdition> FN_EDITION_BY_CODE = new Function<String, CardEdition>() {
        @Override
        public CardEdition apply(String code) {
            return EditionCollection.this.get(code);
        };
    };
}

