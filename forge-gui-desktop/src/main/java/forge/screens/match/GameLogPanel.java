package forge.screens.match;

import java.awt.AWTEvent;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

import javax.swing.JComponent;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.LayerUI;

import forge.CachedCardImage;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.gui.MouseUtil;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinFont;
import forge.toolbox.FSkin.SkinnedTextArea;
import net.miginfocom.swing.MigLayout;

@SuppressWarnings("serial")
public class GameLogPanel extends JPanel {
    private static final String CARD_VIEW_KEY = "GameLogPanel.cardView";

    private FScrollPane scrollPane;
    private MyScrollablePanel scrollablePanel;
    private SkinFont textFont = FSkin.getFont();

    private final LayerUI<FScrollPane> layerUI = new GameLogPanelLayerUI();
    private JLayer<FScrollPane> layer;
    private boolean isScrollBarVisible = false;
    private Consumer<CardView> onCardHover;

    public GameLogPanel() {
        setMyLayout();
        createScrollablePanel();
        addNewScrollPane();
        setResizeListener();
    }

    public void setOnCardHover(final Consumer<CardView> callback) {
        this.onCardHover = callback;
    }

    public void reset() {
        scrollablePanel.removeAll();
        scrollablePanel.validate();
    }

    protected void setVerticalScrollbarVisibility() {
        if (isScrollBarVisible) {
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        } else {
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        }
        forceVerticalScrollbarToMax();
    }

    private void setMyLayout() {
        setLayout(new MigLayout("insets 0"));
        setOpaque(false);
    }

