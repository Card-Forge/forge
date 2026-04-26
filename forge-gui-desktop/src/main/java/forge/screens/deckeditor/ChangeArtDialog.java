package forge.screens.deckeditor;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.ImmutableList;

import forge.ImageCache;
import forge.StaticData;
import forge.card.CardEdition;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.item.PaperCard;
import forge.itemmanager.CardManager;
import forge.toolbox.FComboBox;
import forge.toolbox.FLabel;
import forge.toolbox.FList;
import forge.toolbox.FOptionPane;
import forge.toolbox.FScrollPane;
import forge.toolbox.FTextField;
import forge.util.ImageFetcher;
import forge.util.Localizer;

public final class ChangeArtDialog {

    private static final int THUMB_W = 200;
    private static final int THUMB_H = 280;
    private static final int CELL_W = THUMB_W + 16;
    private static final int CELL_H = THUMB_H + 70;
    private static final int COLUMNS = 4;
    private static final int SCROLLBAR_W = 16;
    // Empirical buffer: FScrollPane's themed border eats ~4px; without this, 4*CELL_W+SCROLLBAR_W rounds down to 3 cols.
    private static final int VIEWPORT_BUFFER = 12;
    private static final int CONTAINER_W = COLUMNS * CELL_W + SCROLLBAR_W + VIEWPORT_BUFFER;
    private static final int CONTAINER_H = 736;          // 700 grid + 28 search bar + 8 gap
    private static final int SEARCH_DEBOUNCE_MS = 200;

    private ChangeArtDialog() {}

