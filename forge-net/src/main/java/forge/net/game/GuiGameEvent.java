package forge.net.game;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import forge.trackable.TrackableObject;

public final class GuiGameEvent implements NetEvent {

    private final String method;
    private final Iterable<? extends TrackableObject> objects;

    public GuiGameEvent(final String method, final Iterable<? extends TrackableObject> objects) {
        this.method = method;
        this.objects = objects == null ? ImmutableSet.<TrackableObject>of() : objects;
    }

    public String getMethod() {
        return method;
    }

    public TrackableObject getObject() {
        return Iterables.getFirst(objects, null);
    }

    public Iterable<? extends TrackableObject> getObjects() {
        return objects;
    }
}
