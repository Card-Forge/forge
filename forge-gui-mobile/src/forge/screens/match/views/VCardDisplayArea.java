package forge.screens.match.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Sets;
import forge.Forge;
import forge.Graphics;
import forge.animation.FlipOntoBattlefieldAnimation;
import forge.card.CardRenderer.CardStackPosition;
import forge.card.CardZoom;
import forge.card.CardZoom.ActivateHandler;
import forge.game.card.CardView;
import forge.game.zone.ZoneType;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.screens.match.CardFlightOverlay;
import forge.screens.match.MatchController;
import forge.screens.match.MatchScreen;
import forge.screens.match.arena.ArenaBoard;
import forge.screens.match.arena.ArenaCards;
import forge.screens.match.arena.ArenaDialogOverlay;
import forge.screens.match.arena.ArenaMatchLayout;
import forge.toolbox.FCardPanel;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FSelectionBox;
import forge.util.ThreadUtil;
import forge.util.Utils;
import io.sentry.Sentry;

public abstract class VCardDisplayArea extends VDisplayArea implements ActivateHandler {
    private static final float CARD_STACK_OFFSET = 0.2f;
    protected Supplier<List<CardView>> orderedCards = Suppliers.memoize(ArrayList::new);
    protected Supplier<List<CardAreaPanel>> cardPanels = Suppliers.memoize(ArrayList::new);
    // Cards shown only as informational exile ghosts here, so the zoom carousel doesn't act on them
    private final Supplier<Set<Integer>> infoGhostCardIds = Suppliers.memoize(HashSet::new);
    private boolean rotateCards180;
    // what the previous refresh showed. The row's cardPanels is already emptied before refreshCardPanels runs
    // (so it can't be used to find departed cards), which is why we keep our own list.
    private final List<CardAreaPanel> shownLastRefresh = new ArrayList<>();
    private final List<CardAreaPanel> shownScratch = new ArrayList<>();
    private final Set<CardView> knownScratch = new HashSet<>();
    private final Set<CardView> currentScratch = new HashSet<>();
    private int shownGeneration;
    private static int generation; // bumped on every new game so the old board never counts as "departures"
    private static final float SELECTION_BOX_THICKNESS = 3f;
    private CardAreaPanel dragPanel; //card currently under the finger while touch dragging, null when not dragging
    private static long lastDragMillis;
    private static boolean dragging;

    public static boolean isDragGestureActive() {
        return dragging || System.currentTimeMillis() - lastDragMillis < 250;
    }
    public Iterable<CardView> getOrderedCards() {
        return orderedCards.get();
    }

    public Iterable<CardAreaPanel> getCardPanels() {
        return cardPanels.get();
    }
    /** @return the card under the finger while touch dragging over this area in landscape mode, or null */
    public CardView getDragCard() {
        return dragPanel == null ? null : dragPanel.getCard();
    }

    public CardAreaPanel getDragPanel() {
        return dragPanel;
    }

    protected void updateDragPanel(float x, float y) {
        //children also include attached and stacked cards, which cardPanels doesn't
        for (int i = getChildCount() - 1; i >= 0; i--) { //later children are drawn on top, so check them first
            final FDisplayObject child = getChildAt(i);
            if (child instanceof CardAreaPanel panel && child.contains(x, y)) {
                dragPanel = panel; //if the finger crosses a gap between cards, keep the last card selected
                break;
            }
        }
        Gdx.graphics.requestRendering();
        dragging = true;
    }

    private void clearDragPanel() {
        if (dragPanel != null) {
            dragPanel = null;
            Gdx.graphics.requestRendering();
        }
        if (panMode == PAN_INSPECT) {
            inspectEndMillis = System.currentTimeMillis();
        }
        panMode = PAN_NONE;
        panSumX = panSumY = 0;
        dragging = false;
        lastDragMillis = System.currentTimeMillis();
    }

    // horizontal strip gestures: a mostly horizontal drag scrolls, a vertical-first drag inspects cards
    private static final int PAN_NONE = 0, PAN_SCROLL = 1, PAN_INSPECT = 2;
    private static final float PAN_LOCK_DISTANCE = Utils.scale(8);
    private static final float INSPECT_STEEPNESS = 1.5f; //a touch drag must be this much more vertical than horizontal to inspect
    private int panMode = PAN_NONE;
    private float panSumX, panSumY;
    private long inspectEndMillis;

    /** Horizontal single-row display in landscape: drag scrolls, a vertical-first drag inspects cards. */
    protected boolean isHorizontalStrip() { return false; }

    //FScrollPane.pan gives the drag away as soon as a single event is more vertical than horizontal, which a mouse
    //does all the time; once a drag has been recognised as a scroll it stays one
    private boolean scrollRow(float deltaX) {
        if (getMaxScrollLeft() <= 0) { return false; } //nothing to scroll
        setScrollLeft(getScrollLeft() - deltaX);
        return true;
    }

    @Override
    public boolean pan(float x, float y, float deltaX, float deltaY, boolean moreVertical) {
        return super.pan(x, y, deltaX, deltaY, moreVertical);
        // TODO for touch
        /*if (!Forge.isLandscapeMode()) {
            return super.pan(x, y, deltaX, deltaY, moreVertical);
        }
        if (!isHorizontalStrip()) { //vertical column layout keeps the old behaviour
            updateDragPanel(x, y);
            return super.pan(x, y, deltaX, deltaY, moreVertical);
        }
        if (panMode == PAN_NONE) { //decide once per gesture, after a small slop
            panSumX += deltaX;
            panSumY += deltaY;
            if (Math.max(Math.abs(panSumX), Math.abs(panSumY)) < PAN_LOCK_DISTANCE) { return true; }
            //a mouse always scrolls: hovering and right click already show the preview, and it can't aim a steep drag
            boolean inspect = !GuiBase.getInterface().isRunningOnDesktop()
                    && Math.abs(panSumY) > Math.abs(panSumX) * INSPECT_STEEPNESS;
            panMode = inspect ? PAN_INSPECT : PAN_SCROLL;
            if (panMode == PAN_SCROLL) {
                return scrollRow(panSumX); //hand over what was accumulated
            }
        }
        if (panMode == PAN_INSPECT) {
            updateDragPanel(x, y); //preview follows the finger, no scrolling
            return true;
        }
        return scrollRow(deltaX); //scrolling never shows the preview*/
    }

