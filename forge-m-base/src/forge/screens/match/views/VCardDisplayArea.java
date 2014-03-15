package forge.screens.match.views;

import java.util.ArrayList;
import java.util.List;

import forge.FThreads;
import forge.game.card.Card;
import forge.screens.match.FControl;
import forge.toolbox.FCardPanel;

public abstract class VCardDisplayArea extends VDisplayArea {
    private static final float CARD_STACK_OFFSET = 0.2f;

    private final List<Card> orderedCards = new ArrayList<Card>();
    private final List<FCardPanel> cardPanels = new ArrayList<FCardPanel>();

    public Iterable<FCardPanel> getCardPanels() {
        return cardPanels;
    }

    @Override
    public int getCount() {
        return cardPanels.size();
    }

    protected void refreshCardPanels(List<Card> model) {
        clear();
        for (Card card : model) {
            addCard(card);
        }
        revalidate();
    }

    public CardAreaPanel addCard(final Card card) {
        CardAreaPanel cardPanel = add(new CardAreaPanel(card));
        cardPanels.add(cardPanel);
        return cardPanel;
    }

    public final FCardPanel getCardPanel(final int gameCardID) {
        for (final FCardPanel panel : cardPanels) {
            if (panel.getCard().getUniqueNumber() == gameCardID) {
                return panel;
            }
        }
        return null;
    }

    public final void removeCardPanel(final FCardPanel fromPanel) {
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
        cardPanels.clear();
    }

    protected void startLayout() {
        orderedCards.clear();
    }

    protected final float layoutCardPanel(FCardPanel cardPanel, float x, float y, float cardWidth, float cardHeight) {
        int count = addCards(cardPanel, x, y, cardWidth, cardHeight);
        return cardWidth + (count - 1) * cardWidth * CARD_STACK_OFFSET;
    }

    private final int addCards(FCardPanel cardPanel, float x, float y, float cardWidth, float cardHeight) {
        int count = 0;
        List<FCardPanel> attachedPanels = cardPanel.getAttachedPanels();
        if (!attachedPanels.isEmpty()) {
            for (int i = attachedPanels.size() - 1; i >= 0; i--) {
                count += addCards(attachedPanels.get(i), x, y, cardWidth, cardHeight);
                x += count * cardWidth * CARD_STACK_OFFSET;
            }
        }
        orderedCards.add(cardPanel.getCard());
        cardPanel.setBounds(x, y, cardWidth, cardHeight);
        return count + 1;
    }

    @Override
    protected void doLayout(float width, float height) {
        startLayout();

        float x = 0;
        float y = 0;
        float cardHeight = height;
        float cardWidth = ((cardHeight - 2 * FCardPanel.PADDING) / FCardPanel.ASPECT_RATIO) + 2 * FCardPanel.PADDING; //ensure aspect ratio maintained after padding applied

        for (FCardPanel cardPanel : cardPanels) {
            x += layoutCardPanel(cardPanel, x, y, cardWidth, cardHeight);
        }
    }

    protected class CardAreaPanel extends FCardPanel {
        private CardAreaPanel(Card card0) {
            super(card0);
        }

        @Override
        public boolean tap(float x, float y, int count) {
            FControl.getInputProxy().selectCard(getCard(), new ArrayList<Card>(orderedCards)); //copy list to allow it being modified
            return true;
        }
    }
}
