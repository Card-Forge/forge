package forge.card;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Graphics;
import forge.ImageKeys;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.game.card.CardView;
import forge.item.IPaperCard;
import forge.item.InventoryItem;
import forge.toolbox.FList;
import forge.toolbox.FOverlay;
import forge.util.FCollectionView;
import forge.util.Utils;

public class CardZoom extends FOverlay {
    private static final float TAB_HEIGHT = Utils.AVG_FINGER_HEIGHT;
    private static final FSkinFont FONT = FSkinFont.get(14);
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
        updateVisibleCards();
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

    private static void updateVisibleCards() {
        currentCard = getCardView(items.get(currentIndex));
        prevCard = currentIndex > 0 ? getCardView(items.get(currentIndex - 1)) : null;
        nextCard = currentIndex < items.size() - 1 ? getCardView(items.get(currentIndex + 1)) : null;
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
        if (y >= getHeight() - TAB_HEIGHT && zoomMode != (x < getWidth() / 2)) {
            zoomMode = !zoomMode; //handle toggling between zoom and details
            return true;
        }
        hide(); //hide if uncovered area tapped
        return true;
    }

    @Override
    public boolean fling(float velocityX, float velocityY) {
        //toggle between Zoom and Details with a quick horizontal fling action
        if (Math.abs(velocityX) > Math.abs(velocityY)) {
            zoomMode = !zoomMode;
            return true;
        }
        return false;
    }

    @Override
    public void drawOverlay(Graphics g) {
        float w = getWidth();
        float h = TAB_HEIGHT;
        float x = w / 2;
        float y = getHeight() - h;

        //draw zoom/details options
        FSkinColor foreColor;
        if (zoomMode) {
            if (!CardRenderer.drawZoom(g, currentCard, w, y)) {
                CardRenderer.drawDetails(g, currentCard, w, y); //draw details if can't draw zoom
            }
            g.fillRect(FList.PRESSED_COLOR, 0, y, x, h);
            foreColor = FList.FORE_COLOR;
        }
        else {
            foreColor = FList.FORE_COLOR.alphaColor(ALPHA_COMPOSITE);
        }
        g.drawText("Zoom", FONT, foreColor, 0, y, x, h, false, HAlignment.CENTER, true);

        if (!zoomMode) {
            CardRenderer.drawDetails(g, currentCard, w, y);
            g.fillRect(FList.PRESSED_COLOR, x, y, w - x, h);
            foreColor = FList.FORE_COLOR;
        }
        else {
            foreColor = FList.FORE_COLOR.alphaColor(ALPHA_COMPOSITE);
        }
        g.drawText("Details", FONT, foreColor, x, y, w - x, h, false, HAlignment.CENTER, true);

        g.drawLine(1, FList.LINE_COLOR, 0, y, w, y);
        g.drawLine(1, FList.LINE_COLOR, x, y, x, y + h);
        y += h;
        g.drawLine(1, FList.LINE_COLOR, 0, y, w, y);
    }

    @Override
    protected void doLayout(float width, float height) {
    }
}
