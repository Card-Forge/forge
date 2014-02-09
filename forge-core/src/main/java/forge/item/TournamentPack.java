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
import forge.StaticData;
import forge.card.BoosterGenerator;
import forge.card.CardEdition;

import java.util.List;

public class TournamentPack extends SealedProduct {

    /** The Constant fnFromSet. */
    public static final Function<CardEdition, TournamentPack> FN_FROM_SET = new Function<CardEdition, TournamentPack>() {
        @Override
        public TournamentPack apply(final CardEdition arg1) {
            Template d = StaticData.instance().getTournamentPacks().get(arg1.getCode());
            return new TournamentPack(arg1.getName(), d);
        }
    };

    public TournamentPack(final String name0, final Template boosterData) {
        super(name0, boosterData);
    }

    public final boolean isStarterDeck() {
        return contents.getSlots().get(0).getRight() < 30; // hack - getting number of commons, they are first in list
    }

    @Override
    public final String getItemType() {
        return !isStarterDeck() ? "Tournament Pack" : "Starter Deck";
    }

    @Override
    protected List<PaperCard> generate() {
        return BoosterGenerator.getBoosterPack(this.contents);
    }

    @Override
    public final Object clone() {
        return new TournamentPack(name, contents);
    }
}
