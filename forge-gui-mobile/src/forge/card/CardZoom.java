package forge.card;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import forge.Graphics;
import forge.ImageKeys;
import forge.game.card.CardView;
import forge.item.IPaperCard;
import forge.item.InventoryItem;
import forge.toolbox.FCardPanel;
import forge.toolbox.FOverlay;
import forge.util.FCollectionView;
import forge.util.Utils;

public class CardZoom extends FOverlay {
    private static final float TAB_HEIGHT = Utils.AVG_FINGER_HEIGHT;
    private static final CardZoom cardZoom = new CardZoom();
    private static List<?> items;
    private static int currentIndex;
    private static CardView currentCard, prevCard, nextCard;
    private static boolean zoomMode = true;

    public static <T> void show(final Object card) {
        List<Object> cards0 = new ArrayList<Object>();
        show(cards0, 0);
    }
    public static <T> void show(final FCollectionView<?> items0, int currentIndex0) {
        show((List<?>)items0, currentIndex0);
    }
    public static <T> void show(final List<?> items0, int currentIndex0) {
        items = items0;
        currentIndex = currentIndex0;
        currentCard = getCardView(items.get(currentIndex));
        prevCard = currentIndex > 0 ? getCardView(items.get(currentIndex - 1)) : null;
        nextCard = currentIndex < items.size() - 1 ? getCardView(items.get(currentIndex + 1)) : null;
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
    }

    private static CardView getCardView(Object item) {
        if (item instanceof Entry) {
            item = ((Entry<?, ?>)item).getKey();
        }
        if (item instanceof CardView) {
            return (CardView)item;
        }
        if (item instanceof IPaperCard) {
            return CardView.getCardForUi((IPaperCard)item);
        }
        if (item instanceof InventoryItem) {
            InventoryItem ii = (InventoryItem)item;
            return new CardView(-1, ii.getName(), null, ImageKeys.getImageKey(ii, false));
        }
        return new CardView(-1, item.toString());
    }

    @Override
    public boolean tap(float x, float y, int count) {
        hide();
        return true;
    }

    @Override
    public boolean fling(float velocityX, float velocityY) {
        //toggle between Zoom and Details with a quick horizontal fling action
        if (Math.abs(velocityX) > Math.abs(velocityY)) {
            incrementCard(velocityX > 0 ? -1 : 1);
            return true;
        }
        else {
            if (velocityY > 0) {
                zoomMode = !zoomMode;
            }
            else {
                
            }
        }
        return false;
    }

    @Override
    public void drawOverlay(Graphics g) {
        float w = getWidth();
        float h = getHeight();

        float cardWidth = w * 0.5f;
        float cardHeight = FCardPanel.ASPECT_RATIO * cardWidth;
        if (prevCard != null) {
            CardRenderer.drawZoom(g, prevCard, 0, (h - cardHeight) / 2, cardWidth, cardHeight);
        }
        if (nextCard != null) {
            CardRenderer.drawZoom(g, nextCard, w - cardWidth, (h - cardHeight) / 2, cardWidth, cardHeight);
        }

        cardWidth = w * 0.75f;
        cardHeight = FCardPanel.ASPECT_RATIO * cardWidth;
        if (zoomMode) {
            CardRenderer.drawZoom(g, currentCard, (w - cardWidth) / 2, (h - cardHeight) / 2, cardWidth, cardHeight);
        }
        else {
            CardImageRenderer.drawDetails(g, currentCard, (w - cardWidth) / 2, (h - cardHeight) / 2, cardWidth, cardHeight);
        }
    }

    @Override
    protected void doLayout(float width, float height) {
    }
}
