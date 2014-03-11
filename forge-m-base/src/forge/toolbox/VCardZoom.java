package forge.toolbox;

import java.util.List;

import forge.game.card.Card;

public class VCardZoom extends FOverlay {
    private final CardSlider slider;
    private List<Card> orderedCards;
    private int selectedIndex;
    private ZoomController<?> controller;

    public VCardZoom() {
        slider = add(new CardSlider());
    }

    public static abstract class ZoomController<T> {
        public abstract List<T> getOptions(Card card);
        public abstract boolean selectOption(Card card, T option);
    }

    public void show(final Card card0, final List<Card> orderedCards0, ZoomController<?> controller0) {
        if (orderedCards0.isEmpty()) {
            return;
        }

        orderedCards = orderedCards0;
        controller = controller0;

        int index = orderedCards.indexOf(card0);
        if (index == -1) {
            index = 0;
        }
        setSelectedIndex(index);
        setVisible(true);
    }

    public void hide() {
        if (isVisible()) {
            orderedCards = null; //clear when hidden
            controller = null;
            setVisible(false);
        }
    }

    private void setSelectedIndex(int selectedIndex0) {
        if (selectedIndex0 < 0) {
            selectedIndex0 = 0;
        }
        else if (selectedIndex0 >= orderedCards.size()) {
            selectedIndex0 = orderedCards.size() - 1;
        }

        if (selectedIndex == selectedIndex0) { return; }
        selectedIndex = selectedIndex0;

        Card selectedCard = orderedCards.get(selectedIndex);
        slider.frontPanel.setCard(selectedCard);
        slider.leftPanel.setCard(selectedIndex > 0 ? orderedCards.get(selectedIndex - 1) : null);
        slider.rightPanel.setCard(selectedIndex < orderedCards.size() - 1 ? orderedCards.get(selectedIndex + 1) : null);
    }

    @Override
    protected void doLayout(float width, float height) {
        float y = height * 0.2f;
        float h = height * 0.7f;
        slider.setBounds(0, y, width, h);
    }

    private class CardSlider extends FContainer {
        private final FCardPanel frontPanel, leftPanel, rightPanel;

        private CardSlider() {
            leftPanel = add(new FCardPanel() {
                @Override
                public boolean tap(float x, float y, int count) {
                    setSelectedIndex(selectedIndex - 1);
                    return true;
                }
            });
            rightPanel = add(new FCardPanel() {
                @Override
                public boolean tap(float x, float y, int count) {
                    setSelectedIndex(selectedIndex + 1);
                    return true;
                }
            });
            frontPanel = add(new FCardPanel() {
                @Override
                public boolean tap(float x, float y, int count) {
                    hide();
                    return true;
                }
            });
        }

        @Override
        protected void doLayout(float width, float height) {
            float backPanelHeight = height * 0.8f;
            float backPanelWidth = backPanelHeight / FCardPanel.ASPECT_RATIO;
            float y = (height - backPanelHeight) / 2;
            leftPanel.setBounds(0, y, backPanelWidth, backPanelHeight);
            rightPanel.setBounds(width - backPanelWidth, y, backPanelWidth, backPanelHeight);

            float frontPanelWidth = height / FCardPanel.ASPECT_RATIO;
            frontPanel.setBounds((width - frontPanelWidth) / 2, 0, frontPanelWidth, height);
        }
    }
}
