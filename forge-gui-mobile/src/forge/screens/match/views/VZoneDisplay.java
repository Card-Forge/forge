package forge.screens.match.views;

import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;

import forge.Forge;
import forge.Graphics;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.screens.match.MatchController;
import forge.toolbox.FCardPanel;
import forge.toolbox.FDisplayObject;
import forge.util.collect.FCollectionView;

public class VZoneDisplay extends VCardDisplayArea {
    private final PlayerView player;
    private final ZoneType zoneType;
    private static final float SELECTION_BOX_THICKNESS = 3f;
    private FCardPanel revealedPanel;
    private CardAreaPanel dragPanel; //card currently under the finger while touch dragging, null when not dragging

    public VZoneDisplay(PlayerView player0, ZoneType zoneType0) {
        player = player0;
        zoneType = zoneType0;
    }

    public ZoneType getZoneType() {
        return zoneType;
    }

    @Override
    public void update() {
        FCollectionView<CardView> cards = player.getCards(zoneType);
        refreshCardPanels(MatchController.instance.isNetGame() ? cards.threadSafeIterable() : cards);
    }

    @Override
    public void buildTouchListeners(float screenX, float screenY, List<FDisplayObject> listeners) {
        super.buildTouchListeners(screenX, screenY, listeners);

        if (revealedPanel != null) {
            float x = screenToLocalX(screenX);
            float y = screenToLocalY(screenY);
            if (revealedPanel.contains(x, y)) { return; }

            int idx = cardPanels.get().size() - 1;
            for (int i = getChildCount() - 2; i >= 0; i--) {
                final FDisplayObject cardPanel = getChildAt(i);
                if (cardPanel.contains(x, y)) {
                    idx = cardPanels.get().indexOf(cardPanel);
                    break;
                }
            }
            setRevealedPanel(idx);
        }
    }

    /**
     * @return the card under the finger while touch dragging over this zone in landscape mode, or null if not dragging.
     * The player panel uses this to show a larger card display next to the zone.
     */
    public CardView getDragCard() {
        return dragPanel == null ? null : dragPanel.getCard();
    }

    private void updateDragPanel(float x, float y) {
        final List<CardAreaPanel> panels = cardPanels.get();
        for (int i = panels.size() - 1; i >= 0; i--) { //later panels are drawn on top, so check them first
            final CardAreaPanel panel = panels.get(i);
            if (panel.contains(x, y)) {
                dragPanel = panel; //if finger crosses a gap between cards, keep the last card selected
                break;
            }
        }
        Gdx.graphics.requestRendering();
    }

    private void clearDragPanel() {
        if (dragPanel != null) {
            dragPanel = null;
            Gdx.graphics.requestRendering();
        }
    }

    @Override
    public boolean panStop(float x, float y) {
        clearDragPanel();
        return false; //let any other listeners finish handling the pan
    }

    @Override
    public boolean release(float x, float y) {
        clearDragPanel();
        return false;
    }

    @Override
    public void setVisible(boolean b0) {
        if (!b0) {
            dragPanel = null;
        }
        super.setVisible(b0);
    }

    @Override
    protected void drawOverlay(Graphics g) {
        super.drawOverlay(g);
        if (dragPanel == null || !Forge.isLandscapeMode()) { return; }

        //surround the selected card with a box
        final float padding = FCardPanel.PADDING;
        float w = dragPanel.getWidth() - 2 * padding;
        final float h = dragPanel.getHeight() - 2 * padding;
        if (w == h) { //adjust width if needed to match how the card itself is drawn
            w = h / FCardPanel.ASPECT_RATIO;
        }
        g.drawRect(SELECTION_BOX_THICKNESS, Color.LIME, dragPanel.getLeft() + padding, dragPanel.getTop() + padding, w, h);
    }

    @Override
    public boolean pan(float x, float y, float deltaX, float deltaY, boolean moreVertical) {
        if (Forge.isLandscapeMode()) {
            updateDragPanel(x, y);
        }
        if (revealedPanel == null) { //if no overlapping panels, just pan scroll as normal
            return super.pan(x, y, deltaX, deltaY, moreVertical);
        }
        int idx = cardPanels.get().size() - 1;
        for (int i = idx - 1; i >= 0; i--) {
            if (cardPanels.get().get(i).contains(x, y)) {
                idx = i;
                break;
            }
        }
        setRevealedPanel(idx);
        return true;
    }

