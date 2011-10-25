package forge.gui.deckeditor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

import net.miginfocom.swing.MigLayout;

import forge.Card;
import forge.Singletons;
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
    private JButton bChangeState = new JButton();

    /**
     * 
     * Constructor.
     */
    public CardPanelLite() {
        bChangeState.setVisible(false);
        bChangeState.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                bChangeState_actionPerformed(e);
            }
        });
        if (!Singletons.getModel().getPreferences().lafFonts)
            bChangeState.setFont(new java.awt.Font("Dialog", 0, 10));
        
        this.setLayout(new MigLayout("fill, ins 0"));
        this.add(detail, "w 239, h 303, grow, flowy, wrap");
        this.add(bChangeState, "align 50% 0%, wrap");
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
            Card toSet = ((CardPrinted) card).toForgeCard();
            
            detail.setCard(toSet);
            if(toSet.hasAlternateState()) {
                bChangeState.setVisible(true);
                if(toSet.isFlip()) {
                    bChangeState.setText("Flip");
                }
                else {
                    bChangeState.setText("Transform");
                }
            }
        }
    }
    
    public void setCard(Card c) {
        picture.setCard(c);
        if(c != null) {
            detail.setCard(c);
            if(c.hasAlternateState()) {
                bChangeState.setVisible(true);
                if(c.isFlip()) {
                    bChangeState.setText("Flip");
                }
                else {
                    bChangeState.setText("Transform");
                }
            }
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
    
    private void bChangeState_actionPerformed(ActionEvent e) {
        Card cur = detail.getCard();
        if(cur != null) {
            cur.changeState();
            
            setCard(cur);
        }
    }

}
