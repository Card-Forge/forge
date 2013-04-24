package forge.item;

import java.util.List;
import java.util.Map.Entry;
import java.util.ArrayList;
import java.util.Collection;
import forge.util.MyRandom;


/** 
 * TODO: Write javadoc for this type.
 *
 */
public class PrintSheet {
    private final ItemPool<CardPrinted> cardsWithWeights = new ItemPool<CardPrinted>(CardPrinted.class);

    
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
    
    public CardPrinted random() {
        int totalWeight = cardsWithWeights.countAll();
        int index = MyRandom.getRandom().nextInt(totalWeight);
        return fetchRoulette(0, index, null);
    }

    public List<CardPrinted> random(int number, boolean wantUnique) {
        List<CardPrinted> result = new ArrayList<CardPrinted>();
        
        int totalWeight = cardsWithWeights.countAll();
        for(int iC = 0; iC < number; iC++) {
            int index = MyRandom.getRandom().nextInt(totalWeight);
            CardPrinted toAdd = fetchRoulette(0, index, wantUnique ? result : null);
            result.add(toAdd);
        }
        return result;
    }


}
