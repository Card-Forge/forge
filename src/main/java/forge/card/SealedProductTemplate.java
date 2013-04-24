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

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;

public class SealedProductTemplate {

    protected final List<Pair<String, Integer>> slots;


    public String getEdition() {
        return null;
    }

    public final List<Pair<String, Integer>> getSlots() {
        return slots;
    }

    public SealedProductTemplate(Iterable<Pair<String, Integer>> itrSlots)
    {
        slots = Lists.newArrayList(itrSlots);
    }

    @SuppressWarnings("unchecked")
    public SealedProductTemplate(int qty) {
        this(Lists.newArrayList(Pair.of("any", qty)));
    }

    public int getTotal() {
        int sum = 0;
        for(Pair<String, Integer> p : slots) {
            sum += p.getRight().intValue();
        }
        return sum;
    }
}
