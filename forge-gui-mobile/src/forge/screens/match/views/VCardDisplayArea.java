package forge.screens.match.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;

import forge.Forge;
import forge.Graphics;
import forge.card.CardRenderer.CardStackPosition;
import forge.card.CardZoom;
import forge.card.CardZoom.ActivateHandler;
import forge.game.card.CardView;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.screens.match.MatchController;
import forge.toolbox.FCardPanel;
import forge.toolbox.FDisplayObject;
import forge.util.ThreadUtil;

public abstract class VCardDisplayArea extends VDisplayArea implements ActivateHandler {
    private static final float CARD_STACK_OFFSET = 0.2f;

    protected final List<CardView> orderedCards = new ArrayList<>();
    protected final List<CardAreaPanel> cardPanels = new ArrayList<>();
    private boolean rotateCards180;

    public Iterable<CardView> getOrderedCards() {
        return orderedCards;
    }

    public Iterable<CardAreaPanel> getCardPanels() {
        return cardPanels;
    }

    @Override
    public int getCount() {
        return cardPanels.size();
    }

    @Override
    public void setRotate180(boolean b0) {
        //only rotate cards themselves
        rotateCards180 = b0;
    }

    protected void refreshCardPanels(Iterable<CardView> model) {
        clear();

        CardAreaPanel newCardPanel = null;
        if (model != null) {
            for (CardView card : model) {
                CardAreaPanel cardPanel = CardAreaPanel.get(card);
                addCardPanelToDisplayArea(cardPanel);
                cardPanels.add(cardPanel);
                if (newCardPanel == null && !orderedCards.contains(card)) {
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
        if (isVisible() == b0) { return; }
        super.setVisible(b0);
        if (b0) { //when zone becomes visible, ensure display area of panels is updated and panels layed out
            for (CardAreaPanel pnl : cardPanels) {
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
        cardPanels.remove(fromPanel);
        remove(fromPanel);
    }

    protected void clearChildren() {
        super.clear();
    }

    @Override
    public void clear() {
        super.clear();
        if (!cardPanels.isEmpty()) {
            for (CardAreaPanel panel : cardPanels) {
                if (panel.displayArea == null || panel.displayArea == this ||
                        !panel.displayArea.cardPanels.contains(panel)) { //don't reset if panel's displayed in another area already
                    panel.reset();
                }
            }
            cardPanels.clear();
        }
    }

    private final int addCards(CardAreaPanel cardPanel, float x, float y, float cardWidth, float cardHeight) {
        int totalCount = 0;
        List<CardAreaPanel> attachedPanels = cardPanel.getAttachedPanels();
        if (!attachedPanels.isEmpty()) {
            for (int i = attachedPanels.size() - 1; i >= 0; i--) {
                int count = addCards(attachedPanels.get(i), x, y, cardWidth, cardHeight);
                x += count * cardWidth * CARD_STACK_OFFSET;
                totalCount += count;
            }
        }

        orderedCards.add(cardPanel.getCard());
        cardPanel.setBounds(x, y, cardWidth, cardHeight);

        if (cardPanel.getNextPanelInStack() != null) { //add next panel in stack if needed
            x += cardWidth * CARD_STACK_OFFSET;
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
        orderedCards.clear();

        float x = 0;
        float y = 0;
        float cardHeight = visibleHeight;
        float cardWidth = getCardWidth(cardHeight);

        for (CardAreaPanel cardPanel : cardPanels) {
            int count = addCards(cardPanel, x, y, cardWidth, cardHeight);
            x += cardWidth + (count - 1) * cardWidth * CARD_STACK_OFFSET;
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
        if(!GuiBase.isNetworkplay()) //causes lag on netplay client side
            return MatchController.instance.getGameController().getActivateDescription(orderedCards.get(index));

        return "Activate | Cast | Play (if allowed)"; //simple text on card zoom swipe up
    }

    @Override
    public void setSelectedIndex(int index) {
        //just scroll card into view
        if (index < orderedCards.size()) {
            final CardAreaPanel cardPanel = CardAreaPanel.get(orderedCards.get(index));
            scrollIntoView(cardPanel);
        }
    }

    @Override
    public void activate(int index) {
        final CardAreaPanel cardPanel = CardAreaPanel.get(orderedCards.get(index));
        ThreadUtil.invokeInGameThread(new Runnable() { //must invoke in game thread in case a dialog needs to be shown
            @Override
            public void run() {
                cardPanel.selectCard(false);
            }
        });
    }

    public static class CardAreaPanel extends FCardPanel {
        private static final Map<Integer, CardAreaPanel> allCardPanels = new HashMap<>();

        public static CardAreaPanel get(CardView card0) {
            CardAreaPanel cardPanel = allCardPanels.get(card0.getId());
            if (cardPanel == null || cardPanel.getCard() != card0) { //replace card panel if card copied
                cardPanel = new CardAreaPanel(card0);
                allCardPanels.put(card0.getId(), cardPanel);
            }
            return cardPanel;
        }

        public static void resetForNewGame() {
            for (CardAreaPanel cardPanel : allCardPanels.values()) {
                cardPanel.displayArea = null;
                cardPanel.attachedToPanel = null;
                cardPanel.attachedPanels.clear();
                cardPanel.prevPanelInStack = null;
                cardPanel.nextPanelInStack = null;
            }
            allCardPanels.clear();
        }

        private VCardDisplayArea displayArea;
        private CardAreaPanel attachedToPanel;
        private final List<CardAreaPanel> attachedPanels = new ArrayList<>();
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
       
            if (card.getAttachedTo() != null) {
                setAttachedToPanel(CardAreaPanel.get(card.getAttachedTo()));
            }
            else {
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
            if (renderedCardContains(x, y)) {
                ThreadUtil.invokeInGameThread(new Runnable() { //must invoke in game thread in case a dialog needs to be shown
                    @Override
                    public void run() {
                        if (GuiBase.getInterface().isRunningOnDesktop() && Forge.mouseButtonID == Input.Buttons.RIGHT) {
                            FThreads.invokeInEdtLater(new Runnable() {
                                @Override
                                public void run() {
                                    showZoom();
                                }
                            });
                            return;
                        } else if (!selectCard(false)) {
                            //if no cards in stack can be selected, just show zoom/details for card
                            FThreads.invokeInEdtLater(new Runnable() {
                                @Override
                                public void run() {
                                    showZoom();
                                }
                            });
                        }
                    }
                });
                return true;
            }
            return false;
        }

        @Override
        public boolean flick(float x, float y) {
            if (renderedCardContains(x, y)) {
                ThreadUtil.invokeInGameThread(new Runnable() { //must invoke in game thread in case a dialog needs to be shown
                    @Override
                    public void run() {
                        selectCard(true);
                    }
                });
                return true;
            }
            return false;
        }

        public boolean selectCard(boolean selectEntireStack) {
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

        private void showZoom() {
            if (displayArea == null) { return; }

            final List<CardView> cards = displayArea.orderedCards;
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
            if (!selectOtherCardsInStack) { return null; }

            //on double-tap select all other cards in stack if any
            if (prevPanelInStack == null && nextPanelInStack == null) { return null; }

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
            if (displayArea == null || !displayArea.isVisible()) { return null; }

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
            }
            else {
                super.draw(g);
            }
        }
    }
}
