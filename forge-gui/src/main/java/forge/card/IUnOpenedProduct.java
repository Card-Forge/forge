package forge.card;

import java.util.List;

import com.google.common.base.Supplier;

import forge.item.PaperCard;

/** 
 * TODO: Write javadoc for this type.
 *
 */

public interface IUnOpenedProduct extends Supplier<List<PaperCard>> {
    public List<PaperCard> get();
}