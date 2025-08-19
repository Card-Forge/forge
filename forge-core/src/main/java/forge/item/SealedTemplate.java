package forge.item;

import com.google.common.collect.Lists;
import forge.item.generation.BoosterSlots;
import forge.util.TextUtil;
import forge.util.storage.StorageReaderFile;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class SealedTemplate {

    public final static SealedTemplate genericDraftBooster = new SealedTemplate(null, Lists.newArrayList(
            Pair.of(BoosterSlots.COMMON, 10), Pair.of(BoosterSlots.UNCOMMON, 3),
            Pair.of(BoosterSlots.RARE_MYTHIC, 1), Pair.of(BoosterSlots.BASIC_LAND, 1)
    ));

    // This is a generic cube booster. 15 cards, no rarity slots.
    public final static SealedTemplate genericNoSlotBooster = new SealedTemplate(null, Lists.newArrayList(
            Pair.of(BoosterSlots.ANY, 15)
    ));

    protected final List<Pair<String, Integer>> slots;

    protected final String name;

    public final String getName() {
        return name;
    }

    public final List<Pair<String, Integer>> getSlots() {
        return slots;
    }

    public boolean hasSlot(String s) {
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
    public SealedTemplate(Iterable<Pair<String, Integer>> itrSlots) {
        this(null, itrSlots);
    }

    public SealedTemplate(String name0, Iterable<Pair<String, Integer>> itrSlots) {
        slots = Lists.newArrayList(itrSlots);
        name = name0;
    }

    public SealedTemplate(String code, String boosterDesc) {
        this(code, Reader.parseSlots(boosterDesc));
    }

    public int getNumberOfCardsExpected() {
        int sum = 0;
        for(Pair<String, Integer> p : slots) {
            sum += p.getRight();
        }
        return sum;
    }

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

        SealedTemplate template = (SealedTemplate) o;

        return slots.equals(template.slots) && name.equals(template.name);
    }

    @Override
    public int hashCode() {
        int result = slots.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }

    public final static class Reader extends StorageReaderFile<SealedTemplate> {
        public Reader(File file) {
            super(file, (Function<? super SealedTemplate, String>) (Function<SealedTemplate, String>) SealedTemplate::getName);
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
        protected SealedTemplate read(String line, int i) {
            String[] headAndData = TextUtil.split(line, ':', 2);
            return new SealedTemplate(headAndData[0], parseSlots(headAndData[1]));
        }
    }
}
