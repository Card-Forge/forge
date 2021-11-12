package forge.card;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinImage;
import forge.deck.ArchetypeDeckGenerator;
import forge.deck.CardThemedDeckGenerator;
import forge.deck.CommanderDeckGenerator;
import forge.deck.DeckProxy;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.gamemodes.planarconquest.ConquestCommander;
import forge.item.IPaperCard;
import forge.item.InventoryItem;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.screens.match.MatchController;
import forge.toolbox.FCardPanel;
import forge.toolbox.FDialog;
import forge.toolbox.FOverlay;
import forge.util.Localizer;
import forge.util.Utils;
import forge.util.collect.FCollectionView;

public class CardZoom extends FOverlay {
    private static final float REQ_AMOUNT = Utils.AVG_FINGER_WIDTH;

    private static final CardZoom cardZoom = new CardZoom();
    private static final ForgePreferences prefs = FModel.getPreferences();
    private static List<?> items;
    private static int currentIndex, initialIndex;
    private static CardView currentCard, prevCard, nextCard;
    private static boolean zoomMode = true;
    private static boolean oneCardView = prefs.getPrefBoolean(FPref.UI_SINGLE_CARD_ZOOM);
    private float totalZoomAmount;
    private static ActivateHandler activateHandler;
    private static String currentActivateAction;
    private static Rectangle flipIconBounds;
    private static Rectangle mutateIconBounds;
    private static boolean showAltState;
    private static boolean showBackSide = false;
    private static boolean showMerged = false;

    public static void show(Object item) {
        show(item, false);
    }
    public static void show(Object item, boolean showbackside) {
        List<Object> items0 = new ArrayList<>();
        items0.add(item);
        showBackSide = showbackside; //reverse the displayed zoomed card for the choice list
        show(items0, 0, null);
    }
    public static void show(FCollectionView<?> items0, int currentIndex0, ActivateHandler activateHandler0) {
        show((List<?>)items0, currentIndex0, activateHandler0);
    }
    public static void show(final List<?> items0, int currentIndex0, ActivateHandler activateHandler0) {
        items = items0;
        activateHandler = activateHandler0;
        currentIndex = currentIndex0;
        initialIndex = currentIndex0;
        currentCard = getCardView(items.get(currentIndex));
        prevCard = currentIndex > 0 ? getCardView(items.get(currentIndex - 1)) : null;
        nextCard = currentIndex < items.size() - 1 ? getCardView(items.get(currentIndex + 1)) : null;
        onCardChanged();
        cardZoom.show();
    }

    public static boolean isOpen() {
        return cardZoom.isVisible();
    }

    public static void hideZoom() {
        cardZoom.hide();
    }

    private CardZoom() {
    }

    @Override
    public void setVisible(boolean visible0) {
        if (this.isVisible() == visible0) { return; }

        super.setVisible(visible0);

        //update selected index when hidden if current index is different than initial index
        if (!visible0 && activateHandler != null && currentIndex != initialIndex) {
            activateHandler.setSelectedIndex(currentIndex);
        }
    }

    private static void incrementCard(int dir) {
        if (dir > 0) {
            if (currentIndex == items.size() - 1) { return; }
            currentIndex++;

            prevCard = currentCard;
            currentCard = nextCard;
            nextCard = currentIndex < items.size() - 1 ? getCardView(items.get(currentIndex + 1)) : null;
        }
        else {
            if (currentIndex == 0) { return; }
            currentIndex--;

            nextCard = currentCard;
            currentCard = prevCard;
            prevCard = currentIndex > 0 ? getCardView(items.get(currentIndex - 1)) : null;
        }
        onCardChanged();
    }

    private static void onCardChanged() {
        mutateIconBounds = null;
        if (activateHandler != null) {
            currentActivateAction = activateHandler.getActivateAction(currentIndex);
        }
        if (MatchController.instance.mayFlip(currentCard)) {
            flipIconBounds = new Rectangle();
        } else {
            flipIconBounds = null;
        }
        if (currentCard != null) {
            if (currentCard.getMergedCardsCollection() != null )
                if (currentCard.getMergedCardsCollection().size() > 0)
                    mutateIconBounds = new Rectangle();
        }
        showAltState = false;
    }

