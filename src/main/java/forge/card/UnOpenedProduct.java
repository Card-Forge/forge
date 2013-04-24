package forge.card;

import java.util.List;

import com.google.common.base.Supplier;

import forge.item.CardPrinted;
import forge.item.ItemPoolView;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class UnOpenedProduct implements Supplier<List<CardPrinted>> {

    private final ItemPoolView<CardPrinted> cards;
    private final Iterable<CardPrinted> cardPoolFlat;
    private final SealedProductTemplate tpl;
    /**
     * TODO: Write javadoc for Constructor.
     */
    public UnOpenedProduct(SealedProductTemplate template) {
        tpl = template;
        cards = null;
        cardPoolFlat = null;
    }

    public UnOpenedProduct(SealedProductTemplate template, ItemPoolView<CardPrinted> pool) {
        cards = pool;
        cardPoolFlat = null;
        tpl = template;
    }

    public UnOpenedProduct(SealedProductTemplate template, Iterable<CardPrinted> pool) {
        cardPoolFlat = pool;
        tpl = template;
        cards = null;
     }
    
    /* (non-Javadoc)
     * @see com.google.common.base.Supplier#get()
     */
    @Override
    public List<CardPrinted> get() {
        return cards == null && cardPoolFlat == null ? BoosterGenerator.getBoosterPack(tpl) 
                : BoosterGenerator.getBoosterPack(tpl, cardPoolFlat == null ? cards.toFlatList() : cardPoolFlat);
    }

}
