package forge.toolbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Align;

import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.gui.interfaces.IComboBox;
import forge.gui.util.ListHeading;
import forge.menu.FDropDownMenu;
import forge.menu.FMenuItem;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FEvent.FEventType;
import forge.util.Utils;

public class FComboBox<T> extends FTextField implements IComboBox<T> {
    private final List<T> items = new ArrayList<>();
    private T selectedItem;
    private String label = "";
    private final DropDown dropDown = new DropDown();
    private FEventHandler dropDownItemTap, dropDownChangeHandler;
    private boolean autoClose = true;

    public FComboBox() {
        initialize();
    }
    public FComboBox(String label0) {
        initialize();
        setLabel(label0);
    }
    public FComboBox(T[] itemArray) {
        items.addAll(Arrays.asList(itemArray));
        initialize();
    }
    public FComboBox(Iterable<T> items0) {
        for (T item : items0) {
            items.add(item);
        }
        initialize();
    }

    public void setAutoClose(boolean autoClose) {
        this.autoClose = autoClose;
    }

    private void initialize() {
        if (!items.isEmpty()) {
            setSelectedItem(items.get(0)); //select first item by default
        }
    }

    protected String getDisplayText(T item) {
        return item.toString();
    }

    public String getLabel() {
        return label;
    }
    public void setLabel(String label0) {
        if (label0 == null) { label0 = ""; }
        if (label.equals(label0)) { return; }
        label = label0;
        if (selectedItem != null) {
            super.setText(label + getDisplayText(selectedItem));
        }
        else {
            super.setText(label);
        }
    }

    public void setItems(T[] itemArray, T selectedItem0) {
        items.clear();
        if (itemArray != null) {
            items.addAll(Arrays.asList(itemArray));
        }
        setSelectedItem(selectedItem0);
    }
    public void setItems(Iterable<T> items0, T selectedItem0) {
        items.clear();
        if (items0 != null) {
            for (T item : items0) {
                items.add(item);
            }
        }
        setSelectedItem(selectedItem0);
    }

    public void addItem(T item) {
        items.add(item);
        if (items.size() == 1) {
            setSelectedItem(item); //select first item by default
        }
    }

    public boolean removeItem(T item) {
        int restoreIndex = -1; 
        if (selectedItem == item) {
            restoreIndex = getSelectedIndex();
        }
        if (items.remove(item)) {
            if (restoreIndex >= 0) {
                setSelectedIndex(restoreIndex);
            }
        }
        return false;
    }

    public void removeAllItems() {
        items.clear();
        setSelectedItem(null);
    }

    public int getItemCount() {
        return items.size();
    }

    public int getSelectedIndex() {
        if (selectedItem == null) { return -1; }
        return items.indexOf(selectedItem);
    }

    public void setSelectedIndex(int index) {
        if (index < 0) {
            setSelectedItem(null);
            return;
        }

        if (index >= items.size()) {
            index = items.size() - 1;
        }
        setSelectedItem(items.get(index));
    }

    public T getSelectedItem() {
        return selectedItem;
    }

    public void setSelectedItem(T item) {
        if (selectedItem == item || item instanceof ListHeading) { return; }

        if (item != null) {
            if (items.contains(item)) {
                selectedItem = item;
                super.setText(label + getDisplayText(item));
            }
            else { return; }
        }
        else {
            selectedItem = null;
            super.setText(label);
        }

        if (getChangedHandler() != null) {
            getChangedHandler().handleEvent(new FEvent(this, FEventType.CHANGE, item));
        }
    }

    @Override
    public void setText(String text0) {
        for (T item : items) {
            if (getDisplayText(item).equals(text0)) {
                setSelectedItem(item);
                return;
            }
        }
        selectedItem = null;
        super.setText(label + text0);
    }

    @Override
    public boolean tap(float x, float y, int count) {
        dropDown.setVisible(!dropDown.isVisible());
        return true;
    }

    @Override
    public boolean startEdit() {
        return false; //don't allow editing text
    }

    public FEventHandler getDropDownItemTap() {
        return dropDownItemTap;
    }
    public void setDropDownItemTap(FEventHandler itemTap0) {
        dropDownItemTap = itemTap0;
    }

