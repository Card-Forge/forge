package forge.gui.match;

import forge.gui.MouseUtil;
import forge.gui.toolbox.FScrollPane;
import forge.gui.toolbox.FSkin;
import forge.gui.toolbox.FSkin.SkinFont;
import forge.gui.toolbox.FSkin.SkinnedTextArea;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.LayerUI;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;

@SuppressWarnings("serial")
public class GameLogPanel extends JPanel {
    private FScrollPane scrollPane;
    private MyScrollablePanel scrollablePanel;
    private SkinFont textFont = FSkin.getFont();

    private LayerUI<FScrollPane> layerUI = new GameLogPanelLayerUI();
    private JLayer<FScrollPane> layer;
    private boolean isScrollBarVisible = false;

    public GameLogPanel() {
        setMyLayout();
        createScrollablePanel();
        addNewScrollPane();
        setResizeListener();
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
            public void componentResized(ComponentEvent arg0) {
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
        layer = new JLayer<FScrollPane>(scrollPane, layerUI);
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
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JScrollBar scrollbar = scrollPane.getVerticalScrollBar();
                scrollbar.setValue(scrollbar.getMaximum());
                // This is needed to ensure scrollbar is set to max correctly.
                scrollPane.validate();
                scrollbar.setValue(scrollbar.getMaximum());
            }
        });
    }

    public void addLogEntry(final String text) {

        final boolean useAlternateBackColor = (scrollablePanel.getComponents().length % 2 == 0);
        final JTextArea tar = createNewLogEntryJTextArea(text, useAlternateBackColor);

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

    public void setTextFont(SkinFont newFont) {
        this.textFont = newFont;
    }

    private JTextArea createNewLogEntryJTextArea(String text, boolean useAlternateBackColor) {
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

    protected final class MyScrollablePanel extends JPanel implements Scrollable {

        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return textFont.getSize();
        }

        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return textFont.getSize();
        }

        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        // we don't want to track the height, because we want to scroll vertically.
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    protected final class GameLogPanelLayerUI extends LayerUI<FScrollPane> {

        @SuppressWarnings("unchecked")
        @Override
        public void installUI(JComponent c) {
            super.installUI(c);
            JLayer<FScrollPane> l = (JLayer<FScrollPane>)c;
            l.setLayerEventMask(AWTEvent.MOUSE_EVENT_MASK);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void uninstallUI(JComponent c) {
            super.uninstallUI(c);
            JLayer<FScrollPane> l = (JLayer<FScrollPane>)c;
            l.setLayerEventMask(0);
        }

        @Override
        protected void processMouseEvent(MouseEvent e, JLayer<? extends FScrollPane> l) {

            boolean isScrollBarRequired = scrollPane.getVerticalScrollBar().getMaximum() > getHeight();
            boolean isHoveringOverLogEntry = e.getSource() instanceof JTextArea;

            switch (e.getID()) {
            case MouseEvent.MOUSE_ENTERED:
                if (isScrollBarRequired && isHoveringOverLogEntry) {
                    MouseUtil.setCursor(Cursor.HAND_CURSOR);
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
