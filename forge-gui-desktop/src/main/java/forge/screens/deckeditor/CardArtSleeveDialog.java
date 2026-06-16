package forge.screens.deckeditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.Timer;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableList;

import net.miginfocom.swing.MigLayout;

import forge.ImageCache;
import forge.StaticData;
import forge.gui.FThreads;
import forge.item.PaperCard;
import forge.toolbox.FList;
import forge.toolbox.FOptionPane;
import forge.toolbox.FScrollPane;
import forge.toolbox.FTextField;
import forge.toolbox.special.CardImageGrid;
import forge.util.Localizer;
import forge.util.SleeveArt;

/**
 * Master-detail picker for a card-art deck sleeve: a searchable card-name list on the left and the
 * selected card's printings on the right. Returns the chosen printing, or null if cancelled.
 *
 * <p>The right pane reuses {@link CardImageGrid}; the left list is filtered (debounced) over the
 * unique-by-name card pool. Selecting a name pre-selects that card's first printing so OK always
 * has a concrete result.
 */
public final class CardArtSleeveDialog {

    private static final int THUMB_W = 200;
    private static final int THUMB_H = 280;
    private static final int COLUMNS = 3;
    private static final int SCROLLBAR_W = 16;
    private static final int VIEWPORT_BUFFER = 12;
    private static final int LIST_W = 260;
    private static final int PREVIEW_W = 260;
    private static final int CONTAINER_H = 700;
    private static final int SEARCH_DEBOUNCE_MS = 250;

    private CardArtSleeveDialog() {}

    /** The chosen printing together with the crop offset framed in the preview. */
    public static final class Result {
        public final PaperCard card;
        public final int offset;
        Result(final PaperCard card, final int offset) {
            this.card = card;
            this.offset = offset;
        }
    }

    public static Result show() {
        FThreads.assertExecutedByEdt(true);
        final Localizer localizer = Localizer.getInstance();

        final List<PaperCard> allUnique = StaticData.instance().getCommonCards().getUniqueCards().stream()
                .sorted(Comparator.comparing(PaperCard::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());

        final CardImageGrid grid = new CardImageGrid(COLUMNS, THUMB_W, THUMB_H);
        final SleevePreviewPanel preview = new SleevePreviewPanel();
        grid.addSelectionListener(preview::setCard);

        final DefaultListModel<PaperCard> listModel = new DefaultListModel<>();
        listModel.addAll(allUnique);
        final FList<PaperCard> list = new FList<>(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            final PaperCard sel = list.getSelectedValue();
            if (sel == null) {
                grid.setItems(new ArrayList<>());
                return;
            }
            final List<PaperCard> prints = StaticData.instance().getCommonCards().getAllCardsNoAlt(sel.getName());
            grid.setItems(prints);
            if (!prints.isEmpty()) {
                grid.setSelected(prints.get(0));
            }
        });

        // start with a card selected so the grid/preview aren't an empty black void on open
        if (!listModel.isEmpty()) {
            list.setSelectedIndex(0);
        }

        final FTextField txtSearch = new FTextField.Builder()
                .ghostText(localizer.getMessage("lblSearch")).build();
        final Runnable applyFilter = () -> {
            final String query = StringUtils.stripAccents(txtSearch.getText()).toLowerCase().trim();
            listModel.clear();
            if (query.isEmpty()) {
                listModel.addAll(allUnique);
            } else {
                listModel.addAll(allUnique.stream()
                        .filter(pc -> StringUtils.stripAccents(pc.getName()).toLowerCase().contains(query))
                        .collect(Collectors.toList()));
            }
        };
        final Timer searchTimer = new Timer(SEARCH_DEBOUNCE_MS, e -> applyFilter.run());
        searchTimer.setRepeats(false);
        txtSearch.addChangeListener(new FTextField.ChangeListener() {
            @Override public void textChanged() { searchTimer.restart(); }
        });

        final JPanel leftPanel = new JPanel(new BorderLayout(0, 6)) {
            private static final long serialVersionUID = 1L;
            @Override public Dimension getPreferredSize() { return new Dimension(LIST_W, CONTAINER_H); }
        };
        leftPanel.setOpaque(false);
        leftPanel.add(txtSearch, BorderLayout.NORTH);
        final FScrollPane listScroll = new FScrollPane(list, true);
        listScroll.setOpaque(true);
        listScroll.setBackground(list.getBackground());
        leftPanel.add(listScroll, BorderLayout.CENTER);

        final JLabel previewTitle = new JLabel(localizer.getMessage("lblSleevePreview"), SwingConstants.CENTER);
        previewTitle.setForeground(Color.WHITE);
        previewTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        preview.setAlignmentX(Component.CENTER_ALIGNMENT);
        // centre the [title + card] group vertically, with the title just above the card
        final JPanel previewPanel = new JPanel();
        previewPanel.setOpaque(false);
        previewPanel.setLayout(new BoxLayout(previewPanel, BoxLayout.Y_AXIS));
        final JLabel dragHint = new JLabel(localizer.getMessage("lblDragToChangeCrop"), SwingConstants.CENTER);
        dragHint.setForeground(Color.LIGHT_GRAY);
        dragHint.setFont(dragHint.getFont().deriveFont(Font.ITALIC));
        dragHint.setAlignmentX(Component.CENTER_ALIGNMENT);
        previewPanel.add(Box.createVerticalGlue());
        previewPanel.add(previewTitle);
        previewPanel.add(Box.createVerticalStrut(8));
        previewPanel.add(preview);
        previewPanel.add(Box.createVerticalStrut(6));
        previewPanel.add(dragHint);
        previewPanel.add(Box.createVerticalGlue());

        final int gridW = COLUMNS * grid.getCellWidth() + SCROLLBAR_W + VIEWPORT_BUFFER;
        final Dimension fixedSize = new Dimension(LIST_W + gridW + PREVIEW_W + 48, CONTAINER_H);
        final JPanel container = new JPanel(new MigLayout("insets 0, gap 16, fill",
                "[" + LIST_W + "!][grow][" + PREVIEW_W + "!]", "[grow]")) {
            private static final long serialVersionUID = 1L;
            @Override public Dimension getPreferredSize() { return fixedSize; }
            @Override public Dimension getMinimumSize()   { return fixedSize; }
        };
        container.setOpaque(false);
        container.add(leftPanel, "grow");
        container.add(grid.getComponent(), "grow");
        container.add(previewPanel, "grow");

        final FOptionPane optionPane = new FOptionPane(null, localizer.getMessage("lblSelectCardForSleeve"), null,
                container, ImmutableList.of(localizer.getMessage("lblOK"), localizer.getMessage("lblCancel")), 0);
        grid.addDoubleClickListener(pc -> optionPane.setResult(0));

        optionPane.setDefaultFocus(txtSearch);
        optionPane.setVisible(true);
        final int result = optionPane.getResult();
        final PaperCard selected = grid.getSelected();
        final int offset = preview.getOffset();
        optionPane.dispose();
        grid.dispose();

        if (result != 0 || selected == null) {
            return null;
        }
        return new Result(selected, offset);
    }

