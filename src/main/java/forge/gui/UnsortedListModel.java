package forge.gui;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractListModel;

@SuppressWarnings("serial")
class UnsortedListModel extends AbstractListModel {
    List<Object> model;

    public UnsortedListModel() {
        model = new LinkedList<Object>();
    }

    @Override
    public int getSize() {
        return model.size();
    }

    @Override
    public Object getElementAt(int index) {
        return model.get(index);
    }

    public void add(Object element) {
        if (model.add(element)) {
            fireContentsChanged(this, 0, getSize());
        }
    }

    public void addAll(Object elements[]) {
        Collection<Object> c = Arrays.asList(elements);
        model.addAll(c);
        fireContentsChanged(this, 0, getSize());
    }

    public void clear() {
        model.clear();
        fireContentsChanged(this, 0, getSize());
    }

    public boolean contains(Object element) {
        return model.contains(element);
    }

    public Iterator<Object> iterator() {
        return model.iterator();
    }

    public boolean removeElement(Object element) {
        boolean removed = model.remove(element);
        if (removed) {
            fireContentsChanged(this, 0, getSize());
        }
        return removed;
    }
}