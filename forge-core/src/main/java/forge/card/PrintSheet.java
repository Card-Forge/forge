package forge.card;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import com.google.common.base.Function;

import forge.deck.CardPool;
import forge.item.PaperCard;
import forge.util.ItemPool;
import forge.util.MyRandom;
import forge.util.storage.IStorage;
import forge.util.storage.StorageExtendable;
import forge.util.storage.StorageReaderFileSections;


/**
 * TODO: Write javadoc for this type.
 *
 */
public class PrintSheet {
    public static final Function<PrintSheet, String> FN_GET_KEY = new Function<PrintSheet, String>() {
        @Override public final String apply(PrintSheet sheet) { return sheet.name; }
    };

    public static final IStorage<PrintSheet> initializePrintSheets(File sheetsFile, CardEdition.Collection editions) {
        IStorage<PrintSheet> sheets = new StorageExtendable<>("Special print runs", new PrintSheet.Reader(sheetsFile));

        for (CardEdition edition : editions) {
            for (PrintSheet ps : edition.getPrintSheetsBySection()) {
                sheets.add(ps.name, ps);
            }
        }

        return sheets;
    }

    private final ItemPool<PaperCard> cardsWithWeights;


    private final String name;
    public PrintSheet(String name0) {
        this(name0, null);
    }

    public PrintSheet(String name0, ItemPool<PaperCard> pool) {
        name = name0;
        cardsWithWeights = pool != null ? pool : new ItemPool<>(PaperCard.class);
    }

    public void add(PaperCard card) {
        add(card,1);
    }

    public void add(PaperCard card, int weight) {
        cardsWithWeights.add(card, weight);
    }

    public void addAll(Iterable<PaperCard> cards) {
        addAll(cards, 1);
    }

    public void addAll(Iterable<PaperCard> cards, int weight) {
        for (PaperCard card : cards)
            cardsWithWeights.add(card, weight);
    }

    /** Cuts cards out of a sheet - they won't be printed again.
    * Please use mutable sheets for cubes only.*/
    public void removeAll(Iterable<PaperCard> cards) {
        for(PaperCard card : cards)
            cardsWithWeights.remove(card);
    }

    private PaperCard fetchRoulette(int start, int roulette, Collection<PaperCard> toSkip) {
        int sum = start;
        boolean isSecondRun = start > 0;
        for (Entry<PaperCard, Integer> cc : cardsWithWeights ) {
            sum += cc.getValue();
            if (sum > roulette) {
                if (toSkip != null && toSkip.contains(cc.getKey()))
                    continue;
                return cc.getKey();
            }
        }
        if (isSecondRun)
            throw new IllegalStateException("Print sheet does not have enough unique cards");

        return fetchRoulette(sum + 1, roulette, toSkip); // start over from beginning, in case last cards were to skip
    }

    public List<PaperCard> all() {
        List<PaperCard> result = new ArrayList<>();
        for (Entry<PaperCard, Integer> kv : cardsWithWeights) {
            for (int i = 0; i < kv.getValue(); i++) {
                result.add(kv.getKey());
            }
        }
        return result;
    }

    public List<PaperCard> random(int number, boolean wantUnique) {
        List<PaperCard> result = new ArrayList<>();

        int totalWeight = cardsWithWeights.countAll();
        if (totalWeight == 0) {
            System.err.println("No cards were found on sheet " + name);
            return result;
        }

        // If they ask for 40 unique basic lands (to make a fatpack) out of 20 distinct possible, add the whole print run N times.
        int uniqueCards = cardsWithWeights.countDistinct();
        while (number >= uniqueCards) {
            for (Entry<PaperCard, Integer> kv : cardsWithWeights) {
                result.add(kv.getKey());
            }
            number -= uniqueCards;
        }

        List<PaperCard> uniques = wantUnique ? new ArrayList<>() : null;
        for (int iC = 0; iC < number; iC++) {
            int index = MyRandom.getRandom().nextInt(totalWeight);
            PaperCard toAdd = fetchRoulette(0, index, wantUnique ? uniques : null);
            result.add(toAdd);
            if (wantUnique)
                uniques.add(toAdd);
        }
        return result;
    }

    public boolean isEmpty() {
        return cardsWithWeights.isEmpty();
    }

    public Iterable<PaperCard> toFlatList() {
        return cardsWithWeights.toFlatList();
    }

    public static class Reader extends StorageReaderFileSections<PrintSheet> {
        public Reader(File file) {
            super(file, PrintSheet.FN_GET_KEY);
        }

        @Override
        protected PrintSheet read(String title, Iterable<String> body, int idx) {
            return new PrintSheet(title, CardPool.fromCardList(body));
        }

    }

}
