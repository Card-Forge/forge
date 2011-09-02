/**
 * CardContainer.java
 *
 * Created on 17.02.2010
 */

package forge;


/**
 * The class CardContainer. A card container is an object that references a card.
 *
 * @author Clemens Koza
 * @version V0.0 17.02.2010
 */
public interface CardContainer {
    /**
     * <p>setCard.</p>
     *
     * @param card a {@link forge.Card} object.
     */
    void setCard(Card card);

    /**
     * <p>getCard.</p>
     *
     * @return a {@link forge.Card} object.
     */
    Card getCard();
}