    /**
     * Paints the highlighted printing as it will appear on the sleeve — cover-cropped and framed
     * identically to {@link ImageCache#getSleeveArtCropped} — and lets the user drag along the slack
     * axis to reposition the crop. Cover-crops the full-resolution art live on paint (offset 0 =
     * left/top edge, 1000 = right/bottom, 500 = centre); no cropped image is built per drag.
     */
    private static final class SleevePreviewPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        // mirror the dark frame baked into the rendered sleeve (ImageCache.cropToCardAspect) at the
        // same fraction so the preview frames and crops identically to the real sleeve
        private static final Color SLEEVE_ART_BORDER = new Color(38, 37, 38);
        private static final double SLEEVE_ART_BORDER_FRACTION = 0.04;
        private final double aspect = ImageCache.sleeveAspect();
        private String key;
        private BufferedImage art; // full, uncropped art-crop
        private int offset = 500;
        private boolean horizontalSlack = true; // which axis the art overflows the sleeve window
        private int dragStartPx;
        private int dragStartOffset;

        SleevePreviewPanel() {
            setOpaque(false);
            final int w = PREVIEW_W - 56;
            final int h = (int) Math.round(w / aspect);
            final Dimension d = new Dimension(w, h);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);

            final MouseAdapter dragger = new MouseAdapter() {
                @Override public void mousePressed(final MouseEvent e) {
                    dragStartPx = horizontalSlack ? e.getX() : e.getY();
                    dragStartOffset = offset;
                }
                @Override public void mouseDragged(final MouseEvent e) {
                    if (art == null) {
                        return;
                    }
                    final int travel = horizontalSlack ? getWidth() : getHeight();
                    if (travel <= 0) {
                        return;
                    }
                    // grab semantics: dragging the art with the cursor moves the crop window the other way
                    final int delta = (horizontalSlack ? e.getX() : e.getY()) - dragStartPx;
                    offset = SleeveArt.clampOffset(dragStartOffset - Math.round(delta * 1000f / travel));
                    repaint();
                }
            };
            addMouseListener(dragger);
            addMouseMotionListener(dragger);
        }

        int getOffset() {
            return offset;
        }

        void setCard(final PaperCard pc) {
            final String k = pc == null ? null : pc.getImageKey(false);
            if (k == null ? key == null : k.equals(key)) {
                return;
            }
            key = k;
            offset = 500; // each card starts centred; drag re-frames it
            art = k == null ? null : ImageCache.getSleeveArtFull(k);
            if (k != null && art == null) {
                ImageCache.fetchSleeveArt(k, () -> {
                    if (k.equals(key)) {
                        art = ImageCache.getSleeveArtFull(k);
                        updateSlackAxis();
                        repaint();
                    }
                });
            }
            updateSlackAxis();
            repaint();
        }

        // The cover-crop only overflows one axis; the user drags along that one
        private void updateSlackAxis() {
            horizontalSlack = art == null || (double) art.getWidth() / art.getHeight() > aspect;
            setCursor(Cursor.getPredefinedCursor(art == null ? Cursor.DEFAULT_CURSOR : Cursor.MOVE_CURSOR));
        }

        @Override
        protected void paintComponent(final Graphics g) {
            super.paintComponent(g);
            final int w = getWidth();
            final int h = getHeight();
            final Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(SLEEVE_ART_BORDER);
            g2.fillRect(0, 0, w, h);
            if (art != null) {
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                final double scale = Math.max((double) w / art.getWidth(), (double) h / art.getHeight());
                final int scaledW = (int) Math.round(art.getWidth() * scale);
                final int scaledH = (int) Math.round(art.getHeight() * scale);
                final double f = offset / 1000.0;
                final int drawX = -(int) Math.round((scaledW - w) * f);
                final int drawY = -(int) Math.round((scaledH - h) * f);
                g2.drawImage(art, drawX, drawY, scaledW, scaledH, null);
                final int bw = Math.max(1, (int) Math.round(Math.min(w, h) * SLEEVE_ART_BORDER_FRACTION));
                g2.setColor(SLEEVE_ART_BORDER);
                g2.fillRect(0, 0, w, bw);
                g2.fillRect(0, h - bw, w, bw);
                g2.fillRect(0, 0, bw, h);
                g2.fillRect(w - bw, 0, bw, h);
            }
            g2.dispose();
        }
    }
}
