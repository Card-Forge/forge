package forge.control.match;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import forge.AllZone;
import forge.Card;
import forge.Constant.Zone;
import forge.PlayerZone;
import forge.view.match.ViewHand;
import forge.view.match.ViewHand.CardPanel;
import forge.view.match.ViewTopLevel;

/** 
 * Child controller - handles operations related to cards in user's hand
 * and their Swing components, which are assembled in ViewHand.
 *
 */
public class ControlHand {
    private List<Card> cardsInPanel;
    private ViewHand view;

    /** 
     * Child controller - handles operations related to cards in user's hand
     * and their Swing components, which are assembled in ViewHand.
     * @param v &emsp; The Swing component for user hand
     */
    public ControlHand(ViewHand v) {
        view = v;
        cardsInPanel = new ArrayList<Card>();
    }

    /** Adds observers to hand panel. */
    public void addObservers() {
        Observer o1 = new Observer() {
            public void update(final Observable a, final Object b) {
                resetCards(Arrays.asList(((PlayerZone) a).getCards()));
            }
        };
        AllZone.getHumanPlayer().getZone(Zone.Hand).addObserver(o1);
    }

    /** Adds listeners to hand panel: window resize, etc. */
    public void addListeners() {
        view.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                // Ensures cards in hand scale properly with parent.
                view.refreshLayout();
            }
        });
    }

    /**
     * Adds various listeners for cards in hand. Uses CardPanel
     * instance from ViewHand.
     * 
     * @param c &emsp; CardPanel object
     */
    public void addCardPanelListeners(final CardPanel c) {
        // Grab top level controller to facilitate interaction between children
        final ViewTopLevel display = (ViewTopLevel) (AllZone.getDisplay());
        final Card cardobj = c.getCard();

        // Sidebar pic/detail on card hover
        c.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(final MouseEvent e) {
                display.getCardviewerController().showCard(cardobj);
            }
        });

        // Mouse press
        c.addMouseListener(new MouseAdapter()  {
            @Override
            public void mousePressed(final MouseEvent e) {

                if (e.getButton() != MouseEvent.BUTTON1) {
                    return;
                }

                display.getInputController().getInputControl().selectCard(
                        cardobj, AllZone.getHumanPlayer().getZone(Zone.Hand));
            }
        });
    }

    /** @param c &emsp; Card object */
    public void addCard(Card c) {
        cardsInPanel.add(c);
        view.refreshLayout();
    }

    /** @param c &emsp; List of Card objects */
    public void addCards(List<Card> c) {
        cardsInPanel.addAll(c);
        view.refreshLayout();
    }

    /** @return List<Card> */
    public List<Card> getCards() {
        return cardsInPanel;
    }

    /** @param c &emsp; Card object */
    public void removeCard(Card c) {
        cardsInPanel.remove(c);
        view.refreshLayout();
    }

    /** @param c &emsp; List of Card objects */
    public void removeCards(List<Card> c) {
        cardsInPanel.removeAll(c);
        view.refreshLayout();
    }

    /** @param c &emsp; List of Card objects */
    public void resetCards(List<Card> c) {
        cardsInPanel.clear();
        addCards(c);
    }
}
