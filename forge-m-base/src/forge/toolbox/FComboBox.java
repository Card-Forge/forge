package forge.toolbox;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinColor.Colors;

public class FComboBox<E> extends FDisplayObject {
    private static final FSkinColor FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT);
    private static final FSkinColor BACK_COLOR = FSkinColor.get(Colors.CLR_THEME2);
    private static final FSkinColor BORDER_COLOR = BACK_COLOR.getContrastColor(10);

    private final List<E> items = new ArrayList<E>();
    private int selectedIndex;
    private HAlignment alignment;
    private FSkinFont font;

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
        font = FSkinFont.get(12);
        alignment = HAlignment.LEFT;
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
        float w = getWidth();
        float h = getHeight();

        g.fillRect(BACK_COLOR, 0, 0, w, h);
        g.drawRect(BORDER_COLOR, 0, 0, w, h);

        float shapeWidth = 8;
        float shapeHeight = 8;
        float x = w - shapeWidth - 6;
        float y = h / 2 - 1;
        if (getHeight() > 26) { //increase arrow size if taller combo box
            shapeWidth += 2;
            shapeHeight += 2;
            x -= 4;
            y--;
        }
        g.fillTriangle(FORE_COLOR, x, y, x + shapeWidth, y, x + (shapeWidth / 2), y + (shapeHeight / 2));

        E selectedItem = getSelectedItem();
        if (selectedItem != null) {
            g.drawText(selectedItem.toString(), font, FORE_COLOR, 3, 0, x - 6, h, false, alignment, true);
        }
    }
}
