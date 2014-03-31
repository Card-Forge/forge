package forge.toolbox;

import java.util.ArrayList;
import java.util.List;

import forge.Forge.Graphics;

public class FComboBox<E> extends FTextField {
    private final List<E> items = new ArrayList<E>();

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
    }

    @Override
    public void draw(Graphics g) {
        
    }
}
