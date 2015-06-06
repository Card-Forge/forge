package forge.card;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import forge.StaticData;
import forge.item.PaperCard;
import forge.item.SealedProduct;
import forge.util.ItemPool;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public class UnOpenedProduct implements IUnOpenedProduct {

    private final SealedProduct.Template tpl;
    private final Map<String, PrintSheet> sheets;
    private boolean poolLimited = false; // if true after successful generation cards are removed from printsheets.

    public final boolean isPoolLimited() {
        return poolLimited;
    }

    public final void setLimitedPool(boolean considerNumbersInPool) {
        this.poolLimited = considerNumbersInPool; // TODO: Add 0 to parameter's name.
    }


    // Means to select from all unique cards (from base game, ie. no schemes or avatars)
    public UnOpenedProduct(SealedProduct.Template template) {
        tpl = template;
        sheets = null;
    }

    // Invoke this constructor only if you are sure that the pool is not equal to deafult carddb
    public UnOpenedProduct(SealedProduct.Template template, ItemPool<PaperCard> pool) {
        this(template, pool.toFlatList());
    }

    public UnOpenedProduct(SealedProduct.Template template, Iterable<PaperCard> cards) {
        tpl = template;
        sheets = new TreeMap<>();
        prebuildSheets(cards);
    }

    public UnOpenedProduct(SealedProduct.Template sealedProductTemplate, Predicate<PaperCard> filterPrinted) {
        this(sealedProductTemplate, Iterables.filter(StaticData.instance().getCommonCards().getAllCards(), filterPrinted));
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
    private List<PaperCard> getBoosterPack() {
        List<PaperCard> result = new ArrayList<>();
        for(Pair<String, Integer> slot : tpl.getSlots()) {
            PrintSheet ps = sheets.get(slot.getLeft());
            if(ps.isEmpty() &&  poolLimited ) {
                throw new IllegalStateException("The cardpool has been depleted and has no more cards for slot " + slot.getKey());
            }

            List<PaperCard> foundCards = ps.random(slot.getRight(), true);
            if(poolLimited)
                ps.removeAll(foundCards);
            result.addAll(foundCards);
        }
        return result;
    }

}
