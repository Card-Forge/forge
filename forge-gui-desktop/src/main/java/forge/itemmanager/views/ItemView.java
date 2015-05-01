package forge.itemmanager.views;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JViewport;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;

import forge.item.InventoryItem;
import forge.itemmanager.ColumnDef;
import forge.itemmanager.ItemManager;
import forge.itemmanager.ItemManagerConfig;
import forge.itemmanager.ItemManagerModel;
import forge.toolbox.FLabel;
import forge.toolbox.FScrollPane;
import forge.toolbox.FScrollPanel;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinColor;
import forge.toolbox.FSkin.SkinImage;
import forge.toolbox.ToolTipListener;

public abstract class ItemView<T extends InventoryItem> {
    private static final SkinColor BORDER_COLOR = FSkin.getColor(FSkin.Colors.CLR_TEXT);

    protected final ItemManager<T> itemManager;
    protected final ItemManagerModel<T> model;
    private final FScrollPane scroller;
    private final FLabel button;
    private final FScrollPanel pnlOptions = new FScrollPanel(
            new MigLayout("insets 3 1 0 1, gap 3 4, hidemode 3"), true,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

    private int heightBackup;
    private boolean isIncrementalSearchActive = false;

    @SuppressWarnings("serial")
    protected ItemView(final ItemManager<T> itemManager0, final ItemManagerModel<T> model0) {
        this.itemManager = itemManager0;
        this.model = model0;
        this.scroller = new FScrollPane(false) {
            @Override
            protected void processMouseWheelEvent(final MouseWheelEvent e) {
                if (e.isControlDown()) {
                    onMouseWheelZoom(e);
                    return;
                }
                super.processMouseWheelEvent(e);
            }
        };
        this.pnlOptions.setOpaque(false);
        this.pnlOptions.setBorder(new FSkin.MatteSkinBorder(1, 0, 0, 0, BORDER_COLOR));
        this.scroller.setBorder(new FSkin.LineSkinBorder(BORDER_COLOR));
        this.button = new FLabel.Builder()
            .hoverable()
            .selectable(true)
            .icon(getIcon())
            .iconScaleAuto(false)
            .tooltip(getCaption())
            .build();
    }

    public void initialize(final int index) {
        final JComponent comp = this.getComponent();

        //hook incremental search functionality
        final IncrementalSearch incrementalSearch  = new IncrementalSearch();
        comp.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(final FocusEvent arg0) {
                incrementalSearch.cancel();
            }
        });
        comp.addKeyListener(incrementalSearch);

        this.button.setCommand(new Runnable() {
            @Override public void run() {
                if (button.isSelected()) {
                    itemManager.setViewIndex(index);
                }
                else {
                    button.setSelected(true); //prevent toggling off button
                }
            }
        });

