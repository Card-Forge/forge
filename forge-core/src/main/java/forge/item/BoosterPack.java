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

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;

import forge.ImageKeys;
import forge.StaticData;
import forge.card.CardEdition;
import forge.item.generation.BoosterSlots;
import forge.util.MyRandom;

public class BoosterPack extends SealedProduct {
    private final int artIndex;
    private final int hash;

    public static final Function<CardEdition, BoosterPack> FN_FROM_SET = new Function<CardEdition, BoosterPack>() {
        @Override
        public BoosterPack apply(final CardEdition edition) {
            String boosterKind = edition.getRandomBoosterKind();
            Template d = edition.getBoosterTemplate(boosterKind);
            StringBuilder sb = new StringBuilder(edition.getName());
            sb.append(" ").append(boosterKind);
            return new BoosterPack(sb.toString(), d);
        }
    };

    public static final Function<String, BoosterPack> FN_FROM_COLOR = new Function<String, BoosterPack>() {
        @Override
        public BoosterPack apply(final String color) {
            return new BoosterPack(color, new Template("?", ImmutableList.of(
                    Pair.of(BoosterSlots.COMMON + ":color(\"" + color + "\"):!" + BoosterSlots.LAND, 11),
                    Pair.of(BoosterSlots.UNCOMMON + ":color(\"" + color + "\"):!" + BoosterSlots.LAND, 3),
                    Pair.of(BoosterSlots.RARE_MYTHIC + ":color(\"" + color + "\"):!" + BoosterSlots.LAND, 1),
                    Pair.of(BoosterSlots.LAND + ":color(\"" + color + "\")", 1))
            ));
        }
    };

    public BoosterPack(final String name0, final Template boosterData) {
        super(name0, boosterData);

        if (specialSets.contains(boosterData.getEdition()) || boosterData.getEdition().equals("?")) {
            artIndex = 1;
        } else {
            int maxIdx = StaticData.instance().getEditions().get(boosterData.getEdition()).getCntBoosterPictures();
            artIndex = MyRandom.getRandom().nextInt(maxIdx) + 1;
        }

        hash = super.hashCode() ^ artIndex;

    }

    public final int getArtIndex() {
        return artIndex;
    }

    @Override
    public final String getItemType() {
        return "Booster Pack";
    }

    @Override
    public String getDescription() {
        if (specialSets.contains(getEdition()) || getEdition().equals("?")) {
            String color = getName().substring(0, getName().indexOf(getItemType()) - 1).toLowerCase();
            return "11 " + color + " commons, 3 " + color + " uncommons, 1 " + color + " rare, and 1 " + color + " land.";
        }
        return super.getDescription();
    }

    @Override
    public final Object clone() {
        return new BoosterPack(name, contents);
    }

    public Template getBoosterData() {
        return contents;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        BoosterPack other = (BoosterPack) o;

        return artIndex == other.artIndex;
    }

    @Override
    public final int hashCode() {
        return hash;
    }

    @Override
    public String getImageKey(boolean altState) {
        String edition = getEdition();
        if (SealedProduct.specialSets.contains(edition) || edition.equals("?")) {
            return "b:" + getName().substring(0, getName().indexOf(getItemType()) - 1);
        }
        int cntPics = StaticData.instance().getEditions().get(edition).getCntBoosterPictures();
        String suffix = (1 >= cntPics) ? "" : ("_" + artIndex);
        return ImageKeys.BOOSTER_PREFIX + edition + suffix;
    }
}