    @Override
    public boolean fling(float velocityX, float velocityY) {
        if (panMode == PAN_INSPECT || System.currentTimeMillis() - inspectEndMillis < 200) {
            return true; //finishing an inspect drag must not throw the strip
        }
        return super.fling(velocityX, velocityY);
    }

    @Override
    public boolean zoom(float x, float y, float amount) {
        //mouse wheel over a horizontal row that overflows scrolls it sideways; pinch, or nothing to scroll, goes on to the field zoom
        if (isHorizontalStrip() && GuiBase.getInterface().isRunningOnDesktop() && getScrollWidth() > getWidth() + 1) {
            float step = Utils.scale(48);
            setScrollLeft(getScrollLeft() + (amount > 0 ? step : -step)); //flip the sign here if the wheel feels backwards
            return true;
        }
        return super.zoom(x, y, amount);
    }

    @Override
    public boolean panStop(float x, float y) {
        clearDragPanel();
        return super.panStop(x, y);
    }

    @Override
    public boolean release(float x, float y) {
        clearDragPanel();
        return super.release(x, y);
    }

    @Override
    protected void drawOverlay(Graphics g) {
        super.drawOverlay(g);
        if (dragPanel == null || !Forge.isLandscapeMode()) { return; }

        //surround the selected card with a box, matching how the card is drawn (tapped cards are turned sideways)
        final float padding = FCardPanel.PADDING;
        float w = dragPanel.getWidth() - 2 * padding;
        float h = dragPanel.getHeight() - 2 * padding;
        if (w == h) { //adjust width if needed to match how the card itself is drawn
            w = h / FCardPanel.ASPECT_RATIO;
        }
        float top = dragPanel.getTop() + padding;
        if (dragPanel.isTapped()) {
            top += h - w;
            float temp = w;
            w = h;
            h = temp;
        }
        FSelectionBox.draw(g, Color.LIME, SELECTION_BOX_THICKNESS, dragPanel.getLeft() + padding, top, w, h);
    }
    @Override
    public int getCount() {
        return cardPanels.get().size();
    }

    @Override
    public void setRotate180(boolean b0) {
        //only rotate cards themselves
        rotateCards180 = b0;
    }

    private float getCardStackOffset() {
        return CARD_STACK_OFFSET;
    }

    protected boolean animateEntry() { return false; }

    /**
     * Adds the cards of the last layout to {@code out} without allocating. orderedCards is cleared and refilled
     * by layout, so an iterator (or addAll) throws ConcurrentModificationException if a relayout lands
     * mid-copy. Reading by index cannot; if the list shrinks under us the rest is simply skipped, which at
     * worst costs one entry animation.
     */
    private void collectOrderedCards(Set<CardView> out) {
        final List<CardView> ordered = orderedCards.get();
        try {
            for (int i = 0, n = ordered.size(); i < n; i++) {
                out.add(ordered.get(i));
            }
        } catch (IndexOutOfBoundsException e) {
            // list shrank during the copy, keep what was read
        }
    }

    protected void refreshCardPanels(Iterable<CardView> model) {
        final boolean animate = animateEntry();
        Set<CardView> known = null, current = null;
        if (animate) {
            if (shownGeneration != generation) { // new game started: forget the old board
                shownLastRefresh.clear();
                shownGeneration = generation;
            }
            shownScratch.clear();
            knownScratch.clear();
            collectOrderedCards(knownScratch);
            currentScratch.clear();
            known = knownScratch;
            current = currentScratch;
        }

        clear();
        CardAreaPanel newCardPanel = null;
        if (model != null) {
            for (CardView card : model) {
                CardAreaPanel cardPanel = CardAreaPanel.get(card);
                if (animate) {
                    current.add(card);
                    shownScratch.add(cardPanel);
                }
                boolean isNew = known != null ? !known.contains(card)
                        : (newCardPanel == null && !orderedCards.get().contains(card));
                addCardPanelToDisplayArea(cardPanel, known);
                cardPanels.get().add(cardPanel);
                if (newCardPanel == null && isNew) {
                    newCardPanel = cardPanel;
                }
            }
        }

        if (animate) { // cards shown last refresh but not now have left this row
            for (int i = 0; i < shownLastRefresh.size(); i++) {
                CardAreaPanel p = shownLastRefresh.get(i);
                if (current.contains(p.getCard())) { continue; }
                // A network client replaces a moved card's view, so the panel's copy can still say battlefield
                if (CardFlightOverlay.isOnBattlefield(p.getCard())) {
                    // if the new row already took this panel, it has handled the move
                    if (p.getDisplayArea() == null || p.getDisplayArea() == this) {
                        CardAreaPanel.markMoved(p.getCard());
                        CardAreaPanel.forgetAnimated(p.getCard());
                    }
                    continue;
                }
                CardAreaPanel.forgetAnimated(p.getCard());
                p.playLeaveAnimation();
            }
            shownLastRefresh.clear();
            shownLastRefresh.addAll(shownScratch);
        }

        if (isVisible()) {
            revalidate();
            if (newCardPanel != null) {
                scrollIntoView(newCardPanel);
            }
        }
    }

    @Override
    public void setVisible(boolean b0) {
        if (isVisible() == b0) {
            if (!b0) { dragPanel = null; }
            return;
        }
        super.setVisible(b0);
        if (b0) { //when zone becomes visible, ensure display area of panels is updated and panels layed out
            for (CardAreaPanel pnl : cardPanels.get()) {
                pnl.displayArea = this;
            }
            revalidate();
        }
    }

    //support adding card panel and attached panels to display area recursively
    private void addCardPanelToDisplayArea(CardAreaPanel cardPanel, Set<CardView> known) {
        do {
            List<CardAreaPanel> attachedPanels = cardPanel.getAttachedPanels();
            for (int i = attachedPanels.size() - 1; i >= 0; i--) {
                addCardPanelToDisplayArea(attachedPanels.get(i), known);
            }
            // ghosts are fresh throwaway panels for exiled/prepared cards, so never animate them
            if (known != null && !cardPanel.isGhost() && !known.contains(cardPanel.getCard())) {
                cardPanel.playEntryAnimation(this);
            }
            if (isVisible()) { cardPanel.displayArea = this; }
            add(cardPanel);
            cardPanel = cardPanel.getNextPanelInStack();
        } while (cardPanel != null);
    }

