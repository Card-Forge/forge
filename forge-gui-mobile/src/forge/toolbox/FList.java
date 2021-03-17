package forge.toolbox;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.badlogic.gdx.utils.Align;

import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.assets.FSkinTexture;
import forge.item.InventoryItem;
import forge.itemmanager.filters.AdvancedSearchFilter;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.screens.FScreen;
import forge.util.Utils;

public class FList<T> extends FScrollPane implements Iterable<T> {
    public static final float PADDING = Utils.scale(3);
    public static final FSkinColor FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT);
    public static final FSkinColor PRESSED_COLOR = FSkinColor.get(Colors.CLR_ACTIVE).alphaColor(0.9f);
    public static final FSkinColor LINE_COLOR = FORE_COLOR.alphaColor(0.5f);
    public static final float LINE_THICKNESS = Utils.scale(1);

    protected final List<T> items = new ArrayList<>();
    private FSkinFont font;
    private ListItemRenderer<T> renderer;
    private int pressedIndex = -1;

    public FList() {
        initialize();
    }
    public FList(T[] itemArray) {
        for (T item : itemArray) {
            addItem(item);
        }
        initialize();
    }
    public FList(Iterable<? extends T> items0) {
        for (T item : items0) {
            addItem(item);
        }
        initialize();
    }

    private void initialize() {
        font = FSkinFont.get(14);
        renderer = new DefaultListItemRenderer<>();
    }

    public synchronized void addItem(T item) {
        items.add(item);
    }

    public synchronized void removeItem(T item) {
        items.remove(item);
    }

    @Override
    public synchronized void clear() {
        super.clear();
        items.clear();
    }

    public List<T> extractListData() {
        return new ArrayList<>(items); //create copy to avoid modifying items
    }
    public synchronized void setListData(Iterable<? extends T> items0) {
        clear();
        for (T item : items0) {
            addItem(item);
        }
        revalidate();
    }
    public synchronized void setListData(T[] items0) {
        clear();
        for (T item : items0) {
            addItem(item);
        }
        revalidate();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int getCount() {
        return items.size();
    }

    public T getItemAt(int index) {
        if (index < 0 || index >= items.size()) {
            return null;
        }
        return items.get(index);
    }

    public int getIndexOf(T item) {
        return items.indexOf(item);
    }

    public T getItemAtPoint(float x, float y) {
        return getItemAt(getIndexAtPoint(x, y));
    }

    public int getIndexAtPoint(float x, float y) {
        if (renderer.layoutHorizontal()) {
            return (int)((getScrollLeft() + x) / renderer.getItemHeight());
        }
        return (int)((getScrollTop() + y) / renderer.getItemHeight());
    }

    public ListItemRenderer<T> getListItemRenderer() {
        return renderer;
    }
    public void setListItemRenderer(ListItemRenderer<T> renderer0) {
        renderer = renderer0;
    }

    public FSkinFont getFont() {
        return font;
    }
    public void setFont(FSkinFont font0) {
        font = font0;
    }

    @Override
    protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
        if (renderer.layoutHorizontal()) {
            return new ScrollBounds(items.size() * renderer.getItemHeight(), visibleHeight);
        }
        return new ScrollBounds(visibleWidth, items.size() * renderer.getItemHeight());
    }

    @Override
    public boolean press(float x, float y) {
        pressedIndex = getIndexAtPoint(x, y);
        return true;
    }

    @Override
    public boolean release(float x, float y) {
        pressedIndex = -1;
        return true;
    }

    @Override
    public boolean tap(float x, float y, int count) {
        int index = getIndexAtPoint(x, y);
        T item = getItemAt(index);
        if (item == null) { return false; }

        if (renderer.layoutHorizontal()) {
            return renderer.tap(index, item, x - getItemStartPosition(index), y, count);
        }
        return renderer.tap(index, item, x, y - getItemStartPosition(index), count);
    }

    public boolean longPress(float x, float y) {
        int index = getIndexAtPoint(x, y);
        T item = getItemAt(index);
        if (item == null) { return false; }

        return renderer.showMenu(index, item, this, x, y);
    }

    //return scroll position based on layout orientation
    public float getScrollPosition() {
        if (renderer.layoutHorizontal()) {
            return getScrollLeft();
        }
        return getScrollTop();
    }
    public void setScrollPosition(float scrollPosition) {
        if (renderer.layoutHorizontal()) {
            setScrollLeft(scrollPosition);
        }
        setScrollTop(scrollPosition);
    }
    public float getVisibleSize() {
        if (renderer.layoutHorizontal()) {
            return getWidth();
        }
        return getHeight();
    }

    public float getItemStartPosition(int index) {
        return index * renderer.getItemHeight() - getScrollPosition();
    }

    public void scrollIntoView(int index) {
        float itemStartPos = getItemStartPosition(index);
        if (itemStartPos < 0) {
            setScrollPosition(getScrollPosition() + itemStartPos);
        }
        else {
            float itemEndPosition = itemStartPos + renderer.getItemHeight();
            float visibleSize = getVisibleSize();
            if (itemEndPosition > visibleSize) {
                setScrollPosition(getScrollPosition() + itemEndPosition - visibleSize);
            }
        }
    }

    @Override
    protected void drawBackground(Graphics g) {
        //support scrolling texture with list
        g.drawImage(FSkinTexture.BG_TEXTURE, -getScrollLeft(), -getScrollTop(), getScrollWidth(), getScrollHeight());
        g.fillRect(FScreen.TEXTURE_OVERLAY_COLOR, 0, 0, getWidth(), getHeight());
    }

    @Override
    public synchronized void draw(Graphics g) {
        float w = getWidth();
        float h = getHeight();
        g.startClip(0, 0, w, h);
        drawBackground(g);
        
        //draw only items that are visible
        if (!items.isEmpty()) {
            int startIndex = getIndexAtPoint(0, 0);
            boolean drawSeparators = drawLineSeparators();

            float x = 0;
            float y = Math.round(getItemStartPosition(startIndex)); //round y so items don't flicker from rounding error
            float itemWidth = w;
            float itemHeight = renderer.getItemHeight();

            boolean layoutHorizontal = renderer.layoutHorizontal();
            if (layoutHorizontal) {
                x = y;
                y = 0;
                itemWidth = itemHeight;
                itemHeight = h;
            }

            float padding = getPadding();
            float valueWidth = itemWidth - 2 * padding;
            float valueHeight = itemHeight - 2 * padding;

            for (int i = startIndex; i < items.size(); i++) {
                if (x > w || y > h) { break; }

                FSkinColor fillColor = getItemFillColor(i);
                if (fillColor != null) {
                    g.fillRect(fillColor, x, y, w, itemHeight);
                }

                renderer.drawValue(g, i, items.get(i), font, FORE_COLOR, fillColor, pressedIndex == i, x + padding, y + padding, valueWidth, valueHeight);

                if (layoutHorizontal) {
                    x += itemWidth;
                }
                else {
                    y += itemHeight;
                }

                if (drawSeparators) {
                    if (layoutHorizontal) {
                        x -= LINE_THICKNESS / 2;
                        g.drawLine(LINE_THICKNESS, LINE_COLOR, x, 0, x, h);
                    }
                    else {
                        y -= LINE_THICKNESS / 2;
                        g.drawLine(LINE_THICKNESS, LINE_COLOR, 0, y, w, y);
                    }
                }
            }
        }
        
        drawOverlay(g);
        g.endClip();
    }

    protected FSkinColor getItemFillColor(int index) {
        if (index == pressedIndex) {
            return FList.PRESSED_COLOR;
        }
        return null;
    }

    protected boolean drawLineSeparators() {
        return true;
    }

    protected float getPadding() {
        return PADDING;
    }

    public static abstract class ListItemRenderer<V> {
        public abstract float getItemHeight();
        public abstract boolean tap(Integer index, V value, float x, float y, int count);
        public abstract void drawValue(Graphics g, Integer index, V value, FSkinFont font, FSkinColor foreColor, FSkinColor backColor, boolean pressed, float x, float y, float w, float h);

        public boolean showMenu(Integer index, V value, FDisplayObject owner, float x, float y) {
            return false; //showing menu on long press is optional
        }

        public boolean layoutHorizontal() {
            return false; //this doesn't need to be overridden to specify vertical layouts
        }

        public AdvancedSearchFilter<? extends InventoryItem> getAdvancedSearchFilter(ListChooser<V> listChooser) {
            return null; //allow overriding to support advanced search
        }
    }

    public static class DefaultListItemRenderer<V> extends ListItemRenderer<V> {
        @Override
        public float getItemHeight() {
            return Utils.AVG_FINGER_HEIGHT;
        }

        @Override
        public boolean tap(Integer index, V value, float x, float y, int count) {
            return false;
        }

        @Override
        public void drawValue(Graphics g, Integer index, V value, FSkinFont font, FSkinColor foreColor, FSkinColor backColor, boolean pressed, float x, float y, float w, float h) {
            g.drawText(value.toString(), font, foreColor, x, y, w, h, false, Align.left, true);
        }
    }

    public static class CompactModeHandler {
        private static final float REQ_AMOUNT = Utils.AVG_FINGER_WIDTH;

        private float totalZoomAmount;
        private boolean compactMode = FModel.getPreferences().getPrefBoolean(FPref.UI_COMPACT_LIST_ITEMS);

        public boolean isCompactMode() {
            return compactMode;
        }
        public void setCompactMode(boolean compactMode0) {
            compactMode = compactMode0;
        }

        public boolean update(float amount) {
            totalZoomAmount += amount;

            if (totalZoomAmount >= REQ_AMOUNT) {
                compactMode = false;
                totalZoomAmount = 0;
                return true;
            }
            if (totalZoomAmount <= -REQ_AMOUNT) {
                compactMode = true;
                totalZoomAmount = 0;
                return true;
            }
            return false;
        }
    }

    @Override
    public Iterator<T> iterator() {
        return items.iterator();
    }
}