    public FEventHandler getDropDownChangeHandler() {
        return dropDownChangeHandler;
    }
    public boolean getDropDownisVisible() {
        return dropDown != null && dropDown.isVisible();
    }
    public void hideDropDown() {
        if (dropDown != null)
            dropDown.hide();
    }
    public void setDropDownChangeHandler(FEventHandler changedHandler0) {
        dropDownChangeHandler = changedHandler0;
    }

    @Override
    public float getAutoSizeWidth() {
        //use widest item width to determine auto-size width
        float maxTextWidth = 0;
        for (T item : items) {
            float width = font.getBounds(getDisplayText(item)).width;
            if (width > maxTextWidth) {
                maxTextWidth = width;
            }
        }
        return PADDING + maxTextWidth + getRightPadding();
    }

    @Override
    public void draw(Graphics g) {
        super.draw(g);

        float divotWidth = getDivotWidth();
        float divotHeight = divotWidth * 0.5f;
        float x3 = getWidth() - PADDING - 1;
        float x1 = x3 - divotWidth;
        float x2 = x1 + divotWidth / 2;
        float y1 = getHeight() / 2 - 1;
        float y2 = y1 + divotHeight;
        float y3 = y1;
        g.fillTriangle(getForeColor(), x1, y1, x2, y2, x3, y3);
    }

    private float getDivotWidth() {
        return getHeight() / 3 + 1;
    }

    @Override
    protected float getLeftPadding() {
        if (getAlignment() == Align.center) {
            return getRightPadding(); //match right padding if center aligned
        }
        return super.getLeftPadding();
    }

    @Override
    protected float getRightPadding() {
        return getDivotWidth() + 2 * PADDING;
    }

    /** A group label in a grouped list, with a divider above every group but the first. Tapping it does nothing. */
    private static class HeadingMenuItem extends FMenuItem {
        private static final FSkinFont FONT = FSkinFont.get(12);
        private final boolean divider;

        private HeadingMenuItem(final String text, final boolean divider0) {
            // Enabled, because a disabled item is drawn faded
            super(text, null, true);
            divider = divider0;
        }

        @Override
        public boolean tap(final float x, final float y, final int count) {
            return true;
        }

        @Override
        public float getItemHeight() {
            return HEIGHT * 0.7f;
        }

        @Override
        public void draw(final Graphics g) {
            final float w = getWidth();
            final float h = getHeight();
            if (divider) {
                g.drawLine(1, getForeColor().alphaColor(0.3f), 0, 0, w, 0);
            }
            g.drawText(getText(), FONT, getForeColor(), GAP_X, h * 0.2f, w - 2 * GAP_X, h * 0.8f, false, Align.left, true);
        }
    }

    /** A choice in a grouped list: indented under its heading, with a check when it is the current choice and no divider of its own. */
    private static class GroupedMenuItem extends FMenuItem {
        private static final FSkinFont FONT = FSkinFont.get(11);
        private final boolean indented, checked;

        private GroupedMenuItem(final String text, final FEventHandler handler, final boolean indented0, final boolean checked0) {
            super(text, handler);
            indented = indented0;
            checked = checked0;
        }

        @Override
        public void draw(final Graphics g) {
            final float w = getWidth();
            if (showPressedColor()) {
                g.fillRect(FSkinColor.get(FSkinColor.Colors.CLR_ACTIVE).alphaColor(0.9f), 0, 0, w, HEIGHT);
            }
            final float x = indented ? GAP_X * 4 : GAP_X;
            final float textWidth = FONT.getBounds(getText()).width;
            g.drawText(getText(), FONT, getForeColor(), x, 0, w - x - GAP_X, HEIGHT, false, Align.left, true);
            if (checked) {
                final float size = HEIGHT * 0.25f;
                final float cx = x + textWidth + GAP_X * 2;
                final float cy = HEIGHT / 2;
                g.drawLine(2, getForeColor(), cx, cy, cx + size * 0.35f, cy + size * 0.35f);
                g.drawLine(2, getForeColor(), cx + size * 0.35f, cy + size * 0.35f, cx + size, cy - size * 0.4f);
            }
        }
    }

    private class DropDown extends FDropDownMenu {
        private static final Color GROUPED_BACK = new Color(35 / 255f, 40 / 255f, 44 / 255f, 1f);

        private boolean isGrouped() {
            return FComboBox.this.items.stream().anyMatch(ListHeading.class::isInstance);
        }

        @Override
        protected boolean autoHide() {
            return autoClose;
        }

        @Override
        protected void drawBackground(final Graphics g) {
            super.drawBackground(g);
            // The frosted background lets the screen show through, which hides the dimmed headings
            if (isGrouped()) {
                g.fillRect(GROUPED_BACK, 0, 0, getWidth(), getHeight());
            }
        }

        @Override
        protected void drawOverlay(final Graphics g) {
            super.drawOverlay(g);
            if (!isGrouped() || getMaxScrollTop() <= 0) {
                return;
            }
            final float trackHeight = getHeight();
            final float thumbHeight = Math.max(trackHeight * trackHeight / getScrollHeight(), trackHeight / 8);
            final float thumbTop = (trackHeight - thumbHeight) * getScrollTop() / getMaxScrollTop();
            final float thumbWidth = Utils.scale(4);
            g.fillRect(FSkinColor.get(FSkinColor.Colors.CLR_TEXT).alphaColor(0.45f),
                    getWidth() - thumbWidth - Utils.scale(2), thumbTop, thumbWidth, thumbHeight);
        }

        @Override
        public boolean tap(float x, float y, int count) {
            if (!autoClose)
                hide();
            return super.tap(x, y, count);
        }

        @Override
        protected void buildMenu() {
            final boolean grouped = isGrouped();
            boolean underHeading = false;
            for (final T item : FComboBox.this.items) {
                if (item instanceof ListHeading) {
                    addItem(new HeadingMenuItem(item.toString(), item != FComboBox.this.items.get(0)));
                    underHeading = true;
                    continue;
                }
                final FEventHandler onTap = e -> {
                    if (dropDownItemTap != null) {
                        dropDownItemTap.handleEvent(new FEvent(FComboBox.this, FEventType.TAP, item));
                    }
                    if (item != selectedItem) {
                        setSelectedItem(item);
                        if (dropDownChangeHandler != null) {
                            dropDownChangeHandler.handleEvent(new FEvent(FComboBox.this, FEventType.CHANGE, item));
                        }
                    }
                };
                if (grouped) {
                    addItem(new GroupedMenuItem(getDisplayText(item), onTap, underHeading, selectedItem == item));
                    continue;
                }
                FMenuItem menuItem = new FMenuItem(getDisplayText(item), onTap);
                if (selectedItem == item) {
                    menuItem.setSelected(true);
                }
                addItem(menuItem);
            }
        }

        @Override
        protected ScrollBounds updateAndGetPaneSize(float maxWidth, float maxVisibleHeight) {
            clear();
            items.clear();

            buildMenu();

            //determine needed width of menu
            float width = FComboBox.this.getWidth();

            //set bounds for each item
            float y = 0;
            for (FMenuItem item : items) {
                item.setBounds(0, y, width, FMenuItem.HEIGHT);
                y += FMenuItem.HEIGHT;
            }

            return new ScrollBounds(width, y);
        }

        @Override
        protected void updateSizeAndPosition() {
            FContainer container = getContainer();
            float containerHeight = container.getHeight();

            float x = container.screenToLocalX(FComboBox.this.localToScreenX(0));
            float y = FComboBox.this.localToScreenY(FComboBox.this.getHeight());

            float maxVisibleHeight = containerHeight - y;
            paneSize = updateAndGetPaneSize(FComboBox.this.getWidth(), maxVisibleHeight);

            setBounds(Math.round(x), Math.round(y), Math.round(FComboBox.this.getWidth()), Math.min(Math.round(paneSize.getHeight()), maxVisibleHeight));
        }

        @Override
        protected FDisplayObject getDropDownOwner() {
            return FComboBox.this;
        }
    }
}
