package forge.toolbox;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinColor.Colors;
import forge.utils.Utils;

public class FList<E> extends FScrollPane {
    public static final float PREFERRED_HEIGHT = Utils.AVG_FINGER_HEIGHT * 0.7f;

    private static final FSkinColor FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT);
    private static final FSkinColor BACK_COLOR = FSkinColor.get(Colors.CLR_THEME2);
    private static final FSkinColor BORDER_COLOR = FORE_COLOR.getContrastColor(40);

    private final List<ListItem> items = new ArrayList<ListItem>();
    private int selectedIndex;
    private FSkinFont font;
    private ListItemRenderer renderer;

    public FList() {
        initialize();
    }
    public FList(E[] itemArray) {
        for (E item : itemArray) {
            items.add(add(new ListItem(item)));
        }
        initialize();
    }
    public FList(Iterable<E> items0) {
        for (E item : items0) {
            items.add(new ListItem(item));
        }
        initialize();
    }

    private void initialize() {
        selectedIndex = items.isEmpty() ? -1 : 0;
        font = FSkinFont.get(12);
        renderer = new DefaultListItemRenderer();
    }

    public void addItem(E item) {
        if (items.isEmpty()) {
            selectedIndex = 0; //select item if no items previously
        }
        items.add(add(new ListItem(item)));
    }

    private int getIndexOfItem(E item) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).value == item) {
                return i;
            }
        }
        return -1;
    }

    public void removeItem(E item) {
        int index = getIndexOfItem(item);
        if (index >= 0) {
            remove(items.get(index));
            items.remove(index);
            if (selectedIndex >= items.size()) {
                selectedIndex = items.size() - 1;
            }
        }
    }

    public int getItemCount() {
        return items.size();
    }

    public E getSelectedItem() {
        if (selectedIndex >= 0) {
            return items.get(selectedIndex).value;
        }
        return null;
    }

    public void setSelectedItem(E item) {
        int index = getIndexOfItem(item);
        if (index >= 0) {
            selectedIndex = index;
        }
    }

    public void setListItemRenderer(ListItemRenderer renderer0) {
        renderer = renderer0;
    }

    @Override
    protected void doLayout(float width, float height) {
        float y = 0;
        float itemHeight = renderer.getItemHeight();

        for (ListItem item : items) {
            item.setBounds(0, y, width, itemHeight);
            y += itemHeight;
        }
    }

    @Override
    protected void drawBackground(Graphics g) {
        float w = getWidth();
        float h = getHeight();

        g.fillRect(BACK_COLOR, 0, 0, w, h);
        g.drawRect(BORDER_COLOR, 0, 0, w, h);
    }

    private class ListItem extends FDisplayObject {
        private final E value;

        private ListItem(E value0) {
            value = value0;
        }

        @Override
        public final void draw(Graphics g) {
            renderer.drawValue(g, value, font, FORE_COLOR, getWidth(), getHeight());
        }
    }

    public abstract class ListItemRenderer {
        public abstract float getItemHeight();
        public abstract void drawValue(Graphics g, E value, FSkinFont font, FSkinColor foreColor, float width, float height);
    }

    public class DefaultListItemRenderer extends ListItemRenderer {
        @Override
        public float getItemHeight() {
            return 25;
        }

        @Override
        public void drawValue(Graphics g, E value, FSkinFont font, FSkinColor color, float width, float height) {
            g.drawText(value.toString(), font, color, 0, 0, width, height, false, HAlignment.LEFT, true);
        }
    }
}
