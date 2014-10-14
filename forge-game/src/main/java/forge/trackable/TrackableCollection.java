package forge.trackable;

import java.util.Collection;

import forge.util.FCollection;

public class TrackableCollection<T extends TrackableObject> extends FCollection<T> {
    public TrackableCollection() {
    }
    public TrackableCollection(T e) {
        super(e);
    }
    public TrackableCollection(Collection<T> c) {
        super(c);
    }
    public TrackableCollection(Iterable<T> i) {
        super(i);
    }

    @Override
    public boolean add(T item) {
        //TODO: Track change
        return super.add(item);
    }

    @Override
    public boolean remove(Object item) {
        //TODO: Track change
        return super.remove(item);
    }
}
