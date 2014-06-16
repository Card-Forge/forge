package forge.card;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.game.card.Card;
import forge.item.PaperCard;
import forge.toolbox.FList;
import forge.toolbox.FOverlay;
import forge.util.Utils;

public class CardZoom extends FOverlay {
    private static final float TAB_HEIGHT = Utils.AVG_FINGER_HEIGHT;
    private static final FSkinFont FONT = FSkinFont.get(14);
    private static final CardZoom cardZoom = new CardZoom();
    private static Card card;
    private static boolean zoomMode = true;

    public static <T> void show(final PaperCard pc0) {
        show(Card.getCardForUi(pc0));
    }
    public static <T> void show(final Card card0) {
        card = card0;
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
            CardRenderer.drawZoom(g, card, w, y);
            g.fillRect(FList.PRESSED_COLOR, 0, y, x, h);
            foreColor = FList.FORE_COLOR;
        }
        else {
            foreColor = FList.FORE_COLOR.alphaColor(ALPHA_COMPOSITE);
        }
        g.drawText("Zoom", FONT, foreColor, 0, y, x, h, false, HAlignment.CENTER, true);

        if (!zoomMode) {
            CardRenderer.drawDetails(g, card, w, y);
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
