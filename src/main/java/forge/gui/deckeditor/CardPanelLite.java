package forge.gui.deckeditor;

import net.miginfocom.swing.MigLayout;

import forge.Card;
import forge.card.CardPrinted;
import forge.gui.game.CardDetailPanel;
import forge.gui.game.CardPicturePanel;

/** 
 * This panel is to be placed in the right part of a deck editor
 *
 */
public class CardPanelLite extends CardPanelBase {

    private static final long serialVersionUID = -7134546689397508597L;

    // Controls to show card details 
    protected CardDetailPanel detail = new CardDetailPanel(null);
    private CardPicturePanel picture = new CardPicturePanel(null);

    public CardPanelLite() {
        this.setLayout(new MigLayout("fill, ins 0"));
        this.add(detail, "w 239, h 323, grow, flowy, wrap");
        this.add(picture, "wmin 239, hmin 323, grow");
    }

    public void showCard(CardPrinted card) {
        picture.setCard(card);
        detail.setCard(card != null ? card.toForgeCard() : null);
    }

    public Card getCard() { return detail.getCard(); }

}
