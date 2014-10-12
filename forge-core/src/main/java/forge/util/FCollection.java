package forge.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

//base class for a collection with quick lookup and that maintains order
public class FCollection<T> implements List<T>, Set<T>, FCollectionView<T>, Cloneable {
    private final HashSet<T> set = new HashSet<T>();
    private final LinkedList<T> list = new LinkedList<T>();

    public FCollection() {
    }
    public FCollection(T e) {
        add(e);
    }
    public FCollection(Collection<T> c) {
        for (T e : c) {
            add(e);
        }
    }
    public FCollection(Iterable<T> i) {
        for (T e : i) {
            add(e);
        }
    }

    @Override
    public int hashCode() {
        return list.hashCode();
    }

    @Override
    public String toString() {
        return list.toString();
    }

    protected FCollection<T> createNew() {
        return new FCollection<T>();
    }

    @Override
    public final Object clone() {
        FCollection<T> clone = createNew();
        clone.addAll(this);
        return clone;
    }

    public T getFirst() {
        return list.getFirst();
    }
    public T getLast() {
        return list.getLast();
    }

    @Override
    public int size() {
        return set.size();
    }

    @Override
    public boolean isEmpty() {
        return set.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return set.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return list.iterator();
    }

    @Override
    public Object[] toArray() {
        return list.toArray();
    }

    @Override
    public <V> V[] toArray(V[] a) {
        return list.toArray(a);
    }

    @Override
    public boolean add(T e) {
        if (set.add(e)) {
            list.add(e);
            return true;
        }
        return false;
    }

    @Override
    public boolean remove(Object o) {
        if (set.remove(o)) {
            list.remove(o);
            return true;
        }
        return false;
    }
    @Override
    public boolean containsAll(Collection<?> c) {
        return set.containsAll(c);
    }
    @Override
    public boolean addAll(Collection<? extends T> c) {
        boolean changed = false;
        for (T e : c) {
            if (add(e)) {
                changed = true;
            }
        }
        return changed;
    }
    public boolean addAll(Iterable<? extends T> c) {
        boolean changed = false;
        for (T e : c) {
            if (add(e)) {
                changed = true;
            }
        }
        return changed;
    }
    @SuppressWarnings("unchecked")
    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        if (c == null) { return false; }
        List<T> list;
        if (c instanceof List) {
            list = (List<T>)c;
        }
        else {
            list = new ArrayList<T>(c);
        }
        boolean changed = false;
        for (int i = list.size() - 1; i >= 0; i--) { //must add in reverse order so they show up in the right place
            if (insert(index, list.get(i))) {
                changed = true;
            }
        }
        return changed;
    }
    @Override
    public boolean removeAll(Collection<?> c) {
        boolean changed = false;
        for (Object o : c) {
            if (remove(o)) {
                changed = true;
            }
        }
        return changed;
    }
    public boolean removeAll(Iterable<T> c) {
        boolean changed = false;
        for (T o : c) {
            if (remove(o)) {
                changed = true;
            }
        }
        return changed;
    }
    @Override
    public boolean retainAll(Collection<?> c) {
        if (set.retainAll(c)) {
            list.retainAll(c);
            return true;
        }
        return false;
    }
    @Override
    public void clear() {
        if (set.isEmpty()) { return; }
        set.clear();
        list.clear();
    }
    @Override
    public T get(int index) {
        return list.get(index);
    }
    @Override
    public T set(int index, T element) {
        if (element == null) {
            return remove(index);
        }

        T oldItem = list.set(index, element);
        if (element != oldItem) {
            if (oldItem != null) {
                set.remove(oldItem);
            }
            set.add(element);
        }
        return oldItem;
    }
    @Override
    public void add(int index, T element) {
        insert(index, element);
    }
    private boolean insert(int index, T element) {
        if (set.add(element)) {
            list.add(index, element);
            return true;
        }
        //re-position in list if needed
        int oldIndex = list.indexOf(element);
        if (index == oldIndex) { return false; }

        if (index > oldIndex) {
            index--; //account for being removed
        }
        list.remove(oldIndex);
        list.add(index, element);
        return true;
    }
    @Override
    public T remove(int index) {
        T removedItem = list.remove(index);
        if (removedItem != null) {
            set.remove(removedItem);
        }
        return removedItem;
    }
    @Override
    public int indexOf(Object o) {
        return list.indexOf(o);
    }
    @Override
    public int lastIndexOf(Object o) {
        return list.lastIndexOf(o);
    }
    @Override
    public ListIterator<T> listIterator() {
        return list.listIterator();
    }
    @Override
    public ListIterator<T> listIterator(int index) {
        return list.listIterator(index);
    }
    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        FCollection<T> subList = createNew();
        for (int i = fromIndex; i < toIndex; i++) {
            subList.add(list.get(i));
        }
        return subList;
    }
}
