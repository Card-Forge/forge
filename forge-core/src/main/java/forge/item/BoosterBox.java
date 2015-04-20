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

package forge.item;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Function;

import forge.StaticData;
import forge.card.CardEdition;
import forge.util.TextUtil;
import forge.util.storage.StorageReaderFile;

public class BoosterBox extends BoxedProduct {
    public static final Function<CardEdition, BoosterBox> FN_FROM_SET = new Function<CardEdition, BoosterBox>() {
        @Override
        public BoosterBox apply(final CardEdition arg1) {
            BoosterBox.Template d = StaticData.instance().getBoosterBoxes().get(arg1.getCode());
            if (d == null) { return null; }
            return new BoosterBox(arg1.getName(), d, d.cntBoosters);
        }
    };

    private final BoosterBox.Template fpData;

    public BoosterBox(final String name0, final BoosterBox.Template fpData0, final int boosterCount) {
        super(name0, StaticData.instance().getBoosters().get(fpData0.getEdition()), boosterCount);
        fpData = fpData0;
    }

    @Override
    public String getDescription() {
        return fpData.toString() + contents.toString();
    }

    @Override
    public final String getItemType() {
        return "Booster Box";
    }

    @Override
    public final Object clone() {
        return new BoosterBox(name, fpData, fpData.cntBoosters);
    }

    @Override
    public int getTotalCards() {
        return super.getTotalCards() * fpData.getCntBoosters() + fpData.getNumberOfCardsExpected();
    }
    
    public static class Template extends SealedProduct.Template {
        private final int cntBoosters;


        public int getCntBoosters() { return cntBoosters; }

        private Template(String edition, int boosters, Iterable<Pair<String, Integer>> itrSlots)
        {
            super(edition, itrSlots);
            cntBoosters = boosters;
        }

        public static final class Reader extends StorageReaderFile<Template> {
            public Reader(String pathname) {
                super(pathname, FN_GET_NAME);
            }

            @Override
            protected Template read(String line, int i) {
                String[] headAndData = TextUtil.split(line, ':', 2);
                final String edition = headAndData[0];
                final String[] data = TextUtil.splitWithParenthesis(headAndData[1], ',');
                int nBoosters = 6;

                List<Pair<String, Integer>> slots = new ArrayList<Pair<String,Integer>>();
                for(String slotDesc : data) {
                    String[] kv = TextUtil.split(slotDesc, ' ', 2);
                    if (kv[1].startsWith("Booster"))
                        nBoosters = Integer.parseInt(kv[0]);
                    else
                        slots.add(ImmutablePair.of(kv[1], Integer.parseInt(kv[0])));
                }

                return new BoosterBox.Template(edition, nBoosters, slots);
            }
        }
        
        @Override
        public String toString() {
            if (0 >= cntBoosters) {
                return "no cards";
            }

            StringBuilder s = new StringBuilder();
            for(Pair<String, Integer> p : slots) {
                s.append(p.getRight()).append(" ").append(p.getLeft()).append(", ");
            }
            // trim the last comma and space
            if( s.length() > 0 )
                s.replace(s.length() - 2, s.length(), "");

            if (0 < cntBoosters) {
                if( s.length() > 0 )
                    s.append(" and ");
                    
                s.append(cntBoosters).append(" booster packs ");
            }
            return s.toString();
        }
    }    
}