    private void setRevealedPanel(int idx) {
        if (idx >= 0 && idx < cardPanels.get().size())
            revealedPanel = cardPanels.get().get(idx);
        else
            return;

        clearChildren();
        if (Forge.isLandscapeMode()) {
            //for landscape mode, just show revealed card on top
            for (CardAreaPanel cardPanel : cardPanels.get()) {
                if (cardPanel != revealedPanel) {
                    add(cardPanel);
                }
            }
        }
        else {
            //for portrait mode, cascade cards back from revealed panel
            int maxIdx = cardPanels.get().size() - 1;
            int offset = Math.max(idx, maxIdx - idx);
            for (int i = offset; i > 0; i--) {
                int idx1 = idx - i;
                int idx2 = idx + i;
                if (idx1 >= 0) {
                    add(cardPanels.get().get(idx1));
                }
                if (idx2 <= maxIdx) {
                    add(cardPanels.get().get(idx2));
                }
            }
        }
        add(revealedPanel);
    }

    @Override
    public void setSelectedIndex(int index) {
        if (revealedPanel == null) {
            super.setSelectedIndex(index);
        }
        setRevealedPanel(index);
    }

    @Override
    public void clear() {
        revealedPanel = null;
        super.clear();
    }

    protected boolean layoutVerticallyForLandscapeMode() {
        return true;
    }

    @Override
    protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
        if (!isVisible()) { //if zone not visible, don't spend time laying out cards
            return new ScrollBounds(visibleWidth, visibleHeight);
        }

        orderedCards.get().clear();

        if (Forge.isLandscapeMode() && layoutVerticallyForLandscapeMode()) {
            return layoutAndGetScrollBoundsLandscape(visibleWidth, visibleHeight);
        }

        float x = 0;
        float y = 0;
        float cardHeight = visibleHeight;
        float cardWidth = getCardWidth(cardHeight);
        float dx = cardWidth;

        float totalWidth = cardWidth * cardPanels.get().size();
        if (totalWidth > visibleWidth && totalWidth <= visibleWidth * 2) {
            //allow overlapping cards up to one half of the card,
            //otherwise don't overlap and allow scrolling horizontally
            dx *= (visibleWidth - cardWidth) / (totalWidth - cardWidth);
            dx += FCardPanel.PADDING / cardPanels.get().size(); //make final card go right up to right edge of screen
            if (revealedPanel == null) {
                revealedPanel = cardPanels.get().get(cardPanels.get().size() - 1);
            }
        }
        else {
            revealedPanel = null;
        }

        for (CardAreaPanel cardPanel : cardPanels.get()) {
            orderedCards.get().add(cardPanel.getCard());
            cardPanel.setBounds(x, y, cardWidth, cardHeight);
            x += dx;
        }

        return new ScrollBounds(x, visibleHeight);
    }

    private ScrollBounds layoutAndGetScrollBoundsLandscape(float visibleWidth, float visibleHeight) {
        float x = 0;
        float y = 0;
        float cardWidth = visibleWidth / 2;
        float cardHeight = getCardHeight(cardWidth);
        float dy = cardHeight;
        float scrollHeight;

        int rowCount = (int)Math.ceil((float)cardPanels.get().size() / 2f);
        float totalHeight = cardHeight * rowCount;
        if (totalHeight > visibleHeight && totalHeight <= visibleHeight * 3) {
            //allow overlapping cards up to one third of the card,
            //otherwise don't overlap and allow scrolling vertically
            dy *= (visibleHeight - cardHeight) / (totalHeight - cardHeight);
            dy += FCardPanel.PADDING / rowCount; //make final card go right up to right edge of screen
            if (revealedPanel == null) {
                revealedPanel = cardPanels.get().get(cardPanels.get().size() - 1);
            }
            scrollHeight = visibleHeight;
        }
        else {
            revealedPanel = null;
            scrollHeight = rowCount * dy;
        }

        for (CardAreaPanel cardPanel : cardPanels.get()) {
            orderedCards.get().add(cardPanel.getCard());
            cardPanel.setBounds(x, y, cardWidth, cardHeight);
            if (orderedCards.get().size() % 2 == 0) {
                x = 0;
                y += dy;
            }
            else {
                x += cardWidth;
            }
        }

        return new ScrollBounds(visibleWidth, scrollHeight);
    }

    @Override
    protected void startClip(Graphics g) {
        if (Forge.isLandscapeMode()) {
            g.startClip(0, 0, getWidth(), getHeight());
        }
        else {
            super.startClip(g);
        }
    }
}
