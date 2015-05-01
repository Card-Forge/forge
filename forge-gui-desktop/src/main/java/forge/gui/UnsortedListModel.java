package forge.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.ListModel;

import com.google.common.collect.Lists;

@SuppressWarnings("serial")
public class UnsortedListModel<T> extends AbstractListModel<T> {
    final List<T> model;

    public UnsortedListModel() {
        model = Lists.newArrayList();
    }

    @Override
    public int getSize() {
        return model.size();
    }

    @Override
    public T getElementAt(final int index) {
        return model.get(index);
    }

    public void add(final T element) {
        model.add(element);
        fireIntervalAdded(this, getSize() - 1, getSize() - 1);
        fireContentsChanged(this, 0, getSize() - 1);
    }

    public void addAll(final Collection<T> elements) {
        if (elements.isEmpty()) {
            return;
        }
        model.addAll(elements);
        fireIntervalAdded(this, getSize() - elements.size(), getSize() - 1);
        fireContentsChanged(this, 0, getSize() - 1);
    }

    public void addAll(final ListModel<T> otherModel) {
        final Collection<T> elements = new ArrayList<T>();
        final int size = otherModel.getSize();
        for (int i = 0; size > i; ++i) {
            elements.add(otherModel.getElementAt(i));
        }
        addAll(elements);
    }

    public void clear() {
        final int prevSize = getSize();
        model.clear();
        fireIntervalRemoved(this, 0, prevSize - 1);
        fireContentsChanged(this, 0, prevSize - 1);
    }

    public boolean contains(final Object element) {
        return model.contains(element);
    }

    public Iterator<T> iterator() {
        return model.iterator();
    }

    public void removeElement(final int idx) {
        model.remove(idx);
        fireIntervalRemoved(this, idx, idx);
        fireContentsChanged(this, 0, getSize());
    }
}
