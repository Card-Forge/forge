package forge.screens.planarconquest;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Graphics;
import forge.assets.FImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.assets.FSkinTexture;
import forge.card.CardRenderer;
import forge.item.PaperCard;
import forge.planarconquest.ConquestPlane;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FOptionPane;
import forge.toolbox.FTimer;
import forge.util.collect.FCollectionView;

public class ConquestPlaneSelector extends FDisplayObject {
    private static final FSkinFont PLANE_NAME_FONT = FSkinFont.get(30);
    private static final Color BACK_COLOR = FSkinColor.fromRGB(1, 2, 2);
    private static final float MONITOR_TOP_MULTIPLIER = 15f / 315f;
    private static final float MONITOR_BOTTOM_MULTIPLIER = 23f / 315f;
    private static final float MONITOR_LEFT_MULTIPLIER = 19f / 443f;

    private static final ConquestPlane[] planes = ConquestPlane.values();

    private final FTimer timer = new FTimer(2.5f) {
        @Override
        protected void tick() {
            FCollectionView<PaperCard> planeCards = getSelectedPlane().getPlaneCards();
            if (++artIndex >= planeCards.size()) {
                artIndex = 0;
            }
            currentArt = CardRenderer.getCardArt(planeCards.get(artIndex));
        }
    };
    private int selectedIndex, artIndex;
    private FImage currentArt;

    public ConquestPlaneSelector() {
        reset();
    }

    public ConquestPlane getSelectedPlane() {
        return planes[selectedIndex];
    }

    public void activate() {
        timer.start();
    }

    public void deactivate() {
        timer.stop();
    }

    public void reset() {
        setSelectedIndex(0);
        timer.stop();
    }

    private void setSelectedIndex(int selectedIndex0) {
        selectedIndex = selectedIndex0;
        artIndex = 0;
        currentArt = CardRenderer.getCardArt(getSelectedPlane().getPlaneCards().get(artIndex));
        timer.restart();
    }

    @Override
    public boolean fling(float velocityX, float velocityY) {
        if (Math.abs(velocityX) > Math.abs(velocityY)) {
            if (velocityX > 0) {
                if (selectedIndex > 0) {
                    setSelectedIndex(selectedIndex - 1);
                }
                else {
                    setSelectedIndex(planes.length - 1);
                }
            }
            else if (selectedIndex < planes.length - 1) {
                setSelectedIndex(selectedIndex + 1);
            }
            else {
                setSelectedIndex(0);
            }
            return true;
        }
        return false;
    }

    @Override
    public void draw(Graphics g) {
        float w = getWidth();
        float h = getHeight();

        //draw background
        FImage background = FSkinTexture.BG_SPACE;
        float backgroundHeight = w * background.getHeight() / background.getWidth();
        g.fillRect(BACK_COLOR, 0, 0, w, h);
        g.drawImage(background, 0, h - backgroundHeight, w, backgroundHeight); //retain aspect ratio, remaining area will be covered by back color

        //determine monitor position
        FImage monitor = FSkinImage.PLANE_MONITOR;
        float monitorLeft = FOptionPane.PADDING / 2;
        float monitorWidth = w - 2 * monitorLeft;
        float monitorHeight = monitorWidth * monitor.getHeight() / monitor.getWidth();
        float monitorLeftOffset = monitorWidth * MONITOR_LEFT_MULTIPLIER;
        float monitorTopOffset = monitorHeight * MONITOR_TOP_MULTIPLIER;
        float monitorBottomOffset = monitorHeight * MONITOR_BOTTOM_MULTIPLIER;
        float monitorTop = monitorLeft + monitorLeftOffset - monitorTopOffset;

        //draw plane art inside monitor
        if (currentArt != null) {
            float x = monitorLeft + monitorLeftOffset - 1; //-1 to account for rounding error
            float y = monitorTop + monitorTopOffset - 1;
            float artWidth = monitorWidth - 2 * monitorLeftOffset + 2;
            float artHeight = monitorHeight - monitorTopOffset - monitorBottomOffset + 2;

            //scale up art to fill height of monitor while retain aspect ratio
            float fullArtWidth = artHeight * currentArt.getWidth() / currentArt.getHeight();
            g.startClip(x, y, artWidth, artHeight);
            g.drawImage(currentArt, x + (monitorWidth - fullArtWidth) / 2, y, fullArtWidth, artHeight);
            g.endClip();
        }

        //draw monitor so plane art remains within it
        g.drawImage(monitor, monitorLeft, monitorTop, monitorWidth, monitorHeight);

        //draw plane name
        float monitorBottom = monitorTop + monitorHeight;
        ConquestPlane plane = getSelectedPlane();
        g.drawText(plane.getName(), PLANE_NAME_FONT, Color.WHITE, monitorLeft, monitorBottom, monitorWidth, h - monitorBottom, false, HAlignment.CENTER, true);
    }
}
