package forge.trackable;

import forge.util.FCollection;

public class TrackableCollection<T extends TrackableObject> extends FCollection<T> {
    public TrackableCollection() {
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
