package forge.gamemodes.net.event;

import forge.gamemodes.net.ChatMessage;
import forge.gamemodes.net.server.RemoteClient;

public final class MessageEvent implements NetEvent {
    private static final long serialVersionUID = 1700060210647684186L;

    private final String source, message;
    // String not enum — unknown enum constants cause InvalidObjectException on older clients
    private final String type;

    public MessageEvent(final String message) {
        this(null, message, (String) null);
    }
    public MessageEvent(final String source, final String message) {
        this(source, message, (String) null);
    }
    public MessageEvent(final String source, final String message, final ChatMessage.MessageType type) {
        this(source, message, type != null ? type.name() : null);
    }
    private MessageEvent(final String source, final String message, final String type) {
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
        if (type == null) {
            return source == null ? ChatMessage.MessageType.SYSTEM : ChatMessage.MessageType.PLAYER;
        }
        try {
            return ChatMessage.MessageType.valueOf(type);
        } catch (IllegalArgumentException e) {
            return ChatMessage.MessageType.SYSTEM;
        }
    }

    @Override
    public String toString() {
        return getMessage();
    }
}
