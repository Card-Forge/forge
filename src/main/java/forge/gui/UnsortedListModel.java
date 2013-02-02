package forge.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractListModel;


@SuppressWarnings("serial")
public class UnsortedListModel<T> extends AbstractListModel { // Java 7 has a generic version. In 6 we have to cast types
    List<T> model;

    public UnsortedListModel() {
        model = new ArrayList<T>();
    }

    @Override
    public int getSize() {
        return model.size();
    }

    @Override
    public Object getElementAt(int index) {
        return model.get(index);
    }

    public void add(T element) {
        if (model.add(element)) {
            fireContentsChanged(this, 0, getSize());
        }
    }

    public void addAll(T[] elements) {
        for (T e : elements) {
            model.add(e);
        }
        fireContentsChanged(this, 0, getSize());
    }

    public void addAll(Collection<T> elements) {
        model.addAll(elements);
        fireContentsChanged(this, 0, getSize());
    }


    public void clear() {
        model.clear();
        fireContentsChanged(this, 0, getSize());
    }

    public boolean contains(Object element) {
        return model.contains(element);
    }

    public Iterator<T> iterator() {
        return model.iterator();
    }

    public void removeElement(int idx) {
        model.remove(idx);
        fireIntervalRemoved(this, idx, idx);
        fireContentsChanged(this, 0, getSize());
    }
}
