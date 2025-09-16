package forge.screens.match.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import forge.Forge;
import forge.Graphics;
import forge.card.CardRenderer.CardStackPosition;
import forge.card.CardZoom;
import forge.card.CardZoom.ActivateHandler;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.screens.match.MatchController;
import forge.toolbox.FCardPanel;
import forge.toolbox.FDisplayObject;
import forge.util.ThreadUtil;
import io.sentry.Sentry;

public abstract class VCardDisplayArea extends VDisplayArea implements ActivateHandler {
    private static final float CARD_STACK_OFFSET = 0.2f;

    protected Supplier<List<CardView>> orderedCards = Suppliers.memoize(ArrayList::new);
    protected Supplier<List<CardAreaPanel>> cardPanels = Suppliers.memoize(ArrayList::new);
    private boolean rotateCards180;

    public Iterable<CardView> getOrderedCards() {
        return orderedCards.get();
    }

    public Iterable<CardAreaPanel> getCardPanels() {
        return cardPanels.get();
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
        if (Forge.altZoneTabs && "Horizontal".equalsIgnoreCase(Forge.altZoneTabMode))
            return 0.125f;
        return CARD_STACK_OFFSET;
    }

    protected void refreshCardPanels(Iterable<CardView> model) {
        clear();

        CardAreaPanel newCardPanel = null;
        if (model != null) {
            for (CardView card : model) {
                CardAreaPanel cardPanel = CardAreaPanel.get(card);
                addCardPanelToDisplayArea(cardPanel);
                cardPanels.get().add(cardPanel);
                if (newCardPanel == null && !orderedCards.get().contains(card)) {
                    newCardPanel = cardPanel;
                }
            }
        }
        if (isVisible()) { //only revalidate if currently visible
            revalidate();

            if (newCardPanel != null) { //if new cards added, ensure first new card is scrolled into view
                scrollIntoView(newCardPanel);
            }
        }
    }

    @Override
    public void setVisible(boolean b0) {
        if (isVisible() == b0) {
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
    private void addCardPanelToDisplayArea(CardAreaPanel cardPanel) {
        do {
            List<CardAreaPanel> attachedPanels = cardPanel.getAttachedPanels();
            if (!attachedPanels.isEmpty()) {
                for (int i = attachedPanels.size() - 1; i >= 0; i--) {
                    addCardPanelToDisplayArea(attachedPanels.get(i));
                }
            }

            if (isVisible()) { //only set display area for card if area is visible
                cardPanel.displayArea = this;
            }
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
        orderedCards.get().clear();

        float x = 0;
        float y = 0;
        float cardHeight = visibleHeight;
        float cardWidth = getCardWidth(cardHeight);

        for (CardAreaPanel cardPanel : new ArrayList<>(cardPanels.get())) {
            if (cardPanel != null) {
                int count = addCards(cardPanel, x, y, cardWidth, cardHeight);
                x += cardWidth + (count - 1) * cardWidth * getCardStackOffset();
            }
        }

        return new ScrollBounds(x, visibleHeight);
    }

    @Override
    protected void startClip(Graphics g) {
        //prevent clipping top and bottom
        float h = getHeight();
        g.startClip(0, -h, getWidth(), 3 * h);
    }

    @Override
    public String getActivateAction(int index) {
        if (!GuiBase.isNetworkplay()) {
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
            final CardAreaPanel cardPanel = CardAreaPanel.get(orderedCards.get().get(index));
            scrollIntoView(cardPanel);
        }
    }

    @Override
    public void activate(int index) {
        final CardAreaPanel cardPanel = CardAreaPanel.get(orderedCards.get().get(index));
        //must invoke in game thread in case a dialog needs to be shown
        ThreadUtil.invokeInGameThread(() -> cardPanel.selectCard(false));
    }

    public static class CardAreaPanel extends FCardPanel {
        private static Map<Integer, CardAreaPanel> allCardPanels = new HashMap<>();

        public static CardAreaPanel get(CardView card0) {
            CardAreaPanel cardPanel = allCardPanels.get(card0.getId());
            if (cardPanel == null || cardPanel.getCard() != card0) { //replace card panel if card copied
                cardPanel = new CardAreaPanel(card0);
                allCardPanels.put(card0.getId(), cardPanel);
            }
            return cardPanel;
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
        }

        private VCardDisplayArea displayArea;
        private CardAreaPanel attachedToPanel;
        private List<CardAreaPanel> attachedPanels = new ArrayList<>();
        private CardAreaPanel nextPanelInStack, prevPanelInStack;

        //use static get(card) function instead
        private CardAreaPanel(CardView card0) {
            super(card0);
        }

        public VCardDisplayArea getDisplayArea() {
            return displayArea;
        }

        public CardAreaPanel getAttachedToPanel() {
            return attachedToPanel;
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
            setTapped(card.isTapped());

            attachedPanels.clear();

            if (card.hasAnyCardAttachments()) {
                final Iterable<CardView> enchants = card.getAllAttachedCards();
                for (final CardView e : enchants) {
                    final CardAreaPanel cardE = CardAreaPanel.get(e);
                    if (cardE != null) {
                        attachedPanels.add(cardE);
                    }
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
            CardView cardView = getCard();
            if (cardView != null) {
                PlayerView cardController = cardView.getController();
                PlayerView currentPlayer = MatchController.instance.getCurrentPlayer();
                if (cardController != null) {
                    /* TODO:
                        IIRC this check is for mobile UI BUG that can cast nonland card as long as you can view it
                        on any hand. Seems ridiculous, Investigate further. Should be rule based and this isn't needed.
                        To reproduce omit this check and select nonland card on opponent hand while you have
                        Telepathy card in play. */
                    if (!cardController.equals(currentPlayer) && ZoneType.Hand.equals(cardView.getZone()))
                        if (cardView.mayPlayerLook(currentPlayer)) { // can see the card, check if can play...
                            if (!cardView.getMayPlayPlayers(currentPlayer))
                                return false;
                        } else {
                            return false;
                        }
                }
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
                if (panel.selectCard(selectEntireStack)) {
                    Gdx.graphics.requestRendering();
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean longPress(float x, float y) {
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
                super.draw(g);
                g.endTransform();
            } else {
                super.draw(g);
            }
        }
    }
}
