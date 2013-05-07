package forge.item;

import java.util.List;
import java.util.Map.Entry;
import java.util.ArrayList;
import java.util.Collection;

import com.google.common.base.Function;

import forge.deck.CardPool;
import forge.util.MyRandom;
import forge.util.storage.StorageReaderFileSections;


/** 
 * TODO: Write javadoc for this type.
 *
 */
public class PrintSheet {
    public static final Function<PrintSheet, String> FN_GET_KEY = new Function<PrintSheet, String>() { 
        @Override public final String apply(PrintSheet sheet) { return sheet.name; } 
    };


    private final ItemPool<CardPrinted> cardsWithWeights;

    
    private final String name;
    public PrintSheet(String name0) {
        this(name0, null);
    }
    
    private PrintSheet(String name0, ItemPool<CardPrinted> pool) {
        name = name0;
        cardsWithWeights = pool != null ? pool : new ItemPool<CardPrinted>(CardPrinted.class);
    }
    
    public void add(CardPrinted card) {
        add(card,1);
    }

    public void add(CardPrinted card, int weight) {
        cardsWithWeights.add(card, weight);
    }

    public void addAll(Iterable<CardPrinted> cards) {
        addAll(cards, 1);
    }

    public void addAll(Iterable<CardPrinted> cards, int weight) {
        for(CardPrinted card : cards)
            cardsWithWeights.add(card, weight);
    }
    
    /** Cuts cards out of a sheet - they won't be printed again.
    * Please use mutable sheets for cubes only.*/ 
    public void removeAll(Iterable<CardPrinted> cards) {
        for(CardPrinted card : cards)
            cardsWithWeights.remove(card);
    }

    private CardPrinted fetchRoulette(int start, int roulette, Collection<CardPrinted> toSkip) {
        int sum = start;
        boolean isSecondRun = start > 0;
        for(Entry<CardPrinted, Integer> cc : cardsWithWeights ) {
            sum += cc.getValue().intValue();
            if( sum > roulette ) {
                if( toSkip != null && toSkip.contains(cc.getKey()))
                    continue;
                return cc.getKey();
            }
        }
        if( isSecondRun )
            throw new IllegalStateException("Print sheet does not have enough unique cards");
        
        return fetchRoulette(sum + 1, roulette, toSkip); // start over from beginning, in case last cards were to skip
    }
    
    public List<CardPrinted> random(int number, boolean wantUnique) {
        List<CardPrinted> result = new ArrayList<CardPrinted>();

        int totalWeight = cardsWithWeights.countAll();
        if( totalWeight == 0) {
            System.err.println("No cards were found on sheet " + name);
            return result;
        }

        // If they ask for 40 unique basic lands (to make a fatpack) out of 20 distinct possible, add the whole print run N times.
        int uniqueCards = cardsWithWeights.countDistinct();
        while ( number >= uniqueCards ) {
            for(Entry<CardPrinted, Integer> kv : cardsWithWeights) {
                result.add(kv.getKey());
            }
            number -= uniqueCards;
        }

        List<CardPrinted> uniques = wantUnique ? new ArrayList<CardPrinted>() : null; 
        for(int iC = 0; iC < number; iC++) {
            int index = MyRandom.getRandom().nextInt(totalWeight);
            CardPrinted toAdd = fetchRoulette(0, index, wantUnique ? uniques : null);
            result.add(toAdd);
            if( wantUnique )
                uniques.add(toAdd);
        }
        return result;
    }

    public static class Reader extends StorageReaderFileSections<PrintSheet> {
        public Reader(String fileName) {
            super(fileName, PrintSheet.FN_GET_KEY);
        }

        @Override
        protected PrintSheet read(String title, Iterable<String> body, int idx) {
            return new PrintSheet(title, CardPool.fromCardList(body));
        }
        
    }

    public boolean isEmpty() {
        return cardsWithWeights.isEmpty();
    }

    public Iterable<CardPrinted> toFlatList() {
        return cardsWithWeights.toFlatList();
    }


}
