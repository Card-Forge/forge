package forge.card;

import java.util.List;

import com.google.common.base.Supplier;

import forge.item.CardPrinted;

/** 
 * TODO: Write javadoc for this type.
 *
 */

public interface IUnOpenedProduct extends Supplier<List<CardPrinted>> {
    public List<CardPrinted> get();
}