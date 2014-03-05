package forge.toolbox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinColor.Colors;
import forge.utils.Utils;

public class FList<E> extends FScrollPane {
    private static final float INSETS_FACTOR = 0.025f;
    private static final float GROUP_HEADER_HEIGHT = Utils.AVG_FINGER_HEIGHT * 0.7f;
    private static final FSkinColor FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT);
    private static final FSkinColor PRESSED_COLOR = FSkinColor.get(Colors.CLR_THEME2).alphaColor(0.75f);
    private static final FSkinColor LINE_COLOR = FORE_COLOR.alphaColor(0.5f);

    private final Map<String, ListGroup> groups = new TreeMap<String, ListGroup>();
    private FSkinFont font;
    private ListItemRenderer renderer;

    public FList() {
        initialize();
    }
    public FList(E[] itemArray) {
        for (E item : itemArray) {
            addItem(item);
        }
        initialize();
    }
    public FList(Iterable<E> items0) {
        for (E item : items0) {
            addItem(item);
        }
        initialize();
    }

    private void initialize() {
        font = FSkinFont.get(14);
        renderer = new DefaultListItemRenderer();
    }

    public void addItem(E item) {
        addItem(item, "");
    }
    public void addItem(E item, String groupName) {
        ListGroup group = groups.get(groupName);
        if (group == null) {
            group = add(new ListGroup(groupName));
            groups.put(groupName, group);
        }
        group.addItem(new ListItem(item));
    }

    public void removeItem(E item) {
        for (ListGroup group : groups.values()) {
            for (ListItem groupItem : group.items) {
                if (groupItem.value == item) {
                    group.removeItem(groupItem);
                    if (group.items.isEmpty()) {
                        groups.remove(group.getName());
                    }
                    return;
                }
            }
        }
    }

    public void setListItemRenderer(ListItemRenderer renderer0) {
        renderer = renderer0;
    }

    @Override
    protected void doLayout(float width, float height) {
        float y = 0;
        float groupHeight;

        for (ListGroup group : groups.values()) {
            groupHeight = group.getPreferredHeight();
            group.setBounds(0, y, width, groupHeight);
            y += groupHeight;
        }
    }

    private class ListGroup extends FContainer {
        private final FLabel header;
        private final List<ListItem> items = new ArrayList<ListItem>();

        private boolean isCollapsed;

        private ListGroup(String name0) {
            if (name0.isEmpty()) {
                header = null;
            }
            else {
                header = new FLabel.ButtonBuilder().text(name0).command(new Runnable() {
                    @Override
                    public void run() {
                        isCollapsed = !isCollapsed;
                        FList.this.revalidate();
                    }
                }).build();
            }
        }

        public String getName() {
            return header == null ? null : header.getText();
        }

        public void addItem(ListItem item) {
            items.add(item);
            add(item);
        }

        public boolean removeItem(ListItem item) {
            if (items.remove(item)) {
                remove(item);
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
                height += (renderer.getItemHeight() + 1) * items.size();
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

            float itemHeight = renderer.getItemHeight() + 1; //account for bottom border

            for (ListItem item : items) {
                item.setBounds(0, y, width, itemHeight);
                y += itemHeight;
            }
        }
    }

    private class ListItem extends FDisplayObject {
        private final E value;
        private boolean pressed;

        private ListItem(E value0) {
            value = value0;
        }

        public boolean touchDown(float x, float y) {
            pressed = true;
            return true;
        }

        public boolean touchUp(float x, float y) {
            pressed = false;
            return true;
        }

        public boolean tap(float x, float y, int count) {
            return renderer.tap(value, x, y, count);
        }

        @Override
        public final void draw(Graphics g) {
            float w = getWidth();
            float h = renderer.getItemHeight();

            if (pressed) {
                g.fillRect(PRESSED_COLOR, 0, 0, w, h);
            }

            renderer.drawValue(g, value, font, FORE_COLOR, w, h);

            float y = h + 1;
            g.drawLine(LINE_COLOR, 0, y, w, y);
        }
    }

    public abstract class ListItemRenderer {
        public abstract float getItemHeight();
        public abstract boolean tap(E value, float x, float y, int count);
        public abstract void drawValue(Graphics g, E value, FSkinFont font, FSkinColor foreColor, float width, float height);
    }

    public class DefaultListItemRenderer extends ListItemRenderer {
        @Override
        public float getItemHeight() {
            return Utils.AVG_FINGER_HEIGHT;
        }

        @Override
        public boolean tap(E value, float x, float y, int count) {
            return false;
        }

        @Override
        public void drawValue(Graphics g, E value, FSkinFont font, FSkinColor color, float width, float height) {
            float x = width * INSETS_FACTOR;
            g.drawText(value.toString(), font, color, x, 0, width - 2 * x, height, false, HAlignment.LEFT, true);
        }
    }
}
