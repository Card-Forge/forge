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

import forge.StaticData;
import forge.item.generation.BoosterGenerator;
import forge.util.StreamUtil;

import java.util.ArrayList;
import java.util.List;

public abstract class SealedProduct implements InventoryItemFromSet {

    public static final List<String> specialSets = new ArrayList<>();

    protected final SealedTemplate contents;
    protected final String name;
    private final int hash;
    protected List<PaperCard> cards = null;

    static {
        specialSets.add("Black");
        specialSets.add("Blue");
        specialSets.add("Green");
        specialSets.add("Red");
        specialSets.add("White");
        specialSets.add("Colorless");
    }

    public SealedProduct(String name0, SealedTemplate boosterData) {
        if (null == name0)       { throw new IllegalArgumentException("name0 must not be null"); }
        if (null == boosterData) {
            throw new IllegalArgumentException("boosterData for " + name0 + " must not be null");
        }
        contents = boosterData;
        name = name0;
        hash = name.hashCode() ^ getClass().hashCode() ^ contents.hashCode();
    }

    @Override
    public final String getName() {
        return name + " " + getItemType();
    }

    public String getDescription() {
        return contents.toString();
    }

    @Override
    public final String getEdition() {
        return contents.getEdition();
    }

    public List<PaperCard> getCards() {
        if (null == cards) {
            cards = generate();
        }

        return cards;
    }

    public int getTotalCards() {
        return contents.getNumberOfCardsExpected();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SealedProduct other = (SealedProduct) o;

        return contents.equals(other.contents) && name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return getName();
    }

    protected List<PaperCard> generate() {
        return BoosterGenerator.getBoosterPack(contents);

    }

    protected PaperCard getRandomBasicLand(final String setCode) {
        return this.getRandomBasicLands(setCode, 1).get(0);
    }

    protected List<PaperCard> getRandomBasicLands(final String setCode, final int count) {
        return StaticData.instance().getCommonCards().streamAllCards()
                .filter(PaperCardPredicates.printedInSet(setCode))
                .filter(PaperCardPredicates.IS_BASIC_LAND)
                .collect(StreamUtil.random(count));
    }
}
