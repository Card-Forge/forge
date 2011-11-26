package forge.control.match;

import java.awt.Image;

import forge.Card;
import forge.ImageCache;
import forge.view.match.ViewCardviewer;

/** 
 * 
 * Controls the vertical tabber in sidebar used for
 * viewing card details and picture.
 *
 */
public class ControlCardviewer {
    private ViewCardviewer view;
    private Card currentCard = null;

    /** 
     * Controls the vertical tabber in sidebar used for
     * viewing card details and picture.
     * @param v &emsp; The CardViewer Swing component.
     */
    public ControlCardviewer(ViewCardviewer v) {
        view = v;
    }

    /**
     * Shows card details and/or picture in sidebar cardview tabber.
     * @param c &emsp; Card object
     */
    public void showCard(Card c) {
        Image img = ImageCache.getImage(c);
        this.currentCard = c;
        view.getPnlCardPic().setCard(c);
        view.getPnlCardDetail().setCard(c);

        if (img != null) {
            showPnlCardPic();
        }
        else {
            showPnlCardDetail();
        }
    }

    /** @return Card */
    public Card getCurrentCard() {
        return currentCard;
    }

    /**
     * Programatically forces card layout of sidebar tabber to show "CardDetail" panel.
     */
    public void showPnlCardDetail() {
        view.getVtpCardviewer().showTab(1);
    }

    /**
     * Programatically forces card layout of sidebar tabber to show card picture panel.
     */
    public void showPnlCardPic() {
        view.getVtpCardviewer().showTab(0);
    }
}
