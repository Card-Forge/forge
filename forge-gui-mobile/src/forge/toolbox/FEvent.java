package forge.toolbox;

public class FEvent {
    public enum FEventType {
        TAP,
        LONG_PRESS,
        CHANGE,
        ACTIVATE,
        SAVE,
        DELETE,
        CLOSE
    }

    private FDisplayObject source;
    private FEventType type;
    private Object args;

    public FEvent(FDisplayObject source0, FEventType type0) {
        this(source0, type0, null);
    }
    public FEvent(FDisplayObject source0, FEventType type0, Object args0) {
        source = source0;
        type = type0;
        args = args0;
    }

    public FDisplayObject getSource() {
        return source;
    }

    public FEventType getType() {
        return type;
    }

    public Object getArgs() {
        return args;
    }

    public static interface FEventHandler {
        void handleEvent(FEvent e);
    }
}
