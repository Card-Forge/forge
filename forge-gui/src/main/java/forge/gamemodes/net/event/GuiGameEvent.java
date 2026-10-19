package forge.gamemodes.net.event;

import forge.gamemodes.net.ProtocolMethod;

public final class GuiGameEvent implements IdentifiableNetEvent {
    private static final long serialVersionUID = 6223690008522514574L;
    private static int staticId = 0;

    private final int id;
    private final ProtocolMethod method;
    private final Object[] objects;
    private boolean replaceTrackables = true;

    public GuiGameEvent(final ProtocolMethod method, final Object ... objects) {
        this.id = staticId++;
        this.method = method;
        this.objects = objects == null ? new Object[0] : objects;
    }
    public GuiGameEvent(final ProtocolMethod method, boolean replace, final Object ... objects) {
        this(method, objects);
        replaceTrackables = replace;
    }

    @Override
    public String toString() {
        return String.format("GuiGameEvent %d: %s (%d args)", id, method, objects.length);
    }

    @Override
    public int getId() {
        return id;
    }

    public ProtocolMethod getMethod() {
        return method;
    }

    public Object[] getObjects() {
        return objects;
    }

    public boolean replaceTrackables() {
        return replaceTrackables;
    }
}
