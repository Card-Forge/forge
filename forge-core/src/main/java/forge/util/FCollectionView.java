package forge.util;

import java.util.List;

//Interface to expose only the desired functions of CardType without allowing modification
public interface FCollectionView<T> extends Iterable<T> {
    boolean isEmpty();
    int size();
    T get(int index);
    T getFirst();
    T getLast();
    int indexOf(Object o);
    int lastIndexOf(Object o);
    boolean contains(Object o);
    List<T> subList(int fromIndex, int toIndex);
}