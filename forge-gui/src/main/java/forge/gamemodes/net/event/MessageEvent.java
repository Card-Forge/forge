package forge.gamemodes.net.event;

import forge.gamemodes.net.ChatMessage;
import forge.gamemodes.net.server.RemoteClient;

public final class MessageEvent implements NetEvent {
    private static final long serialVersionUID = 1700060210647684186L;

    private final String source, message;
    // Null for backwards compat with pre-WARNING-tag senders; getType() normalizes.
    private final ChatMessage.MessageType type;

    public MessageEvent(final String message) {
        this(null, message, null);
    }
    public MessageEvent(final String source, final String message) {
        this(source, message, null);
    }
    public MessageEvent(final String source, final String message, final ChatMessage.MessageType type) {
        this.source = source;
        this.message = message;
        this.type = type;
    }

    public static MessageEvent warning(final String message) {
        return new MessageEvent(null, message, ChatMessage.MessageType.WARNING);
    }

    @Override
    public void updateForClient(final RemoteClient client) {
    }

    public String getSource() {
        return source;
    }

    public String getMessage() {
        return message;
    }

    public ChatMessage.MessageType getType() {
        if (type != null) return type;
        return source == null ? ChatMessage.MessageType.SYSTEM : ChatMessage.MessageType.PLAYER;
    }

    @Override
    public String toString() {
        return getMessage();
    }
}
