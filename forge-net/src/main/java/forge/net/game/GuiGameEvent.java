package forge.net.game;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import forge.net.game.server.RemoteClient;
import forge.trackable.TrackableObject;

public final class GuiGameEvent implements NetEvent {
    private static final long serialVersionUID = 6223690008522514574L;

    private final String method;
    private final Iterable<? extends TrackableObject> objects;

    public GuiGameEvent(final String method, final Iterable<? extends TrackableObject> objects) {
        this.method = method;
        this.objects = objects == null ? ImmutableSet.<TrackableObject>of() : objects;
    }

    @Override
    public void updateForClient(final RemoteClient client) {
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
