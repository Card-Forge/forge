package forge.screens.match.views;

import java.util.ArrayList;

import forge.FThreads;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.toolbox.FCardPanel;
import forge.toolbox.FDisplayObject;

public class VZoneDisplay extends VCardDisplayArea {
    private final PlayerView player;
    private final ZoneType zoneType;
    private FCardPanel revealedPanel;

    public VZoneDisplay(PlayerView player0, ZoneType zoneType0) {
        player = player0;
        zoneType = zoneType0;
    }

    public ZoneType getZoneType() {
        return zoneType;
    }

    @Override
    public void update() {
        FThreads.invokeInEdtNowOrLater(updateRoutine);
    }

    private final Runnable updateRoutine = new Runnable() {
        @Override
        public void run() {
            refreshCardPanels(player.getCards(zoneType));
        }
    };

    @Override
    public void buildTouchListeners(float screenX, float screenY, ArrayList<FDisplayObject> listeners) {
        super.buildTouchListeners(screenX, screenY, listeners);

        if (revealedPanel != null) {
            updateRevealedPanel(screenToLocalX(screenX), screenToLocalY(screenY));
        }
    }

    @Override
    public boolean pan(float x, float y, float deltaX, float deltaY, boolean moreVertical) {
        if (revealedPanel == null) { //if no overlapping panels, just pan scroll as normal
            return super.pan(x, y, deltaX, deltaY, moreVertical);
        }
        updateRevealedPanel(x, y);
        return true;
    }

    private void updateRevealedPanel(float x, float y) {
        if (revealedPanel.contains(x, y)) { return; }

        int idx = -1;
        for (int i = getChildCount() - 2; i >= 0; i--) {
            final FDisplayObject cardPanel = getChildAt(i);
            if (cardPanel.contains(x, y)) {
                idx = cardPanels.indexOf(cardPanel);
                break;
            }
        }
        if (idx >= 0) { //cascade cards back from revealed panel
            clearChildren();
            int maxIdx = cardPanels.size() - 1;
            int offset = Math.max(idx, maxIdx - idx);
            for (int i = offset; i > 0; i--) {
                int idx1 = idx - i;
                int idx2 = idx + i;
                if (idx1 >= 0) {
                    add(cardPanels.get(idx1));
                }
                if (idx2 <= maxIdx) {
                    add(cardPanels.get(idx2));
                }
            }
            revealedPanel = cardPanels.get(idx);
            add(revealedPanel);
        }
    }

    @Override
    public void clear() {
        revealedPanel = null;
        super.clear();
    }

    @Override
    protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
        orderedCards.clear();

        float x = 0;
        float y = 0;
        float cardHeight = visibleHeight;
        float cardWidth = getCardWidth(cardHeight);
        float dx = cardWidth;

        float totalWidth = cardWidth * cardPanels.size();
        if (totalWidth > visibleWidth && totalWidth <= visibleWidth * 2) {
            //allow overlapping cards up to one half of the card,
            //otherwise don't overlap and allow scrolling horizontally
            dx *= (visibleWidth - cardWidth) / (totalWidth - cardWidth);
            dx += FCardPanel.PADDING / cardPanels.size(); //make final card go right up to right edge of screen
            if (revealedPanel == null) {
                revealedPanel = cardPanels.get(cardPanels.size() - 1);
            }
        }
        else {
            revealedPanel = null;
        }

        for (CardAreaPanel cardPanel : cardPanels) {
            orderedCards.add(cardPanel.getCard());
            cardPanel.setBounds(x, y, cardWidth, cardHeight);
            x += dx;
        }

        return new ScrollBounds(x, visibleHeight);
    }
}
