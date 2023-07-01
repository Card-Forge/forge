package forge.item.generation;

import java.util.List;

import com.google.common.base.Supplier;

import forge.item.PaperCard;

/**
 * TODO: Write javadoc for this type.
 *
 */

public interface IUnOpenedProduct extends Supplier<List<PaperCard>> {
    List<PaperCard> get();
}