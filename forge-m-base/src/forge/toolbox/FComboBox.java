package forge.toolbox;

import java.util.ArrayList;
import java.util.List;

import forge.Forge;
import forge.Forge.Graphics;
import forge.menu.FDropDownMenu;
import forge.menu.FMenuItem;
import forge.screens.FScreen;
import forge.toolbox.FEvent.*;

public class FComboBox<E> extends FTextField {
    private final List<E> items = new ArrayList<E>();
    private E selectedItem;
    private final DropDown dropDown = new DropDown();

    public FComboBox() {
        initialize();
    }
    public FComboBox(E[] itemArray) {
        for (E item : itemArray) {
            items.add(item);
        }
        initialize();
    }
    public FComboBox(Iterable<E> items0) {
        for (E item : items0) {
            items.add(item);
        }
        initialize();
    }

    public void addItem(E item) {
        items.add(item);
    }

    public boolean removeItem(E item) {
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

    public E getSelectedItem() {
        return selectedItem;
    }

    public void setSelectedItem(E item) {
        if (selectedItem == item) { return; }

        if (item != null) {
            if (items.contains(item)) {
                selectedItem = item;
                super.setText(item.toString());
            }
        }
        else {
            selectedItem = null;
            super.setText("");
        }

        if (getChangedHandler() != null) {
            getChangedHandler().handleEvent(new FEvent(this, FEventType.CHANGE, item));
        }
    }

    @Override
    public void setText(String text0) {
        for (E item : items) {
            if (item.toString().equals(text0)) {
                setSelectedItem(item);
                return;
            }
        }
        selectedItem = null;
        setText(text0);
    }

    private void initialize() {
        if (!items.isEmpty()) {
            setSelectedItem(items.get(0)); //select first item by default
        }
    }

    @Override
    public boolean tap(float x, float y, int count) {
        dropDown.setVisible(!dropDown.isVisible());
        return true;
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
    protected float getRightPadding() {
        return getDivotWidth() + 2 * PADDING;
    }

    private class DropDown extends FDropDownMenu {
        @Override
        protected void buildMenu() {
            for (final E item : FComboBox.this.items) {
                addItem(new FMenuItem(item.toString(), new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                        setSelectedItem(item);
                    }
                }));
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
            FScreen screen = Forge.getCurrentScreen();
            float screenHeight = screen.getHeight();

            float x = FComboBox.this.localToScreenX(0);
            float y = FComboBox.this.localToScreenY(FComboBox.this.getHeight());

            paneSize = updateAndGetPaneSize(FComboBox.this.getWidth(), screenHeight - y);

            setBounds(Math.round(x), Math.round(y), Math.round(FComboBox.this.getWidth()), Math.round(paneSize.getHeight()));
        }

        @Override
        protected FDisplayObject getDropDownOwner() {
            return FComboBox.this;
        }
    }
}
