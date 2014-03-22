package forge.toolbox;

public class FEvent {
    public enum FEventType {
        TAP
    }

    private FDisplayObject owner;
    private FEventType type;
    private Object args;

    public FEvent(FDisplayObject owner0, FEventType type0) {
        this(owner0, type0, null);
    }
    public FEvent(FDisplayObject owner0, FEventType type0, Object args0) {
        owner = owner0;
        type = type0;
        args = args0;
    }

    public FDisplayObject getOwner() {
        return owner;
    }

    public FEventType getType() {
        return type;
    }

    public Object getArgs() {
        return args;
    }

    public static abstract class FEventHandler {
        public abstract void handleEvent(FEvent e);
    }
}
