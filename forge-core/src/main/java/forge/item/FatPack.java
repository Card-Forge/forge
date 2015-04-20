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
import forge.card.BoosterGenerator;
import forge.card.CardEdition;
import forge.util.TextUtil;
import forge.util.storage.StorageReaderFile;

public class FatPack extends BoxedProduct {
    public static final Function<CardEdition, FatPack> FN_FROM_SET = new Function<CardEdition, FatPack>() {
        @Override
        public FatPack apply(final CardEdition arg1) {
            FatPack.Template d = StaticData.instance().getFatPacks().get(arg1.getCode());
            if (d == null) { return null; }
            return new FatPack(arg1.getName(), d, d.cntBoosters);
        }
    };

    private final FatPack.Template fpData;

    public FatPack(final String name0, final FatPack.Template fpData0, final int boosterCount) {
        super(name0, StaticData.instance().getBoosters().get(fpData0.getEdition()), boosterCount);
        fpData = fpData0;
    }

    @Override
    public String getDescription() {
        return fpData.toString() + contents.toString();
    }

    @Override
    public final String getItemType() {
        return "Fat Pack";
    }
    
    @Override
    public List<PaperCard> getExtraCards() {
        return BoosterGenerator.getBoosterPack(fpData);
    }

    /*@Override
    protected List<PaperCard> generate() {
        List<PaperCard> result = new ArrayList<PaperCard>();
        for (int i = 0; i < fpData.getCntBoosters(); i++) {
            result.addAll(super.generate());
        }
        // Add any extra cards that may come in the fatpack after Boosters
        result.addAll(BoosterGenerator.getBoosterPack(fpData));
        return result;
    }*/

    @Override
    public final Object clone() {
        return new FatPack(name, fpData, fpData.cntBoosters);
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

                return new FatPack.Template(edition, nBoosters, slots);
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
