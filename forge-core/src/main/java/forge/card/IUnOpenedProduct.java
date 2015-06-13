package forge.card;

import com.google.common.base.Supplier;
import forge.item.PaperCard;

import java.util.List;

/**
 * TODO: Write javadoc for this type.
 *
 */

public interface IUnOpenedProduct extends Supplier<List<PaperCard>> {
    List<PaperCard> get();
}