package forge.gui.deckeditor;

import forge.card.CardPrinted;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public interface CardDisplay {
    void showCard(CardPrinted card);

    // Sorry, this is for JPanel initialization
    void jbInit();
}
