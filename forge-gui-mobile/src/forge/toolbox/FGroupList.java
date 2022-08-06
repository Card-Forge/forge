package forge.toolbox;

import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinTexture;
import forge.screens.FScreen;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FList.DefaultListItemRenderer;
import forge.toolbox.FList.ListItemRenderer;
import forge.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class FGroupList<E> extends FScrollPane {
    private static final float GROUP_HEADER_HEIGHT = Math.round(Utils.AVG_FINGER_HEIGHT * 0.6f);

    private final List<ListGroup> groups = new ArrayList<>();
    private FSkinFont font;
    private ListItemRenderer<E> renderer;

    public FGroupList() {
        initialize();
    }
    public FGroupList(E[] itemArray) {
        for (E item : itemArray) {
            addItem(item);
        }
        initialize();
    }
    public FGroupList(Iterable<E> items0) {
        for (E item : items0) {
            addItem(item);
        }
        initialize();
    }

    private void initialize() {
        font = FSkinFont.get(14);
        renderer = new DefaultListItemRenderer<>();
    }

    public void addGroup(String groupName) {
        groups.add(add(new ListGroup(groupName)));
    }

    public void addItem(E item) {
        addItem(item, 0);
    }
    public void addItem(E item, int groupIndex) {
        if (groups.isEmpty()) {
            addGroup(null);
        }
        if (groupIndex > groups.size()) {
            groupIndex = groups.size() - 1;
        }
        groups.get(groupIndex).addItem(new ListItem(item));
    }

    public void removeItem(E item) {
        for (ListGroup group : groups) {
            for (ListItem groupItem : group.items) {
                if (groupItem.value == item) {
                    group.removeItem(groupItem);
                    if (group.items.isEmpty()) {
                        groups.remove(group);
                    }
                    return;
                }
            }
        }
    }

    @Override
    public void clear() {
        super.clear();
        groups.clear();
    }

    public void setListData(Iterable<E> items0) {
        clear();
        for (E item : items0) {
            addItem(item);
        }
        revalidate();
    }

    public boolean isEmpty() {
        return groups.isEmpty();
    }

    public int getCount() {
        int count = 0;
        for (ListGroup group : groups) {
            count += group.items.size();
        }
        return count;
    }

    public ListItem getItemAt(int index) {
        int count = 0;
        for (ListGroup group : groups) {
            for (ListItem item : group.items) {
                if (index == count) {
                    return item;
                }
                count++;
            }
        }
        return null;
    }

    public E getItemValueAt(int index) {
        ListItem item = getItemAt(index);
        if (item == null) { return null; }
        return item.value;
    }

    public int getIndexOf(E value) {
        int count = 0;
        for (ListGroup group : groups) {
            for (ListItem item : group.items) {
                if (item.value == value) {
                    return count;
                }
                count++;
            }
        }
        return -1;
    }

    public void setListItemRenderer(ListItemRenderer<E> renderer0) {
        renderer = renderer0;
    }

    public FSkinFont getFont() {
        return font;
    }
    public void setFont(FSkinFont font0) {
        font = font0;
    }

    @Override
    protected void drawBackground(Graphics g) {
        //support scrolling texture with list
        g.drawImage(FSkinTexture.BG_TEXTURE, -getScrollLeft(), -getScrollTop(), getScrollWidth(), getScrollHeight());
        g.fillRect(FScreen.TEXTURE_OVERLAY_COLOR, 0, 0, getWidth(), getHeight());
    }

    @Override
    protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
        float y = 0;
        float groupHeight;

        for (ListGroup group : groups) {
            if (group.isVisible()) {
                groupHeight = group.getPreferredHeight();
                group.setBounds(0, y, visibleWidth, groupHeight);
                y += groupHeight;
            }
        }

        return new ScrollBounds(visibleWidth, y);
    }

    private class ListGroup extends FContainer {
        private final FLabel header;
        private final List<ListItem> items = new ArrayList<>();

        private boolean isCollapsed;

        private ListGroup(String name0) {
            if (name0 == null) {
                header = null;
            }
            else {
                header = add(new FLabel.ButtonBuilder().text(name0).command(new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                        isCollapsed = !isCollapsed;
                        FGroupList.this.revalidate();
                    }
                }).build());
            }
            setVisible(false); //hide by default unless it has items
        }

        public void addItem(ListItem item) {
            items.add(item);
            add(item);
            setVisible(true);
        }

        public boolean removeItem(ListItem item) {
            if (items.remove(item)) {
                remove(item);
                setVisible(items.size() > 0);
                return true;
            }
            return false;
        }

        public float getPreferredHeight() {
            float height = 0;
            if (header != null) {
                height += GROUP_HEADER_HEIGHT;
            }
            if (!isCollapsed) {
                height += renderer.getItemHeight() * items.size() + 1; //+1 so bottom border not cut off
            }
            return height;
        }

        @Override
        protected void doLayout(float width, float height) {
            float y = 0;
            if (header != null) {
                header.setBounds(0, y, width, GROUP_HEADER_HEIGHT);
                y += GROUP_HEADER_HEIGHT;
            }

            float itemHeight = renderer.getItemHeight();

            for (ListItem item : items) {
                item.setBounds(0, y, width, itemHeight);
                y += itemHeight;
            }
        }
    }

    public class ListItem extends FDisplayObject {
        private final E value;
        private boolean pressed;

        private ListItem(E value0) {
            value = value0;
        }

        public boolean press(float x, float y) {
            pressed = true;
            return true;
        }

        public boolean release(float x, float y) {
            pressed = false;
            return true;
        }

        public boolean tap(float x, float y, int count) {
            return renderer.tap(-1, value, x, y, count);
        }

        public boolean longPress(float x, float y) {
            return renderer.showMenu(-1, value, this, x, y);
        }

        @Override
        public final void draw(Graphics g) {
            float w = getWidth();
            float h = renderer.getItemHeight();

            FSkinColor fillColor = getItemFillColor(this);
            if (fillColor != null) {
                g.fillRect(fillColor, 0, 0, w, h);
            }

            renderer.drawValue(g, -1, value, font, FList.FORE_COLOR, fillColor, pressed, FList.PADDING, FList.PADDING, w - 2 * FList.PADDING, h - 2 * FList.PADDING);

            if (drawLineSeparators()) {
                g.drawLine(1, FList.LINE_COLOR, 0, h, w, h);
            }
        }
    }

    protected FSkinColor getItemFillColor(ListItem item) {
        if (item.pressed) {
            return FList.PRESSED_COLOR;
        }
        return null;
    }

    protected boolean drawLineSeparators() {
        return true;
    }
}