    public static void show(final PaperCard current) {
        FThreads.assertExecutedByEdt(true);

        final List<PaperCard> printings = StaticData.instance().getCommonCards().getAllCardsNoAlt(current.getName());
        if (printings.size() <= 1) {
            return;
        }
        printings.sort(printingComparator());

        // Match mobile: pull the current printing to the top so it's the first thing the user sees.
        final PaperCard currentNonFoil = current.isFoil() ? current.getUnFoiled() : current;
        if (printings.remove(currentNonFoil)) {
            printings.add(0, currentNonFoil);
        }

        final DefaultListModel<PaperCard> model = new DefaultListModel<>();
        for (PaperCard pc : printings) {
            model.addElement(pc);
        }

        final FList<PaperCard> list = new FList<>(model) {
            private static final long serialVersionUID = 1L;

            @Override
            public Dimension getPreferredScrollableViewportSize() {
                return new Dimension(COLUMNS * CELL_W, 2 * CELL_H);
            }

            // Default JList getPreferredSize over-estimates when computed pre-layout (assumes 1 col); override to bound the scrollbar.
            @Override
            public Dimension getPreferredSize() {
                final int rows = Math.max(1, (getModel().getSize() + COLUMNS - 1) / COLUMNS);
                return new Dimension(COLUMNS * CELL_W, rows * CELL_H);
            }
        };
        list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        list.setVisibleRowCount(0);
        list.setFixedCellWidth(CELL_W);
        list.setFixedCellHeight(CELL_H);
        list.setCellRenderer(new GridCellRenderer());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        final int currentIndex = model.indexOf(currentNonFoil);
        if (currentIndex >= 0) {
            list.setSelectedIndex(currentIndex);
            list.ensureIndexIsVisible(currentIndex);
        }

        final FScrollPane scroll = new FScrollPane(list, true,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setWheelScrollingEnabled(true);
        list.addMouseWheelListener(e ->
                scroll.dispatchEvent(SwingUtilities.convertMouseEvent(list, e, scroll)));

        final Localizer localizer = Localizer.getInstance();

        final FTextField txtSearch = new FTextField.Builder()
                .ghostText(localizer.getMessage("lblChangeArtSearchHint"))
                .build();
        final FComboBox<ArtStyle> cbStyle = new FComboBox<>(ArtStyle.values());

        final CardLayout centerLayout = new CardLayout();
        final JPanel centerPanel = new JPanel(centerLayout);
        centerPanel.setOpaque(false);
        final FLabel emptyLabel = new FLabel.Builder()
                .text(localizer.getMessage("lblChangeArtNoResults"))
                .fontSize(14)
                .build();
        emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        centerPanel.add(scroll, "list");
        centerPanel.add(emptyLabel, "empty");

        final Runnable applyFilter = () -> {
            applyFilter(printings, model, list, txtSearch.getText(), (ArtStyle) cbStyle.getSelectedItem());
            centerLayout.show(centerPanel, model.isEmpty() ? "empty" : "list");
        };
        final Timer searchTimer = new Timer(SEARCH_DEBOUNCE_MS, e -> applyFilter.run());
        searchTimer.setRepeats(false);
        txtSearch.addChangeListener(new FTextField.ChangeListener() {
            @Override public void textChanged() { searchTimer.restart(); }
        });
        cbStyle.addActionListener(e -> applyFilter.run());

        final JPanel topBar = new JPanel(new BorderLayout(8, 0));
        topBar.setOpaque(false);
        topBar.add(txtSearch, BorderLayout.CENTER);
        topBar.add(cbStyle, BorderLayout.EAST);

        final Dimension fixedSize = new Dimension(CONTAINER_W, CONTAINER_H);
        final JPanel container = new JPanel(new BorderLayout(0, 8)) {
            private static final long serialVersionUID = 1L;
            @Override public Dimension getPreferredSize() { return fixedSize; }
            @Override public Dimension getMinimumSize()   { return fixedSize; }
            @Override public Dimension getMaximumSize()   { return fixedSize; }
        };
        container.setOpaque(false);
        container.add(topBar, BorderLayout.NORTH);
        container.add(centerPanel, BorderLayout.CENTER);

        // Route through getMessage(key, args) so Localizer's UTF-8 round-trip handles the em-dash.
        final String title = localizer.getMessage("lblChangeArtDialogTitle", current.getName());

        final FOptionPane optionPane = new FOptionPane(null, title, null, container,
                ImmutableList.of(localizer.getMessage("lblOK"), localizer.getMessage("lblCancel")), 0);

        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && list.getSelectedValue() != null) {
                    optionPane.setResult(0);
                }
            }
        });

        optionPane.setVisible(true);
        final int result = optionPane.getResult();
        optionPane.dispose();
        GridCellRenderer.clearCache();

        if (result != 0) {
            return;
        }

        final PaperCard chosen = list.getSelectedValue();
        if (chosen == null) {
            return;
        }
        performSwap(current, chosen);
    }

    private static Comparator<PaperCard> printingComparator() {
        return Comparator.comparing((PaperCard pc) -> {
                    CardEdition ed = StaticData.instance().getEditions().get(pc.getEdition());
                    return ed != null ? ed.getDate() : null;
                }, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(PaperCard::getCollectorNumber, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private static void applyFilter(List<PaperCard> printings, DefaultListModel<PaperCard> model,
            JList<PaperCard> list, String rawQuery, ArtStyle style) {
        final String query = rawQuery.toLowerCase().trim();
        final PaperCard previous = list.getSelectedValue();
        model.clear();
        for (PaperCard pc : printings) {
            if (!query.isEmpty() && !matchesSet(pc, query)) {
                continue;
            }
            if (style != null && !style.matches(pc)) {
                continue;
            }
            model.addElement(pc);
        }
        if (previous != null) {
            final int idx = model.indexOf(previous);
            if (idx >= 0) {
                list.setSelectedIndex(idx);
            }
        }
    }

    private static boolean matchesSet(PaperCard pc, String lowerQuery) {
        final String editionCode = pc.getEdition();
        if (editionCode != null && editionCode.toLowerCase().contains(lowerQuery)) {
            return true;
        }
        final CardEdition edition = StaticData.instance().getEditions().get(editionCode);
        return edition != null && edition.getName().toLowerCase().contains(lowerQuery);
    }

    /** Edition-section categories the user can filter by. {@code section} = null means no filter (All). */
    private enum ArtStyle {
        ALL("lblArtStyleAll", null),
        STANDARD("lblArtStyleStandard", CardEdition.EditionSectionWithCollectorNumbers.CARDS.getName()),
        BORDERLESS("lblArtStyleBorderless", CardEdition.EditionSectionWithCollectorNumbers.BORDERLESS.getName()),
        FULL_ART("lblArtStyleFullArt", CardEdition.EditionSectionWithCollectorNumbers.FULL_ART.getName()),
        SHOWCASE("lblArtStyleShowcase", CardEdition.EditionSectionWithCollectorNumbers.SHOWCASE.getName()),
        EXTENDED_ART("lblArtStyleExtendedArt", CardEdition.EditionSectionWithCollectorNumbers.EXTENDED_ART.getName()),
        RETRO_FRAME("lblArtStyleRetroFrame", CardEdition.EditionSectionWithCollectorNumbers.RETRO_FRAME.getName()),
        PROMO("lblArtStylePromo", CardEdition.EditionSectionWithCollectorNumbers.PROMO.getName());

        private final String labelKey;
        private final String section;

        ArtStyle(String labelKey, String section) {
            this.labelKey = labelKey;
            this.section = section;
        }

        boolean matches(PaperCard pc) {
            if (section == null) return true;
            final CardEdition edition = StaticData.instance().getEditions().get(pc.getEdition());
            return edition != null && section.equalsIgnoreCase(edition.getSectionForCollectorNumber(pc.getCollectorNumber()));
        }

        @Override
        public String toString() {
            return Localizer.getInstance().getMessage(labelKey);
        }
    }

    private static void performSwap(PaperCard oldCard, PaperCard chosenNonFoil) {
        final PaperCard newCard = oldCard.isFoil() ? chosenNonFoil.getFoiled() : chosenNonFoil;
        if (newCard.equals(oldCard)) {
            return;
        }
        final CardManager deckManager = (CardManager) CDeckEditorUI.SINGLETON_INSTANCE
                .getCurrentEditorController().getDeckManager();
        deckManager.removeItem(oldCard, 1);
        deckManager.addItem(newCard, 1);
        CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController()
                .getDeckController().notifyModelChanged();
    }

    /**
     * Paints a high-resolution source image at smaller display size using BICUBIC against the destination Graphics2D.
     * This routes through the screen's HiDPI transform, so on hi-DPI displays the BICUBIC pass produces a sharp result
     * — pre-rendering into a user-space BufferedImage would let Swing add a bilinear upscale and blur the output.
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

    /** JPanel-based renderer: card image on top, text underneath. Used in HORIZONTAL_WRAP JList. */
    private static final class GridCellRenderer extends JPanel implements ListCellRenderer<PaperCard> {
        private static final long serialVersionUID = 1L;
        private static final Map<String, Icon> ICON_CACHE = new HashMap<>();

        static void clearCache() { ICON_CACHE.clear(); }

        private final JLabel imageLabel = new JLabel();
        private final JLabel textLabel = new JLabel();

        GridCellRenderer() {
            super(new BorderLayout(0, 4));
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
            textLabel.setHorizontalAlignment(SwingConstants.CENTER);
            textLabel.setVerticalAlignment(SwingConstants.TOP);
            textLabel.setPreferredSize(new Dimension(THUMB_W, 60));
            add(imageLabel, BorderLayout.CENTER);
            add(textLabel, BorderLayout.SOUTH);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends PaperCard> list,
                PaperCard value, int index, boolean isSelected, boolean cellHasFocus) {
            final String editionCode = value.getEdition();
            final CardEdition edition = StaticData.instance().getEditions().get(editionCode);
            final String editionName = edition != null ? edition.getName() : editionCode;

            textLabel.setText(String.format(
                    "<html><center>%s<br><i>(%s)</i> #%s</center></html>",
                    editionName, editionCode, value.getCollectorNumber()));

            imageLabel.setIcon(getOrComputeIcon(value, list));

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                textLabel.setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                textLabel.setForeground(list.getForeground());
            }
            return this;
        }

        private static Icon getOrComputeIcon(PaperCard card, JList<? extends PaperCard> list) {
            final String imageKey = card.getImageKey(false);
            final String cacheKey = imageKey + "#" + THUMB_W + "x" + THUMB_H;

            Icon cached = ICON_CACHE.get(cacheKey);
            if (cached != null) {
                return cached;
            }

            final Pair<BufferedImage, Boolean> info = ImageCache.getCardOriginalImageInfo(imageKey, true);
            final BufferedImage src = info.getLeft();
            final boolean isPlaceholder = Boolean.TRUE.equals(info.getRight());
            if (src == null) {
                return null;
            }

            final Icon icon = new HighQualityScaledIcon(src, THUMB_W, THUMB_H);
            ICON_CACHE.put(cacheKey, icon);

            if (isPlaceholder) {
                final ImageFetcher fetcher = GuiBase.getInterface().getImageFetcher();
                if (fetcher != null) {
                    fetcher.fetchImage(imageKey, () -> {
                        ICON_CACHE.remove(cacheKey);
                        SwingUtilities.invokeLater(list::repaint);
                    });
                }
            }

            return icon;
        }
    }
}
