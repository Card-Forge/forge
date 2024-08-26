package forge.card;

import java.util.Map;

/**
 * TODO: Write javadoc for this type.
 *
 */
public interface ICardFace extends ICardCharacteristics, ICardRawAbilites, Comparable<ICardFace> {
    String getAltName();

    boolean hasFunctionalVariants();
    ICardFace getFunctionalVariant(String variant);
    Map<String, ? extends ICardFace> getFunctionalVariants();
}
