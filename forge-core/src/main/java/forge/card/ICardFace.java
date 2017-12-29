package forge.card;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public interface ICardFace extends ICardCharacteristics, ICardRawAbilites, Comparable<ICardFace> {
    public String getAltName();
}
