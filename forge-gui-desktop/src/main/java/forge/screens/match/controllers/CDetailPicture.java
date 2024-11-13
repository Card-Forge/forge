package forge.screens.match.controllers;

import forge.game.GameView;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.gui.interfaces.IMayViewCards;
import forge.item.IPaperCard;
import forge.item.InventoryItem;

/**
 * Class that controls and links a {@link CDetail} and a {@link CPicture}.
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 */
public class CDetailPicture {

    private final CDetail cDetail;
    private final CPicture cPicture;
    private final IMayViewCards mayView;

    private CardView currentView = null;
    private boolean isDisplayAlt = false, alwaysDisplayAlt = false;

    public CDetailPicture() {
        this(IMayViewCards.ALL);
    }
    public CDetailPicture(final IMayViewCards mayView) {
        this.mayView = mayView;
        this.cDetail = new CDetail(this);
        this.cPicture = new CPicture(this);
    }

    CardView getCurrentCard() {
        return currentView;
    }
    public CDetail getCDetail() {
        return cDetail;
    }
    public CPicture getCPicture() {
        return cPicture;
    }

    public void displayAlt(final boolean showAlt) {
        isDisplayAlt = showAlt;
    }

    public void showCard(final CardView c, final boolean showAlt) {
        currentView = c;
        final boolean mayFlip = mayView() && mayFlip();
        isDisplayAlt = mayFlip && showAlt;
        alwaysDisplayAlt = mayFlip && c.isFaceDown();

        update();
    }

    /**
     * Displays image associated with either a {@code Card}
     * or {@code InventoryItem} instance.
     */
    public void showItem(final InventoryItem item) {
        if (item instanceof IPaperCard) {
            final IPaperCard paperCard = ((IPaperCard)item);
            final CardView c = CardView.getCardForUi(paperCard);
            if (paperCard.isFoil() && c.getCurrentState().getFoilIndex() == 0) {
                // FIXME should assign a random foil here in all cases
                // (currently assigns 1 for the deck editors where foils "flicker" otherwise)
                if (item instanceof Card) {
                    c.getCurrentState().setFoilIndexOverride(-1); //-1 to choose random
                } else if (item instanceof IPaperCard) {
                    c.getCurrentState().setFoilIndexOverride(1);
                }
            }
            showCard(c, isDisplayAlt);
        } else {
            currentView = null;
            isDisplayAlt = false;
            alwaysDisplayAlt = false;

            cDetail.showItem(item);
            cPicture.showItem(item);
        }
    }

    public void setGameView(final GameView gameView) {
        cDetail.getView().setGameView(gameView);
    }

    void flip() {
        if (mayFlip()) {
            isDisplayAlt = !isDisplayAlt;
            update();
        }
    }

    private void update() {
        final boolean mayView = mayView(), mayFlip = mayFlip();
        cDetail.showCard(currentView, isDisplayAlt, mayView, mayFlip);
        cPicture.showCard(currentView, isDisplayAlt || alwaysDisplayAlt, mayView, mayFlip && !alwaysDisplayAlt);
    }

    private boolean mayView() {
        return currentView == null || mayView.mayView(currentView) || currentView.isForeTold(); // FIXME: should isForeTold be added somewhere higher up in the chain?
    }

    private boolean mayFlip() {
        return currentView != null && mayView.mayFlip(currentView);
    }

}
