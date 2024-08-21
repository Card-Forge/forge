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

import forge.ImageKeys;
import forge.StaticData;
import forge.card.CardEdition;
import forge.item.generation.BoosterGenerator;

import java.util.List;

public class TournamentPack extends SealedProduct {

    public static TournamentPack fromSet(CardEdition edition) {
        SealedTemplate d = StaticData.instance().getTournamentPacks().get(edition.getCode());
        return new TournamentPack(edition.getName(), d);
    }

    public TournamentPack(final String name0, final SealedTemplate boosterData) {
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

    @Override
    public String getImageKey(boolean altState) {
        return ImageKeys.TOURNAMENTPACK_PREFIX + getEdition();
    }
}
