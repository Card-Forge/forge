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

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Function;

import forge.ImageKeys;
import forge.StaticData;
import forge.card.CardEdition;
import forge.item.generation.BoosterGenerator;

public class FatPack extends BoxedProduct {
    public static final Function<CardEdition, FatPack> FN_FROM_SET = new Function<CardEdition, FatPack>() {
        @Override
        public FatPack apply(final CardEdition edition) {
            int boosters = edition.getFatPackCount();
            if (boosters <= 0) { return null; }

            FatPack.Template d = new Template(edition);
            if (d == null) { return null; }
            return new FatPack(edition.getName(), d, d.cntBoosters);
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
        boolean isBundle = StaticData.instance().getEditions().get(fpData.getEdition()).getDate().getTime() >=
                StaticData.instance().getEditions().get("KLD").getDate().getTime();

        return isBundle ? "Bundle" : "Fat Pack";
    }
    
    @Override
    public List<PaperCard> getExtraCards() {
        return BoosterGenerator.getBoosterPack(fpData);
    }

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

        private Template(CardEdition edition) {
            super(edition.getCode(), edition.getFatPackExtraSlots());

            cntBoosters = edition.getFatPackCount();
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

    @Override
    public String getImageKey(boolean altState) {
        return ImageKeys.FATPACK_PREFIX + getEdition();
    }    
}
