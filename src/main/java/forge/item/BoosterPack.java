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

import com.google.common.base.Function;

import forge.ImageCache;
import forge.Singletons;
import forge.card.BoosterData;
import forge.card.CardEdition;

public class BoosterPack extends OpenablePack {
    public static final Function<CardEdition, BoosterPack> FN_FROM_SET = new Function<CardEdition, BoosterPack>() {
        @Override
        public BoosterPack apply(final CardEdition arg1) {
            BoosterData d = Singletons.getModel().getBoosters().get(arg1.getCode());
            return new BoosterPack(arg1.getName(), d);
        }
    };

    public BoosterPack(final String name0, final BoosterData boosterData) {
        super(name0, boosterData);
    }

    @Override
    public final String getImageKey() {
        return ImageCache.BOOSTER_PREFIX + this.contents.getEdition();
    }

    @Override
    public final String getItemType() {
        return "Booster Pack";
    }

    @Override
    public final Object clone() {
        return new BoosterPack(name, contents);
    }
}
