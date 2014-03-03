package forge.toolbox;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;

public class FComboBox<E> extends FDisplayObject {
    private static final FSkinColor foreColor = FSkinColor.get(Colors.CLR_TEXT);
    private static final FSkinColor backColor = FSkinColor.get(Colors.CLR_THEME2);

    private final List<E> items = new ArrayList<E>();
    private int selectedIndex;
    private HAlignment alignment;

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

    private void initialize() {
        selectedIndex = items.isEmpty() ? -1 : 0;
    }

    public void addItem(E item) {
        if (items.isEmpty()) {
            selectedIndex = 0; //select item if no items previously
        }
        items.add(item);
    }

    public void removeItem(E item) {
        if (items.remove(item)) {
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
            return items.get(selectedIndex);
        }
        return null;
    }

    public HAlignment getAlignment() {
        return alignment;
    }

    public void setAlignment(HAlignment alignment0) {
        alignment = alignment0;
    }

    @Override
    public void draw(Graphics g) {
        float shapeWidth = 8;
        float shapeHeight = 8;
        float x = getWidth() - shapeWidth - 6;
        float y = getHeight() / 2 - 1;
        if (getHeight() > 26) { //increase arrow size if taller combo box
            shapeWidth += 2;
            shapeHeight += 2;
            x -= 4;
            y--;
        }
        g.fillTriangle(foreColor, x, y, x + shapeWidth, y, x + (shapeWidth / 2), y + (shapeHeight / 2));
    }
}
