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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.StaticData;
import forge.card.CardRulesPredicates;
import forge.item.generation.BoosterGenerator;
import forge.item.generation.BoosterSlots;
import forge.util.Aggregates;
import forge.util.TextUtil;
import forge.util.storage.StorageReaderFile;

public abstract class SealedProduct implements InventoryItemFromSet {

    public static final List<String> specialSets = new ArrayList<>();

    protected final Template contents;
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

    public SealedProduct(String name0, Template boosterData) {
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
        Predicate<PaperCard> cardsRule = Predicates.and(
                IPaperCard.Predicates.printedInSet(setCode),
                Predicates.compose(CardRulesPredicates.Presets.IS_BASIC_LAND, PaperCard.FN_GET_RULES));
        return Aggregates.random(Iterables.filter(StaticData.instance().getCommonCards().getAllCards(), cardsRule), count);
    }


    public static class Template {

        @SuppressWarnings("unchecked")
        public final static Template genericDraftBooster = new Template(null, Lists.newArrayList(
            Pair.of(BoosterSlots.COMMON, 10), Pair.of(BoosterSlots.UNCOMMON, 3),
            Pair.of(BoosterSlots.RARE_MYTHIC, 1), Pair.of(BoosterSlots.BASIC_LAND, 1)
        ));

        protected final List<Pair<String, Integer>> slots;
        protected final String name;

        public final List<Pair<String, Integer>> getSlots() {
            return slots;
        }

        public boolean hasSlot(String s)
        {
            for (Pair<String, Integer> slot : getSlots()) {
                String slotName = slot.getLeft();
                // Anything after a space or ! or : is not part of the slot's main type
                if (slotName.split("[ :!]")[0].equals(s)) {
                    return true;
                }
            }
            return false;
        }
        
        public final String getEdition() {
            return name;
        }
        public Template(Iterable<Pair<String, Integer>> itrSlots)
        {
            this(null, itrSlots);
        }

        public Template(String name0, Iterable<Pair<String, Integer>> itrSlots)
        {
            slots = Lists.newArrayList(itrSlots);
            name = name0;
        }

        public Template(String code, String boosterDesc) {
            this(code, Reader.parseSlots(boosterDesc));
        }

        public int getNumberOfCardsExpected() {
            int sum = 0;
            for(Pair<String, Integer> p : slots) {
                sum += p.getRight();
            }
            return sum;
        }

        public static final Function<? super Template, String> FN_GET_NAME = new Function<Template, String>() {
            @Override
            public String apply(Template arg1) {
                return arg1.name;
            }
        };

        @Override
        public String toString() {
            StringBuilder s = new StringBuilder();

            s.append("consisting of ");
            for(Pair<String, Integer> p : slots) {
                s.append(p.getRight()).append(" ").append(p.getLeft()).append(", ");
            }

            // trim the last comma and space
            s.replace(s.length() - 2, s.length(), "");

            // put an 'and' before the previous comma
            int lastCommaIdx = s.lastIndexOf(",");
            if (0 < lastCommaIdx) {
                s.replace(lastCommaIdx+1, lastCommaIdx+1, " and");
            }

            return s.toString();
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Template template = (Template) o;

            return slots.equals(template.slots) && name.equals(template.name);
        }

        @Override
        public int hashCode() {
            int result = slots.hashCode();
            result = 31 * result + name.hashCode();
            return result;
        }

        public final static class Reader extends StorageReaderFile<Template> {
            public Reader(File file) {
                super(file, Template.FN_GET_NAME);
            }

            public static List<Pair<String, Integer>> parseSlots(String data) {
                final String[] dataz = TextUtil.splitWithParenthesis(data, ',');
                List<Pair<String, Integer>> slots = new ArrayList<>();
                for (String slotDesc : dataz) {
                    String[] kv = TextUtil.splitWithParenthesis(slotDesc, ' ', 2);
                    slots.add(ImmutablePair.of(kv[1].replace(";", ","), Integer.parseInt(kv[0])));
                }
                return slots;
            }

            @Override
            protected Template read(String line, int i) {
                String[] headAndData = TextUtil.split(line, ':', 2);
                return new Template(headAndData[0], parseSlots(headAndData[1]));
            }
        }
    }

}