    public final void removeCardPanel(final CardAreaPanel fromPanel) {
        FThreads.assertExecutedByEdt(true);
        /*if (CardPanelContainer.this.getMouseDragPanel() != null) {
            CardPanel.getDragAnimationPanel().setVisible(false);
            CardPanel.getDragAnimationPanel().repaint();
            CardPanelContainer.this.getCardPanels().remove(CardPanel.getDragAnimationPanel());
            CardPanelContainer.this.remove(CardPanel.getDragAnimationPanel());
            CardPanelContainer.this.setMouseDragPanel(null);
        }*/
        cardPanels.get().remove(fromPanel);
        remove(fromPanel);
    }

    protected void clearChildren() {
        super.clear();
    }

    @Override
    public void clear() {
        super.clear();
        if (!cardPanels.get().isEmpty()) {
            for (CardAreaPanel panel : cardPanels.get()) {
                if (panel.displayArea == null || panel.displayArea == this ||
                        !panel.displayArea.cardPanels.get().contains(panel)) { //don't reset if panel's displayed in another area already
                    panel.reset();
                }
            }
            cardPanels.get().clear();
        }
    }

    private int addCards(CardAreaPanel cardPanel, float x, float y, float cardWidth, float cardHeight) {
        int totalCount = 0;
        List<CardAreaPanel> attachedPanels = cardPanel.getAttachedPanels();
        if (!attachedPanels.isEmpty()) {
            for (int i = attachedPanels.size() - 1; i >= 0; i--) {
                CardAreaPanel attachedPanel = attachedPanels.get(i);
                if (attachedPanel != null) {
                    int count = addCards(attachedPanel, x, y, cardWidth, cardHeight);
                    x += count * cardWidth * getCardStackOffset();
                    totalCount += count;
                }
            }
        }

        orderedCards.get().add(cardPanel.getCard());
        if (cardPanel.isInfoGhost()) {
            infoGhostCardIds.get().add(cardPanel.getCard().getId());
        }
        cardPanel.setBounds(x, y, cardWidth, cardHeight);

        if (cardPanel.getNextPanelInStack() != null) { //add next panel in stack if needed
            x += cardWidth * getCardStackOffset();
            totalCount += addCards(cardPanel.getNextPanelInStack(), x, y, cardWidth, cardHeight);
        }
        return totalCount + 1;
    }

    protected float getCardWidth(float cardHeight) {
        return (cardHeight - 2 * FCardPanel.PADDING) / FCardPanel.ASPECT_RATIO + 2 * FCardPanel.PADDING; //ensure aspect ratio maintained after padding applied
    }

    protected float getCardHeight(float cardWidth) {
        return (cardWidth - 2 * FCardPanel.PADDING) * FCardPanel.ASPECT_RATIO + 2 * FCardPanel.PADDING; //ensure aspect ratio maintained after padding applied
    }

