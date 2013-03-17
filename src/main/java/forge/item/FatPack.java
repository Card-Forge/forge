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

import com.google.common.base.Function;

import forge.Singletons;
import forge.card.CardEdition;
import forge.card.FatPackData;

public class FatPack extends OpenablePack {

    /** The Constant fnFromSet. */
    public static final Function<CardEdition, FatPack> FN_FROM_SET = new Function<CardEdition, FatPack>() {
        @Override
        public FatPack apply(final CardEdition arg1) {
            FatPackData d = Singletons.getModel().getFatPacks().get(arg1.getCode());
            return new FatPack(arg1.getName(), d);
        }
    };

    private final FatPackData fpData;

    public FatPack(final String name0, final FatPackData fpData0) {
        super(name0, Singletons.getModel().getBoosters().get(fpData0.getEdition()));
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
    protected List<CardPrinted> generate() {
        List<CardPrinted> result = new ArrayList<CardPrinted>();
        for (int i = 0; i < fpData.getCntBoosters(); i++) {
            result.addAll(super.generate());
        }
        result.addAll(getRandomBasicLands(fpData.getLandEdition(), fpData.getCntLands()));
        return result;
    }

    @Override
    public final Object clone() {
        return new FatPack(name, fpData);
    }

    @Override
    public int getTotalCards() {
        return super.getTotalCards() * fpData.getCntBoosters() + fpData.getCntLands();
    }
}