        this.scroller.setViewportView(comp);
        this.scroller.getVerticalScrollBar().addAdjustmentListener(new ToolTipListener());
        this.scroller.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(final ComponentEvent e) {
                onResize();
                //scroll selection into view whenever view height changes
                final int height = e.getComponent().getHeight();
                if (height != heightBackup) {
                    heightBackup = height;
                    scrollSelectionIntoView();
                }
            }
        });
    }

    public FLabel getButton() {
        return this.button;
    }

    public FScrollPane getScroller() {
        return this.scroller;
    }

    public FScrollPanel getPnlOptions() {
        return pnlOptions;
    }

    public int getScrollValue() {
        return scroller.getVerticalScrollBar().getValue();
    }

    public void setScrollValue(final int value) {
        scroller.getVerticalScrollBar().setValue(value);
    }

    protected void onMouseWheelZoom(final MouseWheelEvent e) {
    }

    public boolean isIncrementalSearchActive() {
        return this.isIncrementalSearchActive;
    }

    public void refresh(final Iterable<T> itemsToSelect, final int backupIndexToSelect, final int scrollValueToRestore) {
        this.model.refreshSort();
        onRefresh();
        fixSelection(itemsToSelect, backupIndexToSelect, scrollValueToRestore);
    }
    protected abstract void onResize();
    protected abstract void onRefresh();
    protected void fixSelection(final Iterable<T> itemsToSelect, final int backupIndexToSelect, final int scrollValueToRestore) {
        if (itemsToSelect == null) {
            setSelectedIndex(0, false); //select first item if no items to select
            setScrollValue(0); //ensure scrolled to top
        }
        else {
            if (!setSelectedItems(itemsToSelect)) {
                setSelectedIndex(backupIndexToSelect);
            }
        }
    }

    public final T getSelectedItem() {
        return getItemAtIndex(getSelectedIndex());
    }

    public final Collection<T> getSelectedItems() {
        final List<T> items = new ArrayList<T>();
        for (final Integer i : getSelectedIndices()) {
            final T item = getItemAtIndex(i);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    public final boolean setSelectedItem(final T item) {
        return setSelectedItem(item, true);
    }
    public final boolean setSelectedItem(final T item, final boolean scrollIntoView) {
        final int index = getIndexOfItem(item);
        if (index != -1) {
            setSelectedIndex(index, scrollIntoView);
            return true;
        }
        return false;
    }

    public final boolean setSelectedItems(final Iterable<T> items) {
        return setSelectedItems(items, true);
    }
    public final boolean setSelectedItems(final Iterable<T> items, final boolean scrollIntoView) {
        final List<Integer> indices = new ArrayList<Integer>();
        for (final T item : items) {
            final int index = getIndexOfItem(item);
            if (index != -1) {
                indices.add(index);
            }
        }
        if (indices.size() > 0) {
            onSetSelectedIndices(indices);
            if (scrollIntoView) {
                scrollSelectionIntoView();
            }
            return true;
        }
        return false;
    }

    public void setSelectedIndex(final int index) {
        setSelectedIndex(index, true);
    }
    public void setSelectedIndex(int index, final boolean scrollIntoView) {
        final int count = getCount();
        if (count == 0) { return; }

        if (index < 0) {
            index = 0;
        }
        else if (index >= count) {
            index = count - 1;
        }

        onSetSelectedIndex(index);
        if (scrollIntoView) {
            scrollSelectionIntoView();
        }
    }

    public void setSelectedIndices(final Iterable<Integer> indices) {
        setSelectedIndices(indices, true);
    }
    public void setSelectedIndices(final Iterable<Integer> indices, final boolean scrollIntoView) {
        final int count = getCount();
        if (count == 0) { return; }

        final List<Integer> indexList = new ArrayList<Integer>();
        for (final Integer index : indices) {
            if (index >= 0 && index < count) {
                indexList.add(index);
            }
        }

        if (indexList.isEmpty()) { //if no index in range, set selected index based on first index
            for (final Integer index : indices) {
                setSelectedIndex(index);
                return;
            }
            return;
        }

        onSetSelectedIndices(indexList);
        if (scrollIntoView) {
            scrollSelectionIntoView();
        }
    }

    protected void onSelectionChange() {
        final int index = getSelectedIndex();
        if (index != -1) {
            final ListSelectionEvent event = new ListSelectionEvent(itemManager, index, index, false);
            for (final ListSelectionListener listener : itemManager.getSelectionListeners()) {
                listener.valueChanged(event);
            }
        }
    }

    public void scrollSelectionIntoView() {
        final Container parent = getComponent().getParent();
        if (parent instanceof JViewport) {
            onScrollSelectionIntoView((JViewport)parent);
        }
    }

    public void focus() {
        this.getComponent().requestFocusInWindow();
    }

    public boolean hasFocus() {
        return this.getComponent().hasFocus();
    }

    public Point getLocationOnScreen() {
        return this.getComponent().getParent().getLocationOnScreen(); //use parent scroller's location by default
    }

    @Override
    public String toString() {
        return this.getCaption(); //return caption as string for display in combo box
    }

    public abstract JComponent getComponent();
    public abstract void setup(ItemManagerConfig config, Map<ColumnDef, ItemTableColumn> colOverrides);
    public abstract void setAllowMultipleSelections(boolean allowMultipleSelections);
    public abstract T getItemAtIndex(int index);
    public abstract int getIndexOfItem(T item);
    public abstract int getSelectedIndex();
    public abstract Iterable<Integer> getSelectedIndices();
    public abstract void selectAll();
    public abstract int getCount();
    public abstract int getSelectionCount();
    public abstract int getIndexAtPoint(Point p);
    protected abstract SkinImage getIcon();
    protected abstract String getCaption();
    protected abstract void onSetSelectedIndex(int index);
    protected abstract void onSetSelectedIndices(Iterable<Integer> indices);
    protected abstract void onScrollSelectionIntoView(JViewport viewport);

    private class IncrementalSearch extends KeyAdapter {
        private StringBuilder str = new StringBuilder();
        private final FLabel popupLabel = new FLabel.Builder().fontAlign(SwingConstants.LEFT).opaque().build();
        private boolean popupShowing = false;
        private Popup popup;
        private Timer popupTimer;
        private static final int okModifiers = InputEvent.SHIFT_MASK | InputEvent.ALT_GRAPH_MASK;

        public IncrementalSearch() {
        }

        private void setPopupSize() {
            // resize popup to size of label (ensure there's room for the next character so the label
            // doesn't show '...' in the time between when we set the text and when we increase the size
            final Dimension labelDimension = popupLabel.getPreferredSize();
            final Dimension popupDimension = new Dimension(labelDimension.width + 12, labelDimension.height + 4);
            SwingUtilities.getRoot(popupLabel).setSize(popupDimension);
        }

        private void findNextMatch(int startIdx, final boolean reverse) {
            final int numItems = itemManager.getItemCount();
            if (0 == numItems) {
                cancel();
                return;
            }

            // find the next item that matches the string
            startIdx %= numItems;
            final int increment = reverse ? numItems - 1 : 1;
            final int stopIdx = (startIdx + numItems - increment) % numItems;
            final String searchStr = str.toString();
            boolean found = false;
            for (int idx = startIdx;; idx = (idx + increment) % numItems) {
                final T item = ItemView.this.getItemAtIndex(idx);
                if (item == null) {
                    break;
                }

                if (StringUtils.containsIgnoreCase(item.getName(), searchStr)) {
                    ItemView.this.setSelectedIndex(idx);
                    found = true;
                    break;
                }

                if (idx == stopIdx) {
                    break;
                }
            }

            if (searchStr.isEmpty()) {
                cancel();
                return;
            }

            // show a popup with the current search string, highlighted in red if not found
            popupLabel.setText(searchStr + " (hit Enter for next match, Esc to cancel)");
            if (found) {
                popupLabel.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
            }
            else {
                popupLabel.setForeground(new Color(255, 0, 0));
            }

            if (popupShowing) {
                setPopupSize();
                popupTimer.restart();
            }
            else {
                final PopupFactory factory = PopupFactory.getSharedInstance();
                final Point tableLoc = ItemView.this.getLocationOnScreen();
                popup = factory.getPopup(null, popupLabel, tableLoc.x + 10, tableLoc.y + 10);
                FSkin.setTempBackground(SwingUtilities.getRoot(popupLabel), FSkin.getColor(FSkin.Colors.CLR_INACTIVE));

                popupTimer = new Timer(5000, new ActionListener() {
                    @Override public void actionPerformed(final ActionEvent e) {
                        cancel();
                    }
                });
                popupTimer.setRepeats(false);

                popup.show();
                setPopupSize();
                popupTimer.start();
                isIncrementalSearchActive = true;
                popupShowing = true;
            }
        }

        public void cancel() {
            str = new StringBuilder();
            popupShowing = false;
            if (null != popup) {
                popup.hide();
                popup = null;
            }
            if (null != popupTimer) {
                popupTimer.stop();
                popupTimer = null;
            }
            isIncrementalSearchActive = false;
        }

        @Override
        public void keyPressed(final KeyEvent e) {
            if (popupShowing) {
                if (KeyEvent.VK_ESCAPE == e.getKeyCode()) {
                    cancel();
                }
            }
            else {
                for (final KeyListener keyListener : itemManager.getKeyListeners()) {
                    keyListener.keyPressed(e);
                    if (e.isConsumed()) { return; }
                }
                if (KeyEvent.VK_F == e.getKeyCode()) {
                    // let ctrl/cmd-F set focus to the text filter box
                    if (e.isControlDown() || e.isMetaDown()) {
                        itemManager.focusSearch();
                    }
                }
            }
        }

        @Override
        public void keyTyped(final KeyEvent e) {
            if (!popupShowing) {
                for (final KeyListener keyListener : itemManager.getKeyListeners()) {
                    keyListener.keyTyped(e);
                    if (e.isConsumed()) { return; }
                }
            }

            switch (e.getKeyChar()) {
            case KeyEvent.CHAR_UNDEFINED:
                return;

            case KeyEvent.VK_ENTER:
            case 13: // no KeyEvent constant for this, but this comes up on OSX for shift-enter
                if (!str.toString().isEmpty()) {
                    // no need to add (or subtract) 1 -- the table selection will already
                    // have been advanced by the (shift+) enter key
                    findNextMatch(ItemView.this.getSelectedIndex(), e.isShiftDown());
                }
                return;

            case KeyEvent.VK_BACK_SPACE:
                if (!str.toString().isEmpty()) {
                    str.deleteCharAt(str.toString().length() - 1);
                }
                break;

            case KeyEvent.VK_SPACE:
                // don't trigger if the first character is a space
                if (str.toString().isEmpty()) {
                    return;
                }

                //$FALL-THROUGH$
            default:
                // shift and/or alt-graph down is ok.  anything else is a hotkey (e.g. ctrl-f)
                if (okModifiers != (e.getModifiers() | okModifiers)
                || !CharUtils.isAsciiPrintable(e.getKeyChar())) { // escape sneaks in here on Windows
                    return;
                }
                str.append(e.getKeyChar());
            }

            findNextMatch(Math.max(0, ItemView.this.getSelectedIndex()), false);
        }

        @Override
        public void keyReleased(final KeyEvent e) {
            if (!popupShowing) {
                for (final KeyListener keyListener : itemManager.getKeyListeners()) {
                    keyListener.keyReleased(e);
                    if (e.isConsumed()) { return; }
                }
            }
        }
    }
}