    @Override
    protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
        //room at the right end that something floating over the area (a zone box) covers: cards stay clear of it or can be scrolled clear
        final float pad = Math.max(0f, getEndPadding());
        final float usable = Math.max(1f, visibleWidth - pad);
        float x = layoutCards(0f, visibleHeight);
        final float align = getContentAlignment();
        if (align > 0f && x > 0f && x < usable) { //fewer cards than fit: place them again, shifted
            x = layoutCards(Math.round((usable - x) * align), visibleHeight);
            return new ScrollBounds(visibleWidth, visibleHeight);
        }
        return new ScrollBounds(Math.max(x + pad, visibleWidth), visibleHeight);
    }

    //@return width at the right end of the area that is covered by something else, 0 normally
    protected float getEndPadding() {
        return 0f;
    }

    //@return how far to the right (0 = left, 0.5 = centered, 1 = right) cards are placed when they don't fill the area
    protected float getContentAlignment() {
        return 0f;
    }

    //lays the cards out from startX, returns the x the last one ends at
    private float layoutCards(float startX, float visibleHeight) {
        orderedCards.get().clear();
        infoGhostCardIds.get().clear();

        float x = startX;
        float y = 0;
        float cardHeight = visibleHeight;
        float cardWidth = getCardWidth(cardHeight);

        for (CardAreaPanel cardPanel : new ArrayList<>(cardPanels.get())) {
            if (cardPanel != null) {
                int count = addCards(cardPanel, x, y, cardWidth, cardHeight);
                x += cardWidth + (count - 1) * cardWidth * getCardStackOffset();
            }
        }
        return x;
    }

    @Override
    protected void startClip(Graphics g) {
        //prevent clipping top and bottom
        float h = getHeight();
        g.startClip(0, -h, getWidth(), 3 * h);
    }

    @Override
    public String getActivateAction(int index) {
        if (!GuiBase.isNetPlay(MatchController.instance)) {
            //causes lag on netplay client side, also index shouldn't be out of bounds
            if (index >= 0 && index < orderedCards.get().size())
                return MatchController.instance.getGameController().getActivateDescription(orderedCards.get().get(index));
        }

        return Forge.getLocalizer().getMessage("lblActivateAction"); //simple text on card zoom swipe up
    }

    @Override
    public void setSelectedIndex(int index) {
        //just scroll card into view
        if (index < orderedCards.get().size()) {
            final CardView card = orderedCards.get().get(index);
            if (infoGhostCardIds.get().contains(card.getId())) {
                return;
            }
            scrollIntoView(CardAreaPanel.get(card));
        }
    }

    @Override
    public void activate(int index) {
        final CardView card = orderedCards.get().get(index);
        if (infoGhostCardIds.get().contains(card.getId())) {
            return; //informational exile ghost: not actionable
        }
        final CardAreaPanel cardPanel = CardAreaPanel.get(card);
        //must invoke in game thread in case a dialog needs to be shown
        ThreadUtil.invokeInGameThread(() -> cardPanel.selectCard(false));
    }

    // ---- the Arena board's rows ------------------------------------------------------------------------------------

    /** One card on the board with what stands with it: its attachments (behind it), then it, then its stacked copies. */
    private static int arenaGroupSize(CardAreaPanel top) {
        int n = 0;
        for (CardAreaPanel a : top.getAttachedPanels()) {
            if (a != null) { n += arenaGroupSize(a); }
        }
        n++;
        return top.getNextPanelInStack() == null ? n : n + arenaGroupSize(top.getNextPanelInStack());
    }

    private static float arenaGroupWidth(CardAreaPanel top, float w) {
        return w + Math.round(w * ArenaBoard.STACK_STEP) * arenaGroupSteps(top);
    }

    /**
     * How many steps a group fans out to the right: one per card behind the front one, except that a stack of tokens
     * (treasures, food, a crowd of 1/1s) reads as one card - at most {@link ArenaBoard#TOKEN_PEEK} edges show behind
     * it, the rest lie straight under, and its count says how many.
     */
    private static int arenaGroupSteps(CardAreaPanel top) {
        final int size = arenaGroupSize(top);
        return isArenaTokenPile(top) ? Math.min(size - 1, ArenaBoard.TOKEN_PEEK) : size - 1;
    }

    private static boolean isArenaTokenPile(CardAreaPanel top) {
        return top.getNextPanelInStack() != null && top.getCard().isToken() && top.getAttachedPanels().isEmpty();
    }

    /**
     * The Arena board's arrangement of a battlefield row (port of the Swing PlayArea.layoutArenaBoard): the front row
     * (creatures) is centred and its tiles keep one size while they fit, then shrink, then overlap; the back row runs in
     * two wings either side of the portrait, lands on the left pressed against it and everything else on the right. In
     * combat what fights steps out towards the other army and stands a little larger. Tiles are wider than tall.
     *
     * @param front     the front row (else the back row)
     * @param local     the local player's field (else an opponent's)
     * @param seat      an opponent's seat in a game of three or more
     * @param centreX   where the portrait's column is, in this row's coordinates
     * @param rowY      where one row of tiles sits from this area's top (the back row's area is the band it may wrap into)
     */
    protected final void layoutArenaRow(boolean front, boolean local, boolean seat, float centreX, float width, float rowY) {
        orderedCards.get().clear();
        infoGhostCardIds.get().clear();
        final List<CardAreaPanel> tops = cardPanels.get();
        final float frontH = ArenaBoard.frontTileHeight(local, seat);
        final float minH = ArenaBoard.minTileHeight();
        final float margin = ArenaBoard.rowMargin(seat);
        // your rows keep left of the stack's dock while it is open (it would cover the right end of them otherwise)
        final float right = local ? Math.min(width - margin, ArenaBoard.localRowRight()) : width - margin;
        if (front) {
            float h = frontH;
            final float room = right - margin;
            while (h > minH && arenaRowWidth(tops, h) > room) {
                h -= 2;
            }
            final float w = Math.round(h * ArenaBoard.TILE_RATIO);
            final float total = arenaRowWidth(tops, h);
            final float squeeze = total <= room || tops.size() < 2 ? 0f : (total - room) / (tops.size() - 1);
            final float shown = total - squeeze * Math.max(0, tops.size() - 1);
            float x = Math.max(margin, centreX - shown / 2f);
            if (x + shown > right) {
                x = Math.max(margin, right - shown);
            }
            final float step = Math.round(ArenaBoard.windowH() * ArenaBoard.COMBAT_STEP) * (local ? -1 : 1);
            for (CardAreaPanel top : tops) {
                final boolean fighting = top.getCard().isAttacking() || top.getCard().isBlocking();
                placeArenaGroup(top, x, fighting ? step : 0f, w, h, fighting ? ArenaBoard.COMBAT_SCALE : 1f);
                x += arenaGroupWidth(top, w) + Math.round(w * ArenaBoard.TILE_GAP) - squeeze;
            }
            return;
        }
        // the back row: lands on the left wing, the rest on the right; lands shrink before they cross the portrait. It fills
        // this area's whole height (the band it may wrap into); one row of it sits rowY down
        final List<CardAreaPanel> lands = new ArrayList<>(), others = new ArrayList<>();
        for (CardAreaPanel top : tops) {
            (top.getCard().getCurrentState().isLand() ? lands : others).add(top);
        }
        float h = ArenaBoard.backTileHeight(local, seat, frontH);
        final float fullH = h; //the most a wrapped row's tiles get
        final float clear = ArenaBoard.portraitClear(seat);
        final float rightLimit = Math.min(ArenaBoard.backRowRight(local, seat, width), right);
        if (seat) { //a seat's portrait is above its rows: one row across the whole width
            final List<CardAreaPanel> all = new ArrayList<>(lands);
            all.addAll(others);
            while (h > minH && arenaRowWidth(all, h) > rightLimit - margin) {
                h -= 2;
            }
            if (arenaRowWidth(all, h) > rightLimit - margin
                    && placeArenaRows(fullH, new ArenaWing(all, margin, rightLimit, false, true))) {
                return; //more than one row is better than cards on top of each other
            }
            placeArenaWing(all, margin, rightLimit, rowY, h, false, true);
            return;
        }
        final float leftRoom = centreX - clear - margin;
        final float smallest = ArenaBoard.smallestBackHeight();
        while (h > smallest && !lands.isEmpty() && arenaRowWidth(lands, h) > leftRoom) {
            h -= 2;
        }
        final float w = Math.round(h * ArenaBoard.TILE_RATIO);
        final float gap = Math.round(w * ArenaBoard.TILE_GAP);
        final float mostOverlap = Math.round(w * 0.35f);
        final List<CardAreaPanel> leftWing = new ArrayList<>(), rightWing = new ArrayList<>();
        float used = -gap;
        for (CardAreaPanel top : lands) {
            final float gw = arenaGroupWidth(top, w) + gap;
            if (leftWing.isEmpty() || used + gw - leftWing.size() * mostOverlap <= leftRoom) {
                leftWing.add(top);
                used += gw;
            } else {
                rightWing.add(top);
            }
        }
        rightWing.addAll(others);
        // one row would put cards on top of each other: lands and the rest each take more rows of smaller tiles instead
        if ((arenaRowWidth(leftWing, h) > leftRoom || arenaRowWidth(rightWing, h) > rightLimit - centreX - clear)
                && placeArenaRows(fullH, new ArenaWing(lands, margin, centreX - clear, true, false),
                        new ArenaWing(others, centreX + clear, rightLimit, false, false))) {
            return;
        }
        placeArenaWing(leftWing, margin, centreX - clear, rowY, h, true, false);
        placeArenaWing(rightWing, centreX + clear, rightLimit, rowY, h, false, false);
    }

    /** A part of the back row: its cards, between two x positions, pressed against the portrait (alignRight) or centred. */
    private static final class ArenaWing {
        final List<CardAreaPanel> cards;
        final float left, right;
        final boolean alignRight, centre;

        ArenaWing(List<CardAreaPanel> cards, float left, float right, boolean alignRight, boolean centre) {
            this.cards = cards;
            this.left = left;
            this.right = right;
            this.alignRight = alignRight;
            this.centre = centre;
        }
    }

    /**
     * The back row wrapped onto more rows: the largest tiles (up to {@code mostH}) at which every wing fits its width in
     * at most {@link ArenaBoard#BACK_ROWS_MOST} rows without overlapping, and all those rows fit this area's height (at
     * the smallest, one row of small tiles may do). Each wing's rows sit in the middle of the height. False, placing
     * nothing, when no such size is left: then the one row overlaps as it must.
     */
    private boolean placeArenaRows(float mostH, ArenaWing... wings) {
        final float bandH = getHeight(), rowGap = ArenaBoard.backRowsGap();
        final float least = Math.round(ArenaBoard.windowH() * ArenaBoard.BACK_ROWS_LEAST_H);
        for (float h = mostH; h >= least; h -= 2) {
            int rows = 0;
            for (ArenaWing wing : wings) {
                rows = Math.max(rows, arenaRows(wing.cards, wing.right - wing.left, h).size());
            }
            if (rows == 0 || rows > ArenaBoard.BACK_ROWS_MOST || rows * h + (rows - 1) * rowGap > bandH) {
                continue;
            }
            for (ArenaWing wing : wings) {
                final List<List<CardAreaPanel>> lines = arenaRows(wing.cards, wing.right - wing.left, h);
                final float y = (bandH - (lines.size() * h + (lines.size() - 1) * rowGap)) / 2f;
                for (int i = 0; i < lines.size(); i++) {
                    placeArenaWing(lines.get(i), wing.left, wing.right, Math.round(y + i * (h + rowGap)), h, wing.alignRight, wing.centre);
                }
            }
            return true;
        }
        return false;
    }

    /** These cards in rows no wider than {@code room} at tile height {@code h}, filling each row before the next. */
    private static List<List<CardAreaPanel>> arenaRows(List<CardAreaPanel> cards, float room, float h) {
        final List<List<CardAreaPanel>> lines = new ArrayList<>();
        final float w = Math.round(h * ArenaBoard.TILE_RATIO), gap = Math.round(w * ArenaBoard.TILE_GAP);
        List<CardAreaPanel> line = null;
        float used = 0;
        for (CardAreaPanel top : cards) {
            final float gw = arenaGroupWidth(top, w);
            if (line == null || used + gap + gw > room) {
                line = new ArrayList<>();
                lines.add(line);
                used = gw;
            } else {
                used += gap + gw;
            }
            line.add(top);
        }
        return lines;
    }

    private static float arenaRowWidth(List<CardAreaPanel> row, float h) {
        final float w = Math.round(h * ArenaBoard.TILE_RATIO), gap = Math.round(w * ArenaBoard.TILE_GAP);
        float total = -gap;
        for (CardAreaPanel top : row) {
            total += arenaGroupWidth(top, w) + gap;
        }
        return Math.max(0, total);
    }

    /** One wing of the back row between two x positions, pressed against the portrait (alignRight) or centred; overlaps if it must. */
    private void placeArenaWing(List<CardAreaPanel> row, float left, float right, float y, float h, boolean alignRight, boolean centre) {
        if (row.isEmpty()) {
            return;
        }
        final float w = Math.round(h * ArenaBoard.TILE_RATIO), gap = Math.round(w * ArenaBoard.TILE_GAP);
        final float total = arenaRowWidth(row, h);
        final float room = Math.max(w, right - left);
        final float squeeze = total <= room || row.size() < 2 ? 0f : (total - room) / (row.size() - 1);
        final float shown = total - squeeze * Math.max(0, row.size() - 1);
        float x = alignRight ? Math.max(left, right - shown) : centre ? Math.max(left, left + (room - shown) / 2f) : left;
        for (CardAreaPanel top : row) {
            placeArenaGroup(top, x, y, w, h, 1f);
            x += arenaGroupWidth(top, w) + gap - squeeze;
        }
    }

    /** Lays a card and what stands with it out from x: each one a step to the right of the one behind it. */
    private void placeArenaGroup(CardAreaPanel top, float x, float y, float w, float h, float scale) {
        final List<FCardPanel> group = new ArrayList<>();
        top.buildCardPanelList(group);
        final float step = Math.round(w * ArenaBoard.STACK_STEP);
        int chain = 0;
        for (CardAreaPanel p = top; p != null; p = p.getNextPanelInStack()) {
            chain++;
        }
        // the cards of a token pile past the few that peek out lie straight under the first of those
        final int under = group.size() - 1 - arenaGroupSteps(top);
        for (int i = 0; i < group.size(); i++) {
            final CardAreaPanel p = (CardAreaPanel) group.get(i);
            orderedCards.get().add(p.getCard());
            if (p.isInfoGhost()) {
                infoGhostCardIds.get().add(p.getCard().getId());
            }
            p.setBounds(Math.round(x + Math.max(0, i - under) * step), Math.round(y), w, h);
            final ArenaCards.State st = p.getArenaState();
            st.scale = scale;
            st.stackBack = i < group.size() - 1;
            st.groupCount = i == group.size() - 1 && chain > 1 ? chain : 0;
        }
    }

    public static class CardAreaPanel extends FCardPanel {
        private static Map<Integer, CardAreaPanel> allCardPanels = new HashMap<>();
        private static final Map<Integer, Rectangle> handStarts = new ConcurrentHashMap<>();
        private static long matchStartTime = System.currentTimeMillis();
        private long entryStart = -1;
        private CardFlightOverlay.Flight flight;
        private final Rectangle lastHandRect = new Rectangle();
        private boolean hasHandRect;
        private final Rectangle lastFieldRect = new Rectangle();
        private float lastFieldAngle;
        private boolean hasFieldRect;
        private boolean leaveOriginLocked;
        private VCardDisplayArea lastFieldRow;
        private static final Set<Integer> animatedIds = Sets.newConcurrentHashSet();
        private static final Set<Integer> movedIds = Sets.newConcurrentHashSet();
        public static void markMoved(CardView c) { movedIds.add(c.getId()); }
        public static Rectangle takeHandStart(CardView card) {
            return handStarts.remove(card.getId());
        }
        public static void forgetAnimated(CardView card) {
            animatedIds.remove(card.getId());
        }
        public static CardAreaPanel peek(CardView card) {
            CardAreaPanel p = allCardPanels.get(card.getId());
            return (p != null && p.getCard() == card) ? p : null;
        }
        public void setLeaveOrigin(float x, float y, float w, float h, float angle) {
            lastFieldRect.set(x, y, w, h);
            lastFieldAngle = angle;
            hasFieldRect = true;
            leaveOriginLocked = true;
        }
        public void clearLeaveOrigin() { leaveOriginLocked = false; }
        public void playEntryAnimation() { playEntryAnimation(null); }

        public void playEntryAnimation(VCardDisplayArea target) {
            if (CardFlightOverlay.style() == CardFlightOverlay.Style.OFF) { return; }
            if (System.currentTimeMillis() - matchStartTime < 2000) { return; }

            final int id = getCard().getId();

            // the new row refreshed before the old row could mark the move, so detect it here
            if (target != null && lastFieldRow != null && lastFieldRow != target && hasFieldRect
                    && getCard().getZone() == ZoneType.Battlefield) {
                movedIds.add(id);
                animatedIds.remove(id);
            }

            if (!animatedIds.add(id)) { return; }

            if (movedIds.contains(id)) {
                if (hasFieldRect) { // slide from where it was on the field
                    handStarts.put(id, new Rectangle(lastFieldRect));
                }
            } else if (hasHandRect && !handStarts.containsKey(id) && isHandShownFor(getCard())) {
                handStarts.put(id, new Rectangle(lastHandRect));
            }
            hasHandRect = false;

            entryStart = System.currentTimeMillis();
            Gdx.graphics.requestRendering();
        }
        // card left the battlefield row (destroyed, exiled, bounced...): animate it leaving from its last spot
        public void playLeaveAnimation() {
            if (!hasFieldRect) { return; }
            hasFieldRect = false;
            leaveOriginLocked = false;
            if (CardFlightOverlay.style() == CardFlightOverlay.Style.OFF) { return; }
            if (System.currentTimeMillis() - matchStartTime < 2000) { return; }
            CardFlightOverlay.startLeave(getCard(), new Rectangle(lastFieldRect), lastFieldAngle);
            FlipOntoBattlefieldAnimation.leaveStarted(getCard());
        }
        private static boolean isHandShownFor(CardView card) {
            VPlayerPanel pp = MatchScreen.getPlayerPanel(card.getController());
            if (pp == null) { return false; }
            VPlayerPanel.InfoTab tab = pp.getSelectedTab();
            return tab instanceof VPlayerPanel.InfoTabZone z && z.zoneType == ZoneType.Hand;
        }
        public static CardAreaPanel get(CardView card0) {
            CardAreaPanel cardPanel = allCardPanels.get(card0.getId());
            if (cardPanel == null || cardPanel.getCard() != card0) { //replace card panel if card copied
                cardPanel = new CardAreaPanel(card0);
                allCardPanels.put(card0.getId(), cardPanel);
            }
            return cardPanel;
        }

        /**
         * Undo rewinds the game in place: cards that were copied to a new zone after the checkpoint no longer exist,
         * and the restored originals come back with no events. Everything remembered about the old timeline
         * (who already animated, who "moved rows", where a card was in hand/field) would misfire, so start clean.
         */
        public static void resetForUndo() {
            movedIds.clear();
            handStarts.clear();
            animatedIds.clear();
            CardFlightOverlay.clear();
            for (CardAreaPanel p : allCardPanels.values()) {
                p.flight = null;
                p.entryStart = -1;
                p.hasHandRect = false;
                p.hasFieldRect = false;
                p.leaveOriginLocked = false;
                p.lastFieldRow = null;
            }
            generation++; // field rows drop their remembered board, so nothing counts as a departure
        }

        public static void resetForNewGame() {
            if (allCardPanels != null) {
                for (CardAreaPanel cardPanel : allCardPanels.values()) {
                    cardPanel.displayArea = null;
                    cardPanel.attachedToPanel = null;
                    cardPanel.attachedPanels.clear();
                    cardPanel.prevPanelInStack = null;
                    cardPanel.nextPanelInStack = null;
                }
                allCardPanels.clear();
            } else {
                allCardPanels = new HashMap<>();
            }
            matchStartTime = System.currentTimeMillis();
            handStarts.clear();
            animatedIds.clear();
            CardFlightOverlay.clear();
            generation++; // field rows drop their remembered board on their next refresh
        }

        private VCardDisplayArea displayArea;
        private CardAreaPanel attachedToPanel;
        private List<CardAreaPanel> attachedPanels = new ArrayList<>();
        private CardAreaPanel nextPanelInStack, prevPanelInStack;

        // CASTABLE = a prepared spell (tap to cast); INFO = a card exiled until this permanent leaves (inspect only)
        public enum GhostKind { NONE, CASTABLE, INFO }
        private static final Color GHOST_TINT = new Color(90 / 255f, 120 / 255f, 175 / 255f, 0.45f);
        private GhostKind ghostKind = GhostKind.NONE;

        //use static get(card) function instead
        private CardAreaPanel(CardView card0) {
            super(card0);
        }

        private ArenaCards.State arenaState;

        /** What the Arena look remembers about this card between frames (its glide, light, tap...). */
        public ArenaCards.State getArenaState() {
            if (arenaState == null) {
                arenaState = new ArenaCards.State();
            }
            return arenaState;
        }

        /** Whether the Arena look draws this card as a tile on a battlefield row. */
        public boolean isArenaTile() {
            return displayArea instanceof VField.FieldRow && ArenaMatchLayout.applies();
        }

        /** Whether the Arena look draws this card in the local hand's fan. */
        public boolean isArenaHandCard() {
            return displayArea instanceof VZoneDisplay zone && zone.isArenaHand();
        }

        @Override
        protected boolean renderedCardContains(float x, float y) {
            if (isArenaTile() || isArenaHandCard()) { //the whole panel is the tile or the card (and the strip it rose from)
                return x >= 0 && y >= 0 && x <= getWidth() && y <= getHeight();
            }
            return super.renderedCardContains(x, y);
        }

        public VCardDisplayArea getDisplayArea() {
            return displayArea;
        }

        public CardAreaPanel getAttachedToPanel() {
            return attachedToPanel;
        }

        public boolean isGhost() {
            return ghostKind != GhostKind.NONE;
        }

        public boolean isInfoGhost() {
            return ghostKind == GhostKind.INFO;
        }

        public void setAttachedToPanel(final CardAreaPanel attachedToPanel0) {
            attachedToPanel = attachedToPanel0;
        }

        public List<CardAreaPanel> getAttachedPanels() {
            if (attachedPanels == null) {
                attachedPanels = new ArrayList<>();
                String error = getCard() + " - Attached panel is null.";
                Sentry.captureMessage(error);
                System.err.println(error);
            }
            return attachedPanels;
        }

        public CardAreaPanel getNextPanelInStack() {
            return nextPanelInStack;
        }

        public void setNextPanelInStack(CardAreaPanel nextPanelInStack0) {
            nextPanelInStack = nextPanelInStack0;
        }

        public CardAreaPanel getPrevPanelInStack() {
            return prevPanelInStack;
        }

        public void setPrevPanelInStack(CardAreaPanel prevPanelInStack0) {
            prevPanelInStack = prevPanelInStack0;
        }

        @Override
        protected CardStackPosition getStackPosition() {
            if (nextPanelInStack == null && attachedToPanel == null) {
                return CardStackPosition.Top;
            }
            if (isTapped()) {
                return CardStackPosition.Top; //ensure P/T not hidden for tapped cards
            }
            return CardStackPosition.BehindHorz;
        }

        public void updateCard(final CardView card) {
            showOnBattlefield(card);

            attachedPanels.clear();

            for (final CardView e : card.getAllAttachedCards()) {
                final CardAreaPanel cardE = CardAreaPanel.get(e);
                if (cardE != null) {
                    attachedPanels.add(cardE);
                }
            }
            CardView getAttachedto = card.getAttachedTo();
            if (getAttachedto != null) {
                if (card != getAttachedto.getAttachedTo())
                    setAttachedToPanel(CardAreaPanel.get(getAttachedto));
                else {
                    attachedPanels.remove(CardAreaPanel.get(getAttachedto));
                    setAttachedToPanel(null);
                }
            } else {
                setAttachedToPanel(null);
            }

            addLinkedExileGhosts(card);
        }

        // Fresh instances each refresh, never the shared CardAreaPanel cache (which the exile zone reuses)
        private void addLinkedExileGhosts(final CardView card) {
            if (!FModel.getPreferences().getPrefBoolean(FPref.UI_SHOW_LINKED_EXILE_CARDS)) {
                return;
            }
            int cap = FModel.getPreferences().getPrefInt(FPref.UI_MAX_STACK_DEPTH) - attachedPanels.size();
            if (cap <= 0) {
                return;
            }
            final CardView prepared = card.getPreparedSpell();
            if (prepared != null) {
                attachedPanels.add(newGhost(prepared, GhostKind.CASTABLE));
                cap--;
            }
            for (final CardView exiled : card.getUntilLeavesBattlefield()) {
                if (cap <= 0) {
                    break;
                }
                attachedPanels.add(newGhost(exiled, GhostKind.INFO));
                cap--;
            }
        }

        private CardAreaPanel newGhost(final CardView ghostCard, final GhostKind kind) {
            final CardAreaPanel ghost = new CardAreaPanel(ghostCard);
            ghost.ghostKind = kind;
            ghost.setAttachedToPanel(this);
            return ghost;
        }

        private void drawGhostOverlay(Graphics g) {
            final float padding = getPadding();
            float w = getWidth() - 2 * padding;
            final float h = getHeight() - 2 * padding;
            if (w == h) {
                w = h / ASPECT_RATIO;
            }
            g.fillRect(GHOST_TINT, padding, padding, w, h);
        }

        //clear and reset all pointers from this panel
        public void reset() {
            if (!attachedPanels.isEmpty()) {
                attachedPanels.clear();
            }
            if (nextPanelInStack != null) {
                nextPanelInStack.reset();
                nextPanelInStack = null;
            }
            attachedToPanel = null;
            prevPanelInStack = null;
            displayArea = null;
        }

        @Override
        public boolean tap(float x, float y, int count) {
            if (count > 1) //prevent double choice lists or activate handle
                return false;
            if (renderedCardContains(x, y)) {
                //must invoke in game thread in case a dialog needs to be shown
                ThreadUtil.invokeInGameThread(() -> {
                    if (GuiBase.getInterface().isRunningOnDesktop() && Forge.mouseButtonID == Input.Buttons.RIGHT) {
                        FThreads.invokeInEdtLater(CardAreaPanel.this::showZoom);
                    } else if (!selectCard(false)) {
                        //if no cards in stack can be selected, just show zoom/details for card
                        if (!MatchController.instance.isSelecting())
                            FThreads.invokeInEdtLater(CardAreaPanel.this::showZoom);
                    }
                });
                return true;
            }
            return false;
        }

        @Override
        public boolean flick(float x, float y) {
            if (renderedCardContains(x, y)) {
                //must invoke in game thread in case a dialog needs to be shown
                ThreadUtil.invokeInGameThread(() -> selectCard(true));
                return true;
            }
            return false;
        }

        public boolean selectCard(boolean selectEntireStack) {
            if (displayArea != null && (getCard().getZone() == ZoneType.Hand || isArenaHandCard()) && screenPos.width > 0) { //a cast flies from where it was shown
                handStarts.put(getCard().getId(), new Rectangle(screenPos));
            }
            if (MatchController.instance.getGameController().selectCard(getCard(), getOtherCardsToSelect(selectEntireStack), null)) {
                Gdx.graphics.requestRendering();
                return true;
            }
            //if panel can't do anything with card selection, try selecting previous panel in stack
            if (prevPanelInStack != null && prevPanelInStack.selectCard(selectEntireStack)) {
                Gdx.graphics.requestRendering();
                return true;
            }
            //as a last resort try to select attached panels
            for (CardAreaPanel panel : attachedPanels) {
                if (panel.isGhost()) {
                    continue; //ghosts are selected only by a direct tap, never via the host
                }
                if (panel.selectCard(selectEntireStack)) {
                    Gdx.graphics.requestRendering();
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean longPress(float x, float y) {
            if (VCardDisplayArea.isDragGestureActive()) { return true; }
            if (renderedCardContains(x, y)) {
                showZoom();
                return true;
            }
            return false;
        }

        public void showZoom() {
            if (displayArea == null) {
                return;
            }

            final List<CardView> cards = displayArea.orderedCards.get();
            CardZoom.show(cards, cards.indexOf(getCard()), displayArea);
        }

        public void buildCardPanelList(List<? super FCardPanel> list) {
            if (!attachedPanels.isEmpty()) {
                for (int i = attachedPanels.size() - 1; i >= 0; i--) {
                    attachedPanels.get(i).buildCardPanelList(list);
                }
            }

            list.add(this);

            if (nextPanelInStack != null) {
                nextPanelInStack.buildCardPanelList(list);
            }
        }

        private List<CardView> getOtherCardsToSelect(boolean selectOtherCardsInStack) {
            if (!selectOtherCardsInStack) {
                return null;
            }

            //on double-tap select all other cards in stack if any
            if (prevPanelInStack == null && nextPanelInStack == null) {
                return null;
            }

            List<CardView> cards = new ArrayList<>();

            CardAreaPanel panel = nextPanelInStack;
            while (panel != null) {
                cards.add(panel.getCard());
                panel = panel.nextPanelInStack;
            }
            panel = prevPanelInStack;
            while (panel != null) {
                cards.add(panel.getCard());
                panel = panel.prevPanelInStack;
            }
            return cards;
        }

        public static Vector2 getTargetingArrowOrigin(FDisplayObject cardDisplay, boolean isTapped) {
            Vector2 origin = new Vector2(cardDisplay.screenPos.x, cardDisplay.screenPos.y);

            float left = PADDING;
            float top = PADDING;
            float w = cardDisplay.getWidth() - 2 * PADDING;
            float h = cardDisplay.getHeight() - 2 * PADDING;
            if (w == h) { //adjust width if needed to make room for tapping
                w = h / ASPECT_RATIO;
            }

            if (isTapped) { //rotate box if tapped
                top += h - w;
                float temp = w;
                w = h;
                h = temp;
            }

            origin.x += left + w * TARGET_ORIGIN_FACTOR_X;
            origin.y += top + h * TARGET_ORIGIN_FACTOR_Y;

            return origin;
        }

        public Vector2 getTargetingArrowOrigin() {
            //don't show targeting arrow unless in display area that's visible
            if (displayArea == null || !displayArea.isVisible()) {
                return null;
            }

            return getTargetingArrowOrigin(this, isTapped());
        }

        @Override
        protected float getTappedAngle() {
            if (displayArea != null && displayArea.rotateCards180) {
                return -super.getTappedAngle(); //reverse tap angle if rotated 180 degrees
            }
            return super.getTappedAngle();
        }

        @Override
        public void draw(Graphics g) {
            if (CardFlightOverlay.isEnabled() && getWidth() > 0) {
                if (displayArea != null && flight == null && displayArea.animateEntry() && !leaveOriginLocked) {
                    lastFieldRect.set(screenPos);
                    lastFieldAngle = isTapped() ? getTappedAngle() : 0f;
                    hasFieldRect = true;
                    lastFieldRow = displayArea;
                }
                if (getCard().getZone() == ZoneType.Hand) {
                    lastHandRect.set(screenPos);
                    hasHandRect = true;
                }
            }
            if (entryStart >= 0) {
                if (System.currentTimeMillis() - entryStart > 1500) {
                    entryStart = -1; // never became visible (scrolled away), drop it
                } else if (flight == null) {
                    boolean moved = movedIds.remove(getCard().getId());
                    boolean viaStack = !moved && !getCard().getCurrentState().isLand();
                    Rectangle handStart = takeHandStart(getCard());
                    flight = CardFlightOverlay.start(getCard(), handStart, new Rectangle(screenPos),
                            isTapped() ? getTappedAngle() : 0f, viaStack, moved);
                    entryStart = -1;
                }
            }
            if (flight != null) {
                if (!flight.isDone()) {
                    Gdx.graphics.requestRendering();
                    return; // hide the real card while the flying copy is on screen
                }
                flight = null;
            }
            if (isArenaTile()) {
                final ArenaCards.State st = getArenaState();
                st.follow(screenPos.x, screenPos.y, displayArea);
                stopAnimations(); //the Arena look swings the card over itself
                ArenaCards.drawTile(g, getCard(), st, isHovered() && !ArenaDialogOverlay.coversBoard(), 0, 0, getWidth(), getHeight());
                if (ghostKind != GhostKind.NONE) {
                    g.fillSmoothRoundRect(GHOST_TINT, 0, 0, getWidth(), getHeight(), getWidth() * 0.04f);
                }
                return;
            }
            if (isArenaHandCard()) {
                final ArenaCards.State st = getArenaState();
                st.follow(screenPos.x, screenPos.y, displayArea);
                if (st.raised && getWidth() > 0) { //where the pointer is over it: it tilts that way
                    st.pointerU = (Forge.mouseMovedX - screenPos.x) / getWidth();
                    st.pointerV = (Forge.mouseMovedY - screenPos.y) / (st.cardHeight > 0 ? st.cardHeight : getHeight());
                }
                ArenaCards.drawHandCard(g, getCard(), st, isHovered(), 0, 0, getWidth(), st.cardHeight > 0 ? st.cardHeight : getHeight());
                return;
            }
            if (displayArea != null && displayArea.rotateCards180) {
                float padding = getPadding();
                float x = padding;
                float y = padding;
                float w = getWidth() - 2 * padding;
                float h = getHeight() - 2 * padding;
                if (w == h) { //adjust width if needed to make room for tapping
                    w = h / ASPECT_RATIO;
                }
                g.startRotateTransform(x + w / 2, y + h / 2, 180);
                try {
                    super.draw(g);
                } finally {
                    g.endTransform(); // never leave a transform open if drawing the card throws
                }
            } else {
                super.draw(g);
            }
            if (ghostKind != GhostKind.NONE) {
                drawGhostOverlay(g);
            }
        }
    }
}