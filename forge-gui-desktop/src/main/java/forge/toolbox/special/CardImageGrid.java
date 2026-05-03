package forge.toolbox.special;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.tuple.Pair;

import forge.ImageCache;
import forge.StaticData;
import forge.card.CardEdition;
import forge.game.card.CardView;
import forge.gui.GuiBase;
import forge.item.PaperCard;
import forge.toolbox.FList;
import forge.toolbox.FScrollPane;
import forge.toolbox.IDisposable;
import forge.util.ImageFetcher;
import forge.view.arcane.CardPanel;

/**
 * Reusable Swing grid of card thumbnails. Each cell shows a card image with a footer underneath.
 * Filter-agnostic — callers compose their own search bar over {@link #setItems}.
 */
public final class CardImageGrid<T> implements IDisposable {

    public interface CellAdapter<T> {
        String imageKey(T item);
        String footerHtml(T item);
        default int footerHeight() { return 60; }
    }

    private static final int SCROLLBAR_W = 16;
    // FScrollPane themed-border slack — without it AS_NEEDED triggers and HORIZONTAL_WRAP drops a column.
    private static final int VIEWPORT_BUFFER = 12;

    private final int thumbW;
    private final int thumbH;
    private final int footerH;
    private final int cellW;
    private final int cellH;
    private final int columns;
    private final CellAdapter<T> adapter;

    private final DefaultListModel<T> model = new DefaultListModel<>();
    private final FList<T> list;
    private final FScrollPane scrollPane;
    private final Map<String, Icon> iconCache = new HashMap<>();

