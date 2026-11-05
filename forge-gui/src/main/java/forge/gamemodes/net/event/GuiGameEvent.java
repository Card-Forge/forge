package forge.gamemodes.net.event;

import forge.gamemodes.net.ProtocolMethod;

public final class GuiGameEvent implements IdentifiableNetEvent {
    private static final long serialVersionUID = 6223690008522514574L;
    private static int staticId = 0;

    private final int id;
    private final ProtocolMethod method;
    private final Object[] objects;
    private final byte[] encodedObjects;

    public GuiGameEvent(final ProtocolMethod method, final Object ... objects) {
        this.id = staticId++;
        this.method = method;
        this.objects = objects == null ? new Object[0] : objects;
        this.encodedObjects = null;
    }
    private GuiGameEvent(final int id, final ProtocolMethod method, final byte[] encodedObjects) {
        this.id = id;
        this.method = method;
        this.objects = new Object[0];
        this.encodedObjects = encodedObjects;
    }

    public GuiGameEvent withEncodedObjects(final byte[] encodedObjects) {
        return new GuiGameEvent(id, method, encodedObjects);
    }

    @Override
    public String toString() {
        return encodedObjects == null
                ? String.format("GuiGameEvent %d: %s (%d args)", id, method, objects.length)
                : String.format("GuiGameEvent %d: %s (%d encoded bytes)", id, method, encodedObjects.length);
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

    /** Null unless the objects travel encoded, in which case {@link #getObjects()} is empty. */
    public byte[] getEncodedObjects() {
        return encodedObjects;
    }
}
