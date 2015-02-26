package forge.net.game;

public class MessageEvent implements NetEvent {
    private static final long serialVersionUID = 1700060210647684186L;

    private final String source, message;
    public MessageEvent(final String source, final String message) {
        this.source = source;
        this.message = message;
    }

    public String getSource() {
        return source;
    }
    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return getMessage();
    }
}
