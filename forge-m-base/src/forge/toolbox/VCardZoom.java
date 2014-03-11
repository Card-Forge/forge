package forge.toolbox;

import java.util.List;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinColor.Colors;
import forge.game.card.Card;
import forge.toolbox.FList.ListItemRenderer;

public class VCardZoom extends FOverlay {
    private final static FSkinColor BACK_COLOR = FSkinColor.get(Colors.CLR_THEME).alphaColor(0.9f);

    private final CardSlider slider;
    private final FList<Object> optionList;
    private List<Card> orderedCards;
    private int selectedIndex = -1;

    @SuppressWarnings("rawtypes")
    private ZoomController controller;

    public VCardZoom() {
        slider = add(new CardSlider());
        optionList = add(new FList<Object>() {
            @Override
            protected void drawBackground(Graphics g) {
                g.fillRect(BACK_COLOR, 0, 0, getWidth(), getHeight());
            }

            @Override
            protected void drawOverlay(Graphics g) { //draw top border
                g.drawLine(1, FList.LINE_COLOR, 0, 0, getWidth(), 0);
            }
        });
        optionList.setListItemRenderer(new ListItemRenderer<Object>() {
            @Override
            @SuppressWarnings("unchecked")
            public boolean tap(Object value, float x, float y, int count) {
                if (controller.selectOption(orderedCards.get(selectedIndex), value)) {
                    hide();
                }
                return true;
            }

            @Override
            public float getItemHeight() {
                return optionList.getHeight();
            }

            @Override
            public void drawValue(Graphics g, Object value, FSkinFont font, FSkinColor foreColor, float width, float height) {
                float x = width * FList.INSETS_FACTOR;
                float y = 3;
                g.startClip(0, 0, width, height);
                g.drawText(value.toString(), font, foreColor, x, y, width - 2 * x, height - 2 * y, true, HAlignment.LEFT, true);
                g.endClip();
            }
        });
    }

    public static abstract class ZoomController<T> {
        public abstract List<T> getOptions(Card card);
        public abstract boolean selectOption(Card card, T option);
    }

    public <T> void show(final Card card0, final List<Card> orderedCards0, ZoomController<?> controller0) {
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
            selectedIndex = -1;
            optionList.clear();
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

        optionList.clear();
        List<?> options = controller.getOptions(selectedCard);
        if (options == null || options.isEmpty()) {
            
        }
        else {
            for (Object option : options) {
                optionList.addItem(option);
            }
        }
        optionList.revalidate();
    }

    @Override
    protected void doLayout(float width, float height) {
        float y = height * 0.2f;
        float h = height * 0.7f;
        slider.setBounds(0, y, width, h);
        y += h;
        optionList.setBounds(0, y, width, height - y);
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