    private void setResizeListener() {
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(final ComponentEvent arg0) {
                forceVerticalScrollbarToMax();
            }
        });
    }

    /**
     * Creates a transparent scroll pane that handles the scrolling
     * characteristics for the list of {@code JTextArea} log entries.
     */
    private void addNewScrollPane() {
        scrollPane = new FScrollPane(false);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getViewport().add(scrollablePanel);
        layer = new JLayer<>(scrollPane, layerUI);
        this.add(layer, "w 10:100%, h 100%");
    }

    /**
     * Creates a {@code Scrollable JPanel} that works better with
     * {@code FScrollPane} than the standard {@code JPanel}.
     * <p>
     * This manages the layout and display of the list of {@code JTextArea} log entries.
     * <p>
     * <b>MigLayout Settings</b><ul>
     * <li>{@code insets 0} = No margins
     * <li>{@code gap 0} = no gap between cells
     * <li>{@code flowy} = vertical flow mode - add each new cell below the previous cell.
     */
    private void createScrollablePanel() {
        scrollablePanel = new MyScrollablePanel();
        scrollablePanel.setLayout(new MigLayout("insets 0, gap 0, flowy"));
    }

    /**
     * Forces scrollbar to bottom of scroller viewport.
     * <p>
     * This must be run asynchronously using Runnable()
     * otherwise scrollbar will not always set to maximum.
     */
    private void forceVerticalScrollbarToMax() {
        scrollPane.validate();
        SwingUtilities.invokeLater(() -> {
            final JScrollBar scrollbar = scrollPane.getVerticalScrollBar();
            scrollbar.setValue(scrollbar.getMaximum());
            // This is needed to ensure scrollbar is set to max correctly.
            scrollPane.validate();
            scrollbar.setValue(scrollbar.getMaximum());
        });
    }

    public void addLogEntry(final String text) {
        addLogEntry(text, null, null);
    }

    public void addLogEntry(final String text, final CardView card, final Iterable<PlayerView> viewers) {
        final boolean useAlternateBackColor = (scrollablePanel.getComponents().length % 2 == 0);
        final boolean showCardImages = FModel.getPreferences().getPrefBoolean(FPref.UI_LOG_SHOW_CARD_IMAGES);

        final JTextArea tar;
        if (card != null && showCardImages && viewers != null) {
            tar = new LogEntryTextArea(text, useAlternateBackColor, card, viewers);
        } else {
            tar = createNewLogEntryJTextArea(text, useAlternateBackColor);
        }

        if (card != null) {
            ((JComponent) tar).putClientProperty(CARD_VIEW_KEY, card);
        }

        // If the minimum is not specified then the JTextArea will
        // not be sized correctly using MigLayout.
        // (http://stackoverflow.com/questions/6023145/line-wrap-in-a-jtextarea-causes-jscrollpane-to-missbehave-with-miglayout)
        final String importantWidthConstraint = "w 10:100%";
        scrollablePanel.add(tar, importantWidthConstraint);

        // Automatically hide scrollbar (if visible).
        if (isScrollBarVisible) {
            isScrollBarVisible = false;
            setVerticalScrollbarVisibility();
        }

        forceVerticalScrollbarToMax();
    }

    public void setTextFont(final SkinFont newFont) {
        this.textFont = newFont;
    }

    private JTextArea createNewLogEntryJTextArea(final String text, final boolean useAlternateBackColor) {
        final SkinnedTextArea tar = new SkinnedTextArea(text);
        tar.setFont(textFont);
        tar.setBorder(new EmptyBorder(3, 4, 3, 4));
        tar.setFocusable(false);
        tar.setEditable(false);
        tar.setLineWrap(true);
        tar.setWrapStyleWord(true);
        tar.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));

        FSkin.SkinColor skinColor = FSkin.getColor(FSkin.Colors.CLR_ZEBRA);
        if (useAlternateBackColor) { skinColor = skinColor.darker(); }
        tar.setOpaque(true);
        tar.setBackground(skinColor);

        return tar;
    }

    /** A log entry with an inline miniature card image, following VStack's StackInstanceTextArea pattern. */
    private final class LogEntryTextArea extends SkinnedTextArea {
        private static final int PADDING = 3;
        private static final int CARD_WIDTH = 50;
        private static final int CARD_HEIGHT = 70;

        private final CachedCardImage cachedImage;

        LogEntryTextArea(final String text, final boolean useAlternateBackColor,
                         final CardView card, final Iterable<PlayerView> viewers) {
            super(text);
            setFont(textFont);
            setBorder(new EmptyBorder(PADDING, CARD_WIDTH + 2 * PADDING, PADDING, PADDING));
            setFocusable(false);
            setEditable(false);
            setLineWrap(true);
            setWrapStyleWord(true);
            setMinimumSize(new Dimension(CARD_WIDTH + 2 * PADDING, CARD_HEIGHT + 2 * PADDING));
            setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));

            FSkin.SkinColor skinColor = FSkin.getColor(FSkin.Colors.CLR_ZEBRA);
            if (useAlternateBackColor) { skinColor = skinColor.darker(); }
            setOpaque(true);
            setBackground(skinColor);

            this.cachedImage = new CachedCardImage(card, viewers, CARD_WIDTH, CARD_HEIGHT) {
                @Override
                public void onImageFetched() {
                    repaint();
                }
            };
        }

        @Override
        public void paintComponent(final Graphics g) {
            super.paintComponent(g);
            final BufferedImage img = cachedImage.getImage();
            if (img != null) {
                ((Graphics2D) g).drawImage(img, null, PADDING, PADDING);
            }
        }
    }

    protected final class MyScrollablePanel extends JPanel implements Scrollable {

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(final Rectangle visibleRect, final int orientation, final int direction) {
            return textFont.getSize();
        }

        @Override
        public int getScrollableBlockIncrement(final Rectangle visibleRect, final int orientation, final int direction) {
            return textFont.getSize();
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        // we don't want to track the height, because we want to scroll vertically.
        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    protected final class GameLogPanelLayerUI extends LayerUI<FScrollPane> {

        @SuppressWarnings("unchecked")
        @Override
        public void installUI(final JComponent c) {
            super.installUI(c);
            final JLayer<FScrollPane> l = (JLayer<FScrollPane>)c;
            l.setLayerEventMask(AWTEvent.MOUSE_EVENT_MASK);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void uninstallUI(final JComponent c) {
            super.uninstallUI(c);
            final JLayer<FScrollPane> l = (JLayer<FScrollPane>)c;
            l.setLayerEventMask(0);
        }

        @Override
        protected void processMouseEvent(final MouseEvent e, final JLayer<? extends FScrollPane> l) {

            final boolean isScrollBarRequired = scrollPane.getVerticalScrollBar().getMaximum() > getHeight();
            final boolean isHoveringOverLogEntry = e.getSource() instanceof JTextArea;

            switch (e.getID()) {
            case MouseEvent.MOUSE_ENTERED:
                if (isHoveringOverLogEntry) {
                    if (isScrollBarRequired) {
                        MouseUtil.setCursor(Cursor.HAND_CURSOR);
                    }
                    // Trigger card hover callback
                    if (onCardHover != null) {
                        final Object cardProp = ((JComponent) e.getSource()).getClientProperty(CARD_VIEW_KEY);
                        if (cardProp instanceof CardView) {
                            onCardHover.accept((CardView) cardProp);
                        }
                    }
                }
                break;
            case MouseEvent.MOUSE_EXITED:
                MouseUtil.resetCursor();
                break;
            case MouseEvent.MOUSE_RELEASED:
                if (e.getButton() == 1 && isHoveringOverLogEntry) {
                    isScrollBarVisible = isScrollBarRequired && !isScrollBarVisible;
                    setVerticalScrollbarVisibility();
                }
                break;
            }
        }
    }
}
