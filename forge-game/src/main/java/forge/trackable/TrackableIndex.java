package forge.trackable;

import java.util.HashMap;

@SuppressWarnings("serial")
public class TrackableIndex<T extends TrackableObject> extends HashMap<Integer, T> {
    public TrackableIndex() {
    }
}
