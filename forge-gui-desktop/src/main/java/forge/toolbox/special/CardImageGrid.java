package forge.toolbox.special;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
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
import forge.gui.GuiBase;
import forge.item.PaperCard;
import forge.toolbox.FList;
import forge.toolbox.FScrollPane;
import forge.toolbox.IDisposable;
import forge.util.ImageFetcher;

/**
 * Reusable Swing grid of card thumbnails. Each cell shows a card image with the edition name,
 * code, and collector number underneath. The widget is filter-agnostic — callers compose their
 * own search bar over {@link #setItems}.
 */
public final class CardImageGrid implements IDisposable {

    private final int thumbW;
    private final int thumbH;
    private final int cellW;
    private final int cellH;
    private final int columns;

    private final DefaultListModel<PaperCard> model = new DefaultListModel<>();
    private final JList<PaperCard> list;
    private final FScrollPane scrollPane;
    private final Map<String, Icon> iconCache = new HashMap<>();

    public CardImageGrid(int columns, int thumbW, int thumbH) {
        this.columns = columns;
        this.thumbW = thumbW;
        this.thumbH = thumbH;
        this.cellW = thumbW + 16;
        this.cellH = thumbH + 70;

        this.list = new FList<PaperCard>(model) {
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
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setWheelScrollingEnabled(true);
        list.addMouseWheelListener(e ->
                scrollPane.dispatchEvent(SwingUtilities.convertMouseEvent(list, e, scrollPane)));
    }

    public JComponent getComponent() {
        return scrollPane;
    }

    public int getCellWidth() {
        return cellW;
    }

    public int getCellHeight() {
        return cellH;
    }

    /** Replace the items, preserving selection when the previously-selected card is still present. */
    public void setItems(List<PaperCard> items) {
        final PaperCard previous = list.getSelectedValue();
        model.clear();
        for (PaperCard pc : items) {
            model.addElement(pc);
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

    public PaperCard getSelected() {
        return list.getSelectedValue();
    }

    public void setSelected(PaperCard pc) {
        final int idx = model.indexOf(pc);
        if (idx >= 0) {
            list.setSelectedIndex(idx);
            list.ensureIndexIsVisible(idx);
        }
    }

    public void addDoubleClickListener(Consumer<PaperCard> listener) {
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    final PaperCard sel = list.getSelectedValue();
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

    /**
     * Paints a high-resolution source image at smaller display size using BICUBIC against the destination Graphics2D.
     * Routes through the screen's HiDPI transform — pre-rendering into a user-space BufferedImage would let Swing
     * add a bilinear upscale and blur the output.
     */
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
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.drawImage(source, x, y, displayW, displayH, null);
            g2.dispose();
        }
    }

    private final class GridCellRenderer extends JPanel implements ListCellRenderer<PaperCard> {
        private static final long serialVersionUID = 1L;

        private final JLabel imageLabel = new JLabel();
        private final JLabel textLabel = new JLabel();

        GridCellRenderer() {
            super(new BorderLayout(0, 4));
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
            textLabel.setHorizontalAlignment(SwingConstants.CENTER);
            textLabel.setVerticalAlignment(SwingConstants.TOP);
            textLabel.setPreferredSize(new Dimension(thumbW, 60));
            add(imageLabel, BorderLayout.CENTER);
            add(textLabel, BorderLayout.SOUTH);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends PaperCard> source,
                PaperCard value, int index, boolean isSelected, boolean cellHasFocus) {
            final String editionCode = value.getEdition();
            final CardEdition edition = StaticData.instance().getEditions().get(editionCode);
            final String editionName = edition != null ? edition.getName() : editionCode;

            textLabel.setText(String.format(
                    "<html><center>%s<br><i>(%s)</i> #%s</center></html>",
                    editionName, editionCode, value.getCollectorNumber()));

            imageLabel.setIcon(getOrComputeIcon(value, source));

            if (isSelected) {
                setBackground(source.getSelectionBackground());
                textLabel.setForeground(source.getSelectionForeground());
            } else {
                setBackground(source.getBackground());
                textLabel.setForeground(source.getForeground());
            }
            return this;
        }

        private Icon getOrComputeIcon(PaperCard card, JList<? extends PaperCard> source) {
            final String imageKey = card.getImageKey(false);
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

            final Icon icon = new HighQualityScaledIcon(src, thumbW, thumbH);
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