    private static CardView getCardView(Object item) {
        if (item instanceof Entry) {
            item = ((Entry<?, ?>)item).getKey();
        }
        if (item instanceof CardView) {
            return (CardView)item;
        }
        if (item instanceof DeckProxy) {
            if (item instanceof CardThemedDeckGenerator){
                return CardView.getCardForUi(((CardThemedDeckGenerator)item).getPaperCard());
            }else if (item instanceof CommanderDeckGenerator){
                return CardView.getCardForUi(((CommanderDeckGenerator)item).getPaperCard());
            }else if (item instanceof ArchetypeDeckGenerator){
                return CardView.getCardForUi(((ArchetypeDeckGenerator)item).getPaperCard());
            }else{
                DeckProxy deck = ((DeckProxy)item);
                return new CardView(-1, null, deck.getName(), null, deck.getImageKey(false));
            }

        }
        if (item instanceof IPaperCard) {
            return CardView.getCardForUi((IPaperCard)item);
        }
        if (item instanceof ConquestCommander) {
            return CardView.getCardForUi(((ConquestCommander)item).getCard());
        }
        if (item instanceof InventoryItem) {
            InventoryItem ii = (InventoryItem)item;
            return new CardView(-1, null, ii.getName(), null, ii.getImageKey(false));
        }
        return new CardView(-1, null, item.toString());
    }

    @Override
    public boolean tap(float x, float y, int count) {
        if (mutateIconBounds != null && mutateIconBounds.contains(x, y)) {
            if(showMerged) {
                showMerged = false;
            } else {
                showMerged = true;
                show(currentCard.getMergedCardsCollection(), 0, null);
            }
            return true;
        }
        if (flipIconBounds != null && flipIconBounds.contains(x, y)) {
            if (currentCard.isFaceDown() && currentCard.getBackup() != null) {
                if (currentCard.getBackup().hasBackSide() || currentCard.getBackup().isFlipCard() || currentCard.getBackup().isAdventureCard()) {
                    show(currentCard.getBackup());
                    return true;
                }
            }
            if (!showBackSide)
                showAltState = !showAltState;
            else
                showBackSide = !showBackSide;
            return true;
        }
        hide();
        showBackSide = false;
        showAltState = false;
        showMerged = false;
        return true;
    }

    @Override
    public boolean fling(float velocityX, float velocityY) {
        if (Math.abs(velocityX) > Math.abs(velocityY)) {
            incrementCard(velocityX > 0 ? -1 : 1);
            showBackSide = false;
            showAltState = false;
            return true;
        }
        if (velocityY > 0) {
            zoomMode = !zoomMode;
            showBackSide = false;
            showAltState = false;
            return true;
        }
        if (currentActivateAction != null && activateHandler != null) {
            hide();
            showBackSide = false;
            showAltState = false;
            activateHandler.activate(currentIndex);
            return true;
        }
        return false;
    }

    private void setOneCardView(boolean oneCardView0) {
        if (oneCardView == oneCardView0 || Forge.isLandscapeMode()) { return; } //don't allow changing this when in landscape mode

        oneCardView = oneCardView0;
        prefs.setPref(FPref.UI_SINGLE_CARD_ZOOM, oneCardView0);
        prefs.save();
    }

    @Override
    public boolean zoom(float x, float y, float amount) {
        totalZoomAmount += amount;

        if (totalZoomAmount >= REQ_AMOUNT) {
            setOneCardView(true);
            totalZoomAmount = 0;
        }
        else if (totalZoomAmount <= -REQ_AMOUNT) {
            setOneCardView(false);
            totalZoomAmount = 0;
        }
        return true;
    }

    @Override
    public boolean longPress(float x, float y) {
        setOneCardView(!oneCardView);
        return true;
    }

    @Override
    public void drawOverlay(Graphics g) {
        final GameView gameView = MatchController.instance.getGameView();

        float w = getWidth();
        float h = getHeight();
        float messageHeight = FDialog.MSG_HEIGHT;
        float AspectRatioMultiplier;
        switch (Forge.extrawide) {
            case "default":
                AspectRatioMultiplier = 3; //good for tablets with 16:10 or similar
                break;
            case "wide":
                AspectRatioMultiplier = 2.5f;
                break;
            case "extrawide":
                AspectRatioMultiplier = 2; //good for tall phones with 21:9 or similar
                break;
            default:
                AspectRatioMultiplier = 3;
                break;
        }
        float maxCardHeight = h - AspectRatioMultiplier * messageHeight; //maxheight of currently zoomed card

        float cardWidth, cardHeight, y;
        
        if (oneCardView && !Forge.isLandscapeMode()) {

            cardWidth = w;
            cardHeight = FCardPanel.ASPECT_RATIO * cardWidth;
            
            boolean rotateSplit = FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_ROTATE_SPLIT_CARDS);
            if (currentCard.isSplitCard() && rotateSplit) {
                // card will be rotated.  Make sure that the height does not exceed the width of the view
                if (cardHeight > Gdx.graphics.getWidth())
                {
                    cardHeight = Gdx.graphics.getWidth();
                    cardWidth = cardHeight / FCardPanel.ASPECT_RATIO;
                }
            }
        }
        else {
            
            cardWidth = w * 0.5f;
            cardHeight = FCardPanel.ASPECT_RATIO * cardWidth;

            float maxSideCardHeight = maxCardHeight * 5 / 7;
            if (cardHeight > maxSideCardHeight) { //prevent card overlapping message bars
                cardHeight = maxSideCardHeight;
                cardWidth = cardHeight / FCardPanel.ASPECT_RATIO;
            }
            y = (h - cardHeight) / 2;
            if (prevCard != null) {
                CardImageRenderer.drawZoom(g, prevCard, gameView, false, 0, y, cardWidth, cardHeight, getWidth(), getHeight(), false);
            }
            if (nextCard != null) {
                CardImageRenderer.drawZoom(g, nextCard, gameView, false, w - cardWidth, y, cardWidth, cardHeight, getWidth(), getHeight(), false);
            }

            cardWidth = w * 0.7f;
            cardHeight = FCardPanel.ASPECT_RATIO * cardWidth;
        }

