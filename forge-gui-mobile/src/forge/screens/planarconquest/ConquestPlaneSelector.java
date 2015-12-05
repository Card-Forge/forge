package forge.screens.planarconquest;

import forge.Graphics;
import forge.assets.FImage;
import forge.card.CardRenderer;
import forge.item.PaperCard;
import forge.planarconquest.ConquestPlane;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FTimer;
import forge.util.collect.FCollectionView;

public class ConquestPlaneSelector extends FDisplayObject {
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

        if (currentArt != null) {
            float artHeight = h * 0.5f;
            float artWidth = artHeight * currentArt.getWidth() / currentArt.getHeight();
            float y = (h - artHeight) / 2;
            g.startClip(0, y, w, artHeight);
            g.drawImage(currentArt, (w - artWidth) / 2, y, artWidth, artHeight);
            g.endClip();
        }
    }
}
