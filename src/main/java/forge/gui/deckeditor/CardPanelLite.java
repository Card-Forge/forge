package forge.gui.deckeditor;

import net.miginfocom.swing.MigLayout;

import forge.Card;
import forge.gui.game.CardDetailPanel;
import forge.gui.game.CardPicturePanel;
import forge.item.CardPrinted;
import forge.item.InventoryItem;

/**
 * This panel is to be placed in the right part of a deck editor.
 * 
 */
public class CardPanelLite extends CardPanelBase {

    private static final long serialVersionUID = -7134546689397508597L;

    // Controls to show card details
    protected CardDetailPanel detail = new CardDetailPanel(null);
    private CardPicturePanel picture = new CardPicturePanel(null);

    /**
     * 
     * Constructor.
     */
    public CardPanelLite() {
        this.setLayout(new MigLayout("fill, ins 0"));
        this.add(detail, "w 239, h 323, grow, flowy, wrap");
        this.add(picture, "wmin 239, hmin 323, grow");
    }

    /**
     * 
     * ShowCard.
     * 
     * @param card
     *            an InventoryItem
     */
    public final void showCard(final InventoryItem card) {
        picture.setCard(card);
        boolean isCard = card != null && card instanceof CardPrinted;
        detail.setVisible(isCard);
        if (isCard) {
            detail.setCard(((CardPrinted) card).toForgeCard());
        }
    }

    /**
     * 
     * getCard.
     * 
     * @return Card
     */
    public final Card getCard() {
        return detail.getCard();
    }

}