        if (cardHeight > maxCardHeight) { //prevent card overlapping message bars
            cardHeight = maxCardHeight;
            cardWidth = cardHeight / FCardPanel.ASPECT_RATIO;
        }
        float x = (w - cardWidth) / 2;
        y = (h - cardHeight) / 2;
        if (zoomMode) {
            CardImageRenderer.drawZoom(g, currentCard, gameView, showBackSide? showBackSide : showAltState, x, y, cardWidth, cardHeight, getWidth(), getHeight(), true);
        } else {
            CardImageRenderer.drawDetails(g, currentCard, gameView, showBackSide? showBackSide : showAltState, x, y, cardWidth, cardHeight);
        }

        if (!showMerged) {
            if (mutateIconBounds != null) {
                float oldAlpha = g.getfloatAlphaComposite();
                try {
                    g.setAlphaComposite(0.6f);
                    drawIconBounds(g, mutateIconBounds, Forge.hdbuttons ? FSkinImage.HDLIBRARY : FSkinImage.LIBRARY, x, y, cardWidth, cardHeight);
                    g.setAlphaComposite(oldAlpha);
                } catch (Exception e) {
                    mutateIconBounds = null;
                    g.setAlphaComposite(oldAlpha);
                }
            } else if (flipIconBounds != null) {
                drawIconBounds(g, flipIconBounds, Forge.hdbuttons ? FSkinImage.HDFLIPCARD : FSkinImage.FLIPCARD, x, y, cardWidth, cardHeight);
            }
        } else if (flipIconBounds != null) {
            drawIconBounds(g, flipIconBounds, Forge.hdbuttons ? FSkinImage.HDFLIPCARD : FSkinImage.FLIPCARD, x, y, cardWidth, cardHeight);
        }

        if (currentActivateAction != null) {
            g.fillRect(FDialog.MSG_BACK_COLOR, 0, 0, w, messageHeight);
            g.drawText(Localizer.getInstance().getMessage("lblSwipeUpTo").replace("%s", currentActivateAction), FDialog.MSG_FONT, FDialog.MSG_FORE_COLOR, 0, 0, w, messageHeight, false, Align.center, true);
        }
        g.fillRect(FDialog.MSG_BACK_COLOR, 0, h - messageHeight, w, messageHeight);
        g.drawText(zoomMode ? Localizer.getInstance().getMessage("lblSwipeDownDetailView") : Localizer.getInstance().getMessage("lblSwipeDownPictureView"), FDialog.MSG_FONT, FDialog.MSG_FORE_COLOR, 0, h - messageHeight, w, messageHeight, false, Align.center, true);

        interrupt(false);
    }

    private void drawIconBounds(Graphics g, Rectangle iconBounds, FSkinImage skinImage, float x, float y, float cardWidth, float cardHeight) {
        float imageWidth = cardWidth / 2;
        float imageHeight = imageWidth * skinImage.getHeight() / skinImage.getWidth();
        iconBounds.set(x + (cardWidth - imageWidth) / 2, y + (cardHeight - imageHeight) / 2, imageWidth, imageHeight);
        g.drawImage(skinImage, iconBounds.x, iconBounds.y, iconBounds.width, iconBounds.height);
    }

    @Override
    protected void doLayout(float width, float height) {
    }

    public interface ActivateHandler {
        String getActivateAction(int index);
        void setSelectedIndex(int index);
        void activate(int index);
    }

    public void interrupt(boolean resume) {
        if (MatchController.instance.hasLocalPlayers())
            return;
        if(resume && MatchController.instance.isGamePaused()) {
            MatchController.instance.resumeMatch();
            return;
        }
        if(!MatchController.instance.isGamePaused())
            MatchController.instance.pauseMatch();
    }
}
