package forge.trackable;

import java.util.LinkedHashSet;

@SuppressWarnings("serial")
public class TrackableCollection<T extends TrackableObject<?>> extends LinkedHashSet<T> { //use linked hash set so order is maintained
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
