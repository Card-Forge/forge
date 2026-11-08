package forge.screens.match.views;

import java.util.List;

import forge.Forge;
import forge.Graphics;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.screens.match.MatchController;
import forge.screens.match.arena.ArenaBoard;
import forge.screens.match.arena.ArenaCards;
import forge.screens.match.arena.ArenaMatchLayout;
import forge.screens.match.MatchScreen;
import forge.toolbox.FCardPanel;
import forge.toolbox.FDisplayObject;
import forge.util.collect.FCollectionView;

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

    /** Whether this is the local player's hand on the Arena board: a fan along the bottom of the window. */
    public boolean isArenaHand() {
        return zoneType == ZoneType.Hand && ArenaMatchLayout.applies() && MatchController.getView() != null
                && MatchController.getView().getBottomPlayerPanel() != null && MatchController.getView().getBottomPlayerPanel().getPlayer() == player;
    }

    private CardAreaPanel arenaRaised; //the hovered card of the Arena fan, raised above the others

    @Override
    public void draw(Graphics g) {
        if (!isArenaHand() && ArenaMatchLayout.applies() && forge.screens.match.arena.ArenaCardBrowser.isSelectionShowing()) {
            return; //the Arena card browser shows these cards over the board, the strip would only show through it
        }
        super.draw(g);
    }

    @Override
    protected void drawBackground(Graphics g) {
        if (isArenaHand()) {
            //the hovered card rises and its neighbours part: lay the fan out again when the pointer moves to another card
            CardAreaPanel hovered = null;
            for (CardAreaPanel p : cardPanels.get()) {
                if (p.isHovered()) {
                    hovered = p;
                    break;
                }
            }
            if (hovered != arenaRaised) {
                arenaRaised = hovered;
                revalidate();
            }
            return;
        }
        if (ArenaMatchLayout.applies()) { //a zone the game opened: a strip on glass over the board
            forge.screens.match.arena.ArenaArt.glassPanel(g, 0, 0, getWidth(), getHeight());
            return;
        }
        super.drawBackground(g);
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

    private boolean panImpl(float x, float y, float deltaX, float deltaY, boolean moreVertical) {
        if (revealedPanel == null) { //if no overlapping panels, just pan scroll as normal
            return super.pan(x, y, deltaX, deltaY, moreVertical); //base class tracks the card under the finger
        }
        /*if (Forge.isLandscapeMode()) {
            updateDragPanel(x, y);
        }*/
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
        if (Forge.isLandscapeMode() && layoutVerticallyForLandscapeMode()) {
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
        return !VPlayerPanel.isAltZoneLayout() && !ArenaMatchLayout.applies(); //an opened zone is a strip on the Arena board
    }

    @Override
    protected boolean isHorizontalStrip() {
        return Forge.isLandscapeMode() && !layoutVerticallyForLandscapeMode();
    }

    @Override
    protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
        if (!isVisible()) { //if zone not visible, don't spend time laying out cards
            return new ScrollBounds(visibleWidth, visibleHeight);
        }

        orderedCards.get().clear();

        if (isArenaHand()) {
            return layoutArenaFan(visibleWidth, visibleHeight);
        }
        if (Forge.isLandscapeMode() && layoutVerticallyForLandscapeMode()) {
            return layoutAndGetScrollBoundsLandscape(visibleWidth, visibleHeight);
        }

        float x = 0;
        float y = 0;
        float cardHeight = visibleHeight;
        float cardWidth = getCardWidth(cardHeight);
        float dx = cardWidth;

        float totalWidth = cardWidth * cardPanels.get().size();
        final boolean centered = VPlayerPanel.isCompactLayout() && totalWidth < visibleWidth; //compact popup zones center their cards
        if (centered) {
            x = (visibleWidth - totalWidth) / 2;
        }
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

        return new ScrollBounds(centered ? visibleWidth : x, visibleHeight);
    }

    /**
     * The Arena hand (port of the Swing CardArea fan): one centred, overlapping row on a shallow arc, sunk half below
     * the window's bottom edge, each card leaning out from the middle; the hovered card rises (and straightens) and its
     * neighbours part. Each panel reaches down over the strip its card rose from, so a still pointer keeps it raised.
     */
    private ScrollBounds layoutArenaFan(float areaWidth, float areaHeight) {
        revealedPanel = null;
        final java.util.List<CardAreaPanel> panels = cardPanels.get();
        final int count = panels.size();
        final float gutter = Math.max(2f, areaHeight * 0.02f);
        final float sideRoom = Math.round(areaHeight * 0.3f) + Math.round(areaWidth * 0.055f);
        final float left = gutter + sideRoom, right = areaWidth - gutter - sideRoom;
        final float availableWidth = Math.max(1f, right - left);
        final float costRow = Math.round(areaHeight * ArenaBoard.FAN_COST_ROW);
        final float availableHeight = areaHeight - 2 * gutter - costRow;
        float cardHeight = Math.round(availableHeight / (1f + ArenaBoard.FAN_LIFT - ArenaBoard.FAN_SINK));
        float cardWidth = Math.round(cardHeight / FCardPanel.ASPECT_RATIO);
        final float minWidth = Math.round(cardWidth * 0.45f);
        while (cardWidth > minWidth && Math.max(0, count - 1) * cardWidth * ArenaBoard.FAN_MIN_STEP + (count > 0 ? cardWidth : 0) > availableWidth) {
            cardWidth--;
        }
        cardHeight = Math.round(cardWidth * FCardPanel.ASPECT_RATIO);
        float step = cardWidth * ArenaBoard.FAN_STEP;
        if (count > 1) {
            step = Math.max(1f, Math.min(step, (availableWidth - cardWidth) / (count - 1)));
        }
        final float rowWidth = count == 0 ? 0f : (count - 1) * step + cardWidth;
        final float lift = Math.round(cardHeight * ArenaBoard.FAN_LIFT);
        final float arc = Math.round(cardHeight * ArenaBoard.FAN_ARC);
        final float baseY = gutter + costRow + lift;
        float x = Math.max(left, (left + right) / 2f - rowWidth / 2f);
        final CardAreaPanel raised = arenaRaised != null && panels.contains(arenaRaised) ? arenaRaised : null;
        final int raisedIndex = raised == null ? -1 : panels.indexOf(raised);
        final float spread = raised == null ? 0f : Math.round(cardWidth * 0.42f);
        //later cards overlap earlier ones; the raised one is above them all
        clearChildren();
        for (int i = 0; i < count; i++) {
            final CardAreaPanel panel = panels.get(i);
            orderedCards.get().add(panel.getCard());
            final float t = count == 1 ? 0f : (i / (count - 1f)) * 2f - 1f;
            float y = baseY + Math.round(arc * t * t);
            final boolean up = panel == raised;
            if (up) {
                y -= lift;
            }
            final float lean = up || count < 2 ? 0f
                    : Math.max(-ArenaBoard.FAN_MAX_LEAN, Math.min(ArenaBoard.FAN_MAX_LEAN, (i - (count - 1) / 2f) * ArenaBoard.FAN_LEAN));
            final float part = raisedIndex < 0 || i == raisedIndex ? 0f : i < raisedIndex ? -spread : spread;
            panel.setBounds(Math.round(x + part), y, cardWidth, cardHeight + (up ? lift : 0f));
            final ArenaCards.State st = panel.getArenaState();
            st.fanLean = lean;
            st.raised = up;
            st.cardHeight = cardHeight;
            if (!up) {
                add(panel);
            }
            x += step;
        }
        if (raised != null) {
            add(raised);
        }
        return new ScrollBounds(areaWidth, areaHeight);
    }

    @Override
    public boolean pan(float x, float y, float deltaX, float deltaY, boolean moreVertical) {
        if (isArenaHand()) { //the fan always fits; the wheel walks the raised card along it, so each can be read in turn
            final java.util.List<CardAreaPanel> panels = cardPanels.get();
            if (!panels.isEmpty() && moreVertical && deltaY != 0 && !com.badlogic.gdx.Gdx.input.isTouched()) {
                int i = arenaRaised == null ? -1 : panels.indexOf(arenaRaised);
                i = Math.max(0, Math.min(panels.size() - 1, i < 0 ? panels.size() / 2 : i + (deltaY < 0 ? 1 : -1)));
                for (CardAreaPanel p : panels) {
                    p.setHovered(p == panels.get(i));
                }
                MatchScreen.setPotentialListener(java.util.Arrays.asList((forge.toolbox.FDisplayObject) panels.get(i)));
                panels.get(i).setHovered(true);
            }
            return true;
        }
        return panImpl(x, y, deltaX, deltaY, moreVertical);
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
        if (isArenaHand()) { //the raised card rises out of the strip; its light reaches past it
            g.startClip(-getLeft() - 4000, -getTop() - 4000, 8000 + getWidth(), 8000 + getHeight());
            return;
        }
        if (Forge.isLandscapeMode()) {
            g.startClip(0, 0, getWidth(), getHeight());
        }
        else {
            super.startClip(g);
        }
    }
}
