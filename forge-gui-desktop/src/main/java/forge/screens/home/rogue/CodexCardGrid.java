package forge.screens.home.rogue;

import forge.item.PaperCard;
import forge.toolbox.FSkin;
import forge.view.arcane.CardPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.JPanel;
import javax.swing.Scrollable;

class CodexCardGrid extends FSkin.SkinnedPanel implements Scrollable {
    enum CardState {
        HIDDEN,
        GREYED,
        NORMAL
    }

    record Entry(PaperCard card, String label, String tooltip, CardState state) {
    }

    private static final int BASE_CARD_WIDTH = 240;
    private static final int GAP = 15;
    private static final int PADDING = 15;

    CodexCardGrid(List<Entry> entries, Supplier<CardUtil> zoomUtilSupplier) {
        setOpaque(false);
        setLayout(null);
        for (Entry entry : entries) {
            add(createEntryPanel(entry, zoomUtilSupplier));
        }
    }

    private JPanel createEntryPanel(Entry entry, Supplier<CardUtil> zoomUtilSupplier) {
        if (entry.card() != null) {
            CodexReadonlyCardPanel panel = new CodexReadonlyCardPanel(entry.card(), zoomUtilSupplier, entry.state());
            panel.setToolTipText(entry.tooltip());
            return panel;
        }
        JPanel panel = new FSkin.SkinnedPanel();
        panel.setOpaque(false);
        return panel;
    }

    @Override
    public void doLayout() {
        int cardWidth = getCardWidth(getWidth());
        int cardHeight = Math.round(cardWidth * CardPanel.ASPECT_RATIO);
        int columns = getColumnCount(getWidth());

        int rowWidth = columns * cardWidth + (columns - 1) * GAP;
        int startX = Math.max(PADDING, (getWidth() - rowWidth) / 2);
        int y = PADDING;
        for (int i = 0; i < getComponentCount(); i++) {
            int column = i % columns;
            int row = i / columns;
            if (column == 0 && row > 0) {
                y += cardHeight + GAP;
            }
            getComponent(i).setBounds(startX + column * (cardWidth + GAP), y, cardWidth, cardHeight);
        }

        Dimension preferredSize = getPreferredSizeForWidth(getWidth());
        if (!preferredSize.equals(getPreferredSize())) {
            setPreferredSize(preferredSize);
            revalidate();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        int width = getWidth();
        if (width <= 0 && getParent() != null) {
            width = getParent().getWidth();
        }
        if (width <= 0) {
            width = getDefaultWidth();
        }
        return getPreferredSizeForWidth(width);
    }

    int getPreferredHeightForWidth(int width) {
        return getPreferredSizeForWidth(width).height;
    }

    static int getDefaultWidth() {
        return BASE_CARD_WIDTH * 3 + GAP * 2 + PADDING * 2;
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 32;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return Math.max(32, visibleRect.height - GAP);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    private Dimension getPreferredSizeForWidth(int width) {
        int columns = getColumnCount(width);
        int cardHeight = Math.round(getCardWidth(width) * CardPanel.ASPECT_RATIO);
        int rows = (int) Math.ceil(getComponentCount() / (double) columns);
        int preferredHeight = rows == 0 ? PADDING * 2 : PADDING * 2 + rows * cardHeight + (rows - 1) * GAP;
        return new Dimension(width, preferredHeight);
    }

    private int getAvailableWidth(int width) {
        return Math.max(1, width - PADDING * 2);
    }

    private int getCardWidth(int width) {
        return Math.min(BASE_CARD_WIDTH, getAvailableWidth(width));
    }

    private int getColumnCount(int width) {
        int availableWidth = getAvailableWidth(width);
        if (availableWidth < BASE_CARD_WIDTH) {
            return 1;
        }
        return Math.max(1, (availableWidth + GAP) / (BASE_CARD_WIDTH + GAP));
    }

    private static class CodexReadonlyCardPanel extends SelectableCardPanelBase {
        private final CardState state;

        private CodexReadonlyCardPanel(PaperCard card, Supplier<CardUtil> zoomUtilSupplier, CardState state) {
            super(card, zoomUtilSupplier, state == CardState.HIDDEN,
                    ZoomActivation.MIDDLE_MOUSE_BUTTON);
            this.state = state;
        }

        @Override
        protected void toggleSelection() {
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            if (state == CardState.GREYED && !faceDown) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(new Color(70, 70, 70, 145));
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
            if (hovered && (faceDown || state == CardState.GREYED)) {
                drawHoverHighlight((Graphics2D) g, getWidth(), getHeight());
            }
        }
    }

}
