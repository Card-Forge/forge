package forge.card;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.item.PaperCard;
import forge.item.ItemPoolView;
import forge.item.PrintSheet;

public class UnOpenedProduct implements IUnOpenedProduct {

    private final SealedProductTemplate tpl;
    private final Map<String, PrintSheet> sheets;
    private boolean poolLimited = false; // if true after successful generation cards are removed from printsheets. 

    public final boolean isPoolLimited() {
        return poolLimited;
    }

    public final void setLimitedPool(boolean considerNumbersInPool) {
        this.poolLimited = considerNumbersInPool; // TODO: Add 0 to parameter's name.
    }
    

    // Means to select from all unique cards (from base game, ie. no schemes or avatars)
    public UnOpenedProduct(SealedProductTemplate template) {
        tpl = template;
        sheets = null;
    }

    // Invoke this constructor only if you are sure that the pool is not equal to deafult carddb
    public UnOpenedProduct(SealedProductTemplate template, ItemPoolView<PaperCard> pool) {
        this(template, pool.toFlatList());
    }

    public UnOpenedProduct(SealedProductTemplate template, Iterable<PaperCard> cards) {
        tpl = template;
        sheets = new TreeMap<String, PrintSheet>();
        prebuildSheets(cards);
    }

    public UnOpenedProduct(SealedProductTemplate sealedProductTemplate, Predicate<PaperCard> filterPrinted) {
        this(sealedProductTemplate, Iterables.filter(CardDb.instance().getAllCards(), filterPrinted));
    }

    private void prebuildSheets(Iterable<PaperCard> sourceList) {
        for(Pair<String, Integer> cc : tpl.getSlots()) {
            sheets.put(cc.getKey(), BoosterGenerator.makeSheet(cc.getKey(), sourceList));
        }
    }

    @Override
    public List<PaperCard> get() {
        return sheets == null ? BoosterGenerator.getBoosterPack(tpl) : getBoosterPack();
    }

    // If they request cards from an arbitrary pool, there's no use to cache printsheets.
    private final List<PaperCard> getBoosterPack() {
        List<PaperCard> result = new ArrayList<PaperCard>();
        for(Pair<String, Integer> slot : tpl.getSlots()) {
            PrintSheet ps = sheets.get(slot.getLeft());
            if(ps.isEmpty() &&  poolLimited ) {
                throw new IllegalStateException("The cardpool has been depleted and has no more cards for slot " + slot.getKey());
            }

            List<PaperCard> foundCards = ps.random(slot.getRight().intValue(), true); 
            if(poolLimited)
                ps.removeAll(foundCards);
            result.addAll(foundCards);
        }
        return result;
    }

}
 