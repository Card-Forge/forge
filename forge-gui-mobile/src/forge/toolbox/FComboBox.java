package forge.toolbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.badlogic.gdx.utils.Align;

import forge.Graphics;
import forge.gui.interfaces.IComboBox;
import forge.menu.FDropDownMenu;
import forge.menu.FMenuItem;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FEvent.FEventType;

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
        if (selectedItem == item) { return; }

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
        g.fillTriangle(FORE_COLOR, x1, y1, x2, y2, x3, y3);
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

    private class DropDown extends FDropDownMenu {
        @Override
        protected boolean autoHide() {
            return autoClose;
        }

        @Override
        public boolean tap(float x, float y, int count) {
            if (!autoClose)
                hide();
            return super.tap(x, y, count);
        }

        @Override
        protected void buildMenu() {
            for (final T item : FComboBox.this.items) {
                FMenuItem menuItem = new FMenuItem(getDisplayText(item), new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                        if (dropDownItemTap != null) {
                            dropDownItemTap.handleEvent(new FEvent(FComboBox.this, FEventType.TAP, item));
                        }
                        if (item != selectedItem) {
                            setSelectedItem(item);
                            if (dropDownChangeHandler != null) {
                                dropDownChangeHandler.handleEvent(new FEvent(FComboBox.this, FEventType.CHANGE, item));
                            }
                        }
                    }
                });
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
