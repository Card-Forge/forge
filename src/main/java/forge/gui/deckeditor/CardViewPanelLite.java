package forge.gui.deckeditor;

import java.io.File;

import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import forge.Card;
import forge.card.CardPrinted;
import forge.gui.game.CardDetailPanel;
import forge.gui.game.CardPicturePanel;

/** 
 * This panel is to be placed in the right part of a deck editor
 *
 */
public class CardViewPanelLite extends JPanel implements CardDisplay {

    private static final long serialVersionUID = -7134546689397508597L;

    // Controls to show card details 
    protected CardDetailPanel detail = new CardDetailPanel(null);
    private CardPicturePanel picture = new CardPicturePanel(null);

    /** Constant <code>previousDirectory</code> */
    protected static File previousDirectory = null;


    public void jbInit() {
        this.setLayout(new MigLayout("fill, ins 0"));
        this.add(detail, "w 239, h 323, grow, flowy, wrap");
        this.add(picture, "wmin 239, hmin 323, grow");
    }

    public void showCard(CardPrinted card) {
        picture.setCard(card);
        Card card2 = card.toForgeCard();
        detail.setCard(card2);
    }

    public Card getCard() { return detail.getCard(); }

}
