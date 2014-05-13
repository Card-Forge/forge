package forge.screens.match.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import forge.FThreads;
import forge.card.CardZoom;
import forge.game.card.Card;
import forge.screens.match.FControl;
import forge.toolbox.FCardPanel;
import forge.util.ThreadUtil;

public abstract class VCardDisplayArea extends VDisplayArea {
    private static final float CARD_STACK_OFFSET = 0.2f;

    private final List<Card> orderedCards = new ArrayList<Card>();
    private final List<CardAreaPanel> cardPanels = new ArrayList<CardAreaPanel>();

    public Iterable<CardAreaPanel> getCardPanels() {
        return cardPanels;
    }

    @Override
    public int getCount() {
        return cardPanels.size();
    }

    protected void refreshCardPanels(List<Card> model) {
        clear();

        CardAreaPanel newCardPanel = null;
        for (Card card : model) {
            CardAreaPanel cardPanel = CardAreaPanel.get(card);
            addCardPanelToDisplayArea(cardPanel);
            cardPanels.add(cardPanel);
            if (newCardPanel == null && !orderedCards.contains(card)) {
                newCardPanel = cardPanel;
            }
        }
        revalidate();

        if (newCardPanel != null) { //if new cards added, ensure first new card is scrolled into view
            scrollIntoView(newCardPanel);
        }
    }

    //support adding card panel and attached panels to display area recursively
    private void addCardPanelToDisplayArea(CardAreaPanel cardPanel) {
        List<CardAreaPanel> attachedPanels = cardPanel.getAttachedPanels();
        if (!attachedPanels.isEmpty()) {
            for (int i = attachedPanels.size() - 1; i >= 0; i--) {
                addCardPanelToDisplayArea(attachedPanels.get(i));
            }
        }
        cardPanel.displayArea = this;
        add(cardPanel);
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

    @Override
    public void clear() {
        super.clear();
        if (!cardPanels.isEmpty()) {
            for (CardAreaPanel panel : cardPanels) {
                panel.attachedPanels.clear();
                panel.attachedToPanel = null;
                panel.displayArea = null;
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
        return totalCount + 1;
    }

    protected float getCardWidth(float cardHeight) {
        return (cardHeight - 2 * CardAreaPanel.PADDING) / CardAreaPanel.ASPECT_RATIO + 2 * CardAreaPanel.PADDING; //ensure aspect ratio maintained after padding applied
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

    public static class CardAreaPanel extends FCardPanel {
        private static final Map<Integer, CardAreaPanel> allCardPanels = new HashMap<Integer, CardAreaPanel>();

        public static CardAreaPanel get(Card card0) {
            CardAreaPanel cardPanel = allCardPanels.get(card0.getUniqueNumber());
            if (cardPanel == null) {
                cardPanel = new CardAreaPanel(card0);
                allCardPanels.put(card0.getUniqueNumber(), cardPanel);
            }
            return cardPanel;
        }

        public static void resetForNewGame() {
            for (CardAreaPanel cardPanel : allCardPanels.values()) {
                cardPanel.displayArea = null;
                cardPanel.attachedToPanel = null;
                cardPanel.attachedPanels.clear();
            }
            allCardPanels.clear();
        }

        private VCardDisplayArea displayArea;
        private CardAreaPanel attachedToPanel;
        private final List<CardAreaPanel> attachedPanels = new ArrayList<CardAreaPanel>();

        //use static get(card) function instead
        private CardAreaPanel(Card card0) {
            super(card0);
        }

        public VCardDisplayArea getDisplayArea() {
            return displayArea;
        }

        public void setDisplayArea(VCardDisplayArea displayArea0) {
            displayArea = displayArea0;
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

        @Override
        public boolean tap(float x, float y, int count) {
            ThreadUtil.invokeInGameThread(new Runnable() { //must invoke in game thread in case a dialog needs to be shown
                @Override
                public void run() {
                    FControl.getInputProxy().selectCard(getCard(), null);
                }
            });
            return true;
        }

        @Override
        public boolean longPress(float x, float y) {
            CardZoom.show(getCard());
            return true;
        }
    }
}
