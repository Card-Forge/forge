/**
 * CardContainer.java
 * 
 * Created on 17.02.2010
 */

package forge;


/**
 * The class CardContainer. A card container is an object that references a card.
 * 
 * @version V0.0 17.02.2010
 * @author Clemens Koza
 */
public interface CardContainer {
    public void setCard(Card card);
    
    public Card getCard();
}
