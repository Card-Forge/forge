package forge.card;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Graphics;
import forge.ImageKeys;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinColor.Colors;
import forge.game.card.CardView;
import forge.item.IPaperCard;
import forge.item.InventoryItem;
import forge.screens.FScreen;
import forge.toolbox.FCardPanel;
import forge.toolbox.FOverlay;
import forge.util.Callback;
import forge.util.FCollectionView;

public class CardZoom extends FOverlay {
    private static final FSkinFont MSG_FONT = FSkinFont.get(12);
    private static final FSkinColor MSG_FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT).alphaColor(0.9f);
    private static final FSkinColor MSG_BACK_COLOR = FScreen.Header.BACK_COLOR.alphaColor(0.75f);

    private static final CardZoom cardZoom = new CardZoom();
    private static List<?> items;
    private static int currentIndex;
    private static CardView currentCard, prevCard, nextCard;
    private static boolean zoomMode = true;
    private static Callback<Integer> onActivate;

    public static void show(Object item) {
        List<Object> items0 = new ArrayList<Object>();
        items0.add(item);
        show(items0, 0, null);
    }
    public static void show(FCollectionView<?> items0, int currentIndex0, Callback<Integer> onActivate0) {
        show((List<?>)items0, currentIndex0, onActivate0);
    }
    public static void show(final List<?> items0, int currentIndex0, Callback<Integer> onActivate0) {
        items = items0;
        onActivate = onActivate0;
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
        if (velocityY > 0) {
            zoomMode = !zoomMode;
            return true;
        }
        if (onActivate != null) {
            hide();
            onActivate.run(currentIndex);
            return true;
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

        cardWidth = w * 0.7f;
        cardHeight = FCardPanel.ASPECT_RATIO * cardWidth;
        if (zoomMode) {
            CardRenderer.drawZoom(g, currentCard, (w - cardWidth) / 2, (h - cardHeight) / 2, cardWidth, cardHeight);
        }
        else {
            CardImageRenderer.drawDetails(g, currentCard, (w - cardWidth) / 2, (h - cardHeight) / 2, cardWidth, cardHeight);
        }

        float messageHeight = MSG_FONT.getCapHeight() * 2.5f;
        if (onActivate != null) {
            g.fillRect(MSG_BACK_COLOR, 0, 0, w, messageHeight);
            g.drawText("Swipe up to activate card", MSG_FONT, MSG_FORE_COLOR, 0, 0, w, messageHeight, false, HAlignment.CENTER, true);
        }
        g.fillRect(MSG_BACK_COLOR, 0, h - messageHeight, w, messageHeight);
        g.drawText("Swipe down to switch to " + (zoomMode ? "detail" : "picture") + " view", MSG_FONT, MSG_FORE_COLOR, 0, h - messageHeight, w, messageHeight, false, HAlignment.CENTER, true);
    }

    @Override
    protected void doLayout(float width, float height) {
    }
}