    public CardImageGrid(int columns, int thumbW, int thumbH, CellAdapter<T> adapter) {
        this.columns = columns;
        this.thumbW = thumbW;
        this.thumbH = thumbH;
        this.footerH = adapter.footerHeight();
        this.cellW = thumbW + 16;
        // 20 = 8+4+8 chrome (top border, vgap, bottom border); less and the icon overflows imageLabel, clipping the selection ring corners.
        this.cellH = thumbH + footerH + 20;
        this.adapter = adapter;

        this.list = new FList<T>(model) {
            private static final long serialVersionUID = 1L;

            @Override
            public Dimension getPreferredScrollableViewportSize() {
                return new Dimension(CardImageGrid.this.columns * cellW, 2 * cellH);
            }

            // Default JList getPreferredSize over-estimates pre-layout (assumes 1 col); override to bound the scrollbar.
            @Override
            public Dimension getPreferredSize() {
                final int rows = Math.max(1, (getModel().getSize() + CardImageGrid.this.columns - 1) / CardImageGrid.this.columns);
                return new Dimension(CardImageGrid.this.columns * cellW, rows * cellH);
            }
        };
        list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        list.setVisibleRowCount(0);
        list.setFixedCellWidth(cellW);
        list.setFixedCellHeight(cellH);
        list.setCellRenderer(new GridCellRenderer());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        this.scrollPane = new FScrollPane(list, true,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setWheelScrollingEnabled(true);
        list.addMouseWheelListener(e ->
                scrollPane.dispatchEvent(SwingUtilities.convertMouseEvent(list, e, scrollPane)));
    }

    public static CardImageGrid<PaperCard> forPaperCards(int cols, int thumbW, int thumbH) {
        return new CardImageGrid<>(cols, thumbW, thumbH, new PaperCardCellAdapter());
    }

    public static CardImageGrid<CardView> forCardViews(int cols, int thumbW, int thumbH) {
        return new CardImageGrid<>(cols, thumbW, thumbH, new CardViewCellAdapter());
    }

    public JComponent getComponent() {
        return scrollPane;
    }

    public FList<T> getList() {
        return list;
    }

    public int getCellWidth() {
        return cellW;
    }

    public int getCellHeight() {
        return cellH;
    }

    public JComponent makeFixedSizeContainer(int maxVisibleRows) {
        final int rows = Math.max(1, (model.getSize() + columns - 1) / columns);
        final int visibleRows = Math.min(rows, maxVisibleRows);
        final boolean needsScroll = rows > maxVisibleRows;
        final int scrollbarReservation = needsScroll ? SCROLLBAR_W : 0;
        final Dimension fixedSize = new Dimension(
                columns * cellW + scrollbarReservation + VIEWPORT_BUFFER,
                visibleRows * cellH + VIEWPORT_BUFFER);
        final JPanel container = new JPanel(new BorderLayout()) {
            private static final long serialVersionUID = 1L;
            @Override public Dimension getPreferredSize() { return fixedSize; }
            @Override public Dimension getMinimumSize()   { return fixedSize; }
            @Override public Dimension getMaximumSize()   { return fixedSize; }
        };
        container.setOpaque(false);
        container.add(scrollPane, BorderLayout.CENTER);
        return container;
    }

    /** Replace the items, preserving selection when the previously-selected card is still present. */
    public void setItems(List<T> items) {
        final T previous = list.getSelectedValue();
        model.clear();
        for (T item : items) {
            model.addElement(item);
        }
        if (previous != null) {
            final int idx = model.indexOf(previous);
            if (idx >= 0) {
                list.setSelectedIndex(idx);
            }
        }
    }

    public boolean isEmpty() {
        return model.isEmpty();
    }

    public T getSelected() {
        return list.getSelectedValue();
    }

    public void setSelected(T item) {
        final int idx = model.indexOf(item);
        if (idx >= 0) {
            list.setSelectedIndex(idx);
            list.ensureIndexIsVisible(idx);
        }
    }

    public void addDoubleClickListener(Consumer<T> listener) {
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    final T sel = list.getSelectedValue();
                    if (sel != null) {
                        listener.accept(sel);
                    }
                }
            }
        });
    }

    @Override
    public void dispose() {
        iconCache.clear();
    }

    // Draws against the destination Graphics2D so HiDPI scaling stays in one step; pre-rendering to a BufferedImage adds a blurring upscale.
    private static final class HighQualityScaledIcon implements Icon {
        private final Image source;
        private final int displayW;
        private final int displayH;

        HighQualityScaledIcon(Image source, int displayW, int displayH) {
            this.source = source;
            this.displayW = displayW;
            this.displayH = displayH;
        }

        @Override public int getIconWidth()  { return displayW; }
        @Override public int getIconHeight() { return displayH; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            final Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.drawImage(source, x, y, displayW, displayH, null);
            g2.dispose();
        }
    }

    private static final class PaperCardCellAdapter implements CellAdapter<PaperCard> {
        @Override
        public String imageKey(PaperCard pc) {
            return pc.getImageKey(false);
        }
        @Override
        public String footerHtml(PaperCard pc) {
            final String editionCode = pc.getEdition();
            final CardEdition edition = StaticData.instance().getEditions().get(editionCode);
            final String editionName = edition != null ? edition.getName() : editionCode;
            return String.format("<html><center>%s<br><i>(%s)</i> #%s</center></html>",
                    editionName, editionCode, pc.getCollectorNumber());
        }
    }

    private static final class CardViewCellAdapter implements CellAdapter<CardView> {
        @Override
        public String imageKey(CardView cv) {
            return cv.getCurrentState().getImageKey();
        }
        @Override
        public String footerHtml(CardView cv) {
            String name = cv.getCurrentState().getName();
            if (name == null || name.isEmpty()) {
                name = "—";
            }
            return String.format("<html><center>%s</center></html>", name);
        }
        @Override public int footerHeight() { return 24; }
    }

    private final class GridCellRenderer extends JPanel implements ListCellRenderer<T> {
        private static final long serialVersionUID = 1L;

        private final JLabel imageLabel = new JLabel();
        private final JLabel textLabel = new JLabel();
        private boolean cellSelected;

        GridCellRenderer() {
            super(new BorderLayout(0, 4));
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
            textLabel.setHorizontalAlignment(SwingConstants.CENTER);
            textLabel.setVerticalAlignment(SwingConstants.TOP);
            textLabel.setPreferredSize(new Dimension(thumbW, footerH));
            add(imageLabel, BorderLayout.CENTER);
            add(textLabel, BorderLayout.SOUTH);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends T> source,
                T value, int index, boolean isSelected, boolean cellHasFocus) {

            textLabel.setText(adapter.footerHtml(value));
            imageLabel.setIcon(getOrComputeIcon(value, source));
            setBackground(source.getBackground());
            textLabel.setForeground(source.getForeground());
            this.cellSelected = isSelected;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (!cellSelected) {
                return;
            }

            final Rectangle b = imageLabel.getBounds();
            if (b.width <= 0 || b.height <= 0) {
                return;
            }

            // Inflated rounded rect drawn before the image label paints; only the outer ring stays visible.
            final Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                final int n = Math.max(1, Math.round(b.width * CardPanel.SELECTED_BORDER_SIZE));
                final int corner = Math.max(4, Math.round(b.width * CardPanel.ROUNDED_CORNER_SIZE));
                g2.setColor(Color.green);
                g2.fillRoundRect(b.x - n, b.y - n,
                        b.width + 2 * n, b.height + 2 * n,
                        corner + n, corner + n);
            } finally {
                g2.dispose();
            }
        }

        private Icon getOrComputeIcon(T value, JList<? extends T> source) {
            final String imageKey = adapter.imageKey(value);
            final String cacheKey = imageKey + "#" + thumbW + "x" + thumbH;

            Icon cached = iconCache.get(cacheKey);
            if (cached != null) {
                return cached;
            }

            final Pair<BufferedImage, Boolean> info = ImageCache.getCardOriginalImageInfo(imageKey, true);
            final BufferedImage src = info.getLeft();
            final boolean isPlaceholder = Boolean.TRUE.equals(info.getRight());
            if (src == null) {
                return null;
            }

            // Bake corners at source resolution; clipping in paintIcon doesn't antialias the boundary on most JDKs.
            final int srcCorner = Math.max(4, Math.round(src.getWidth() * CardPanel.ROUNDED_CORNER_SIZE));
            final BufferedImage rounded = ImageCache.makeRoundedCorner(src, srcCorner);
            final Icon icon = new HighQualityScaledIcon(rounded, thumbW, thumbH);
            iconCache.put(cacheKey, icon);

            if (isPlaceholder) {
                final ImageFetcher fetcher = GuiBase.getInterface().getImageFetcher();
                if (fetcher != null) {
                    fetcher.fetchImage(imageKey, () -> {
                        iconCache.remove(cacheKey);
                        SwingUtilities.invokeLater(source::repaint);
                    });
                }
            }

            return icon;
        }
    }
}
