package forge.screens.planarconquest;

import com.badlogic.gdx.graphics.Color;

import forge.Graphics;
import forge.assets.FImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinImage;
import forge.assets.FSkinTexture;
import forge.card.CardRenderer;
import forge.item.PaperCard;
import forge.planarconquest.ConquestPlane;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FTimer;
import forge.util.collect.FCollectionView;

public class ConquestPlaneSelector extends FDisplayObject {
    private static final Color BACK_COLOR = FSkinColor.fromRGB(1, 2, 2);
    private static final float MONITOR_TOP_MULTIPLIER = 15f / 315f;
    private static final float MONITOR_BOTTOM_MULTIPLIER = 23f / 315f;
    private static final float MONITOR_LEFT_MULTIPLIER = 19f / 443f;

    private static final ConquestPlane[] planes = ConquestPlane.values();

    private final FTimer timer = new FTimer(2.5f) {
        @Override
        protected void tick() {
            FCollectionView<PaperCard> planeCards = getSelectedPlane().getPlaneCards();
            if (++artIndex == planeCards.size()) {
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
        selectedIndex = 0;
        artIndex = 0;
        currentArt = CardRenderer.getCardArt(getSelectedPlane().getPlaneCards().get(artIndex));
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
        float monitorHeight = w * monitor.getHeight() / monitor.getWidth();
        float monitorTop = (h - monitorHeight) / 2;

        //draw plane art inside monitor
        if (currentArt != null) {
            float monitorTopOffset = monitorHeight * MONITOR_TOP_MULTIPLIER;
            float monitorBottomOffset = monitorHeight * MONITOR_BOTTOM_MULTIPLIER;
            float x = w * MONITOR_LEFT_MULTIPLIER;
            float y = monitorTop + monitorTopOffset;
            float artWidth = w - 2 * x;
            float artHeight = monitorHeight - monitorTopOffset - monitorBottomOffset;

            //scale up art to fill height of monitor while retain aspect ratio
            float fullArtWidth = artHeight * currentArt.getWidth() / currentArt.getHeight();
            g.startClip(x, y, artWidth, artHeight);
            g.drawImage(currentArt, x + (w - fullArtWidth) / 2, y, fullArtWidth, artHeight);
            g.endClip();
        }

        //draw monitor so plane art remains within it
        g.drawImage(monitor, 0, monitorTop, w, monitorHeight);
    }
}
