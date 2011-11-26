package forge.view.match;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import forge.Card;
import forge.ImageCache;
import forge.control.match.ControlCardviewer;
import forge.view.toolbox.CardDetailPanel;
import forge.view.toolbox.FPanel;
import forge.view.toolbox.FVerticalTabPanel;

/** 
 * Vertical tab panel for viewing card picture and/or details.
 *
 */
public class ViewCardviewer {
    private List<JPanel> panelList;
    private ControlCardviewer control;

    private CardPicPanel pnlCardPic;
    private CardDetailPanel pnlCardDetail;
    private FVerticalTabPanel vtpCardviewer;

    private int w, h;

    /**
     * 
     */
    public ViewCardviewer() {
        // Assemble card pic viewer
        panelList = new ArrayList<JPanel>();

        pnlCardPic = new CardPicPanel();
        pnlCardPic.setOpaque(false);
        pnlCardPic.setName("Picture");
        pnlCardPic.setToolTipText("Card Picture");
        panelList.add(pnlCardPic);

        pnlCardDetail = new CardDetailPanel();
        pnlCardDetail.setOpaque(false);
        pnlCardDetail.setName("Detail");
        pnlCardDetail.setToolTipText("Card Text");
        panelList.add(pnlCardDetail);

        vtpCardviewer = new FVerticalTabPanel(panelList);

        // After all components are in place, instantiate controller.
        control = new ControlCardviewer(this);
    }

    /** @return ControlCardviewer */
    public ControlCardviewer getController() {
        return control;
    }

    /**
     * Card picture handling in side panel of match.
     *
     */
    @SuppressWarnings("serial")
    public class CardPicPanel extends FPanel {
        private Card card = null;

        /** @param c &emsp; Card object */
        public void setCard(Card c) {
            this.card = c;
            repaint();
        }

        /** @return Card */
        public Card getCard() {
            return this.card;
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (card != null) {
                w = getWidth();
                h = (int) (w / 0.7);
                BufferedImage img = ImageCache.getImage(card, w, h);
                g.drawImage(img, 0, (int) ((getHeight() - h) / 2), null);
            }
        }
    }

    /** @return CardPicPanel */
    public CardPicPanel getPnlCardPic() {
        return pnlCardPic;
    }

    /** @return CardDetailPanel */
    public CardDetailPanel getPnlCardDetail() {
        return pnlCardDetail;
    }

    /** @return FVerticalTabPanel */
    public FVerticalTabPanel getVtpCardviewer() {
        return vtpCardviewer;
    }
}
