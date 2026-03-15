package forge.gamemodes.net;

import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ChatMessage {
    private static final ForgePreferences prefs = FModel.getPreferences();
    private static final SimpleDateFormat inFormat = new SimpleDateFormat("HH:mm:ss");

    public enum MessageType {
        PLAYER,   // Regular player chat message
        SYSTEM    // System notification (displayed in blue)
    }

    private final String source, message, timestamp;
    private final MessageType type;

    // Default constructor - automatically detects system messages (null source)
    public ChatMessage(String source0, String message0) {
        this(source0, message0, source0 == null ? MessageType.SYSTEM : MessageType.PLAYER);
    }

    // Constructor with message type
    public ChatMessage(String source0, String message0, MessageType type0) {
        source = source0;
        message = message0;
        timestamp = inFormat.format(new Date());
        type = type0;
    }

    // Factory method for system messages
    public static ChatMessage createSystemMessage(String message0) {
        return new ChatMessage(null, message0, MessageType.SYSTEM);
    }

    public boolean isLocal() {
        return source == null || source.equals(prefs.getPref(FPref.PLAYER_NAME));
    }

    public boolean isSystemMessage() {
        return type == MessageType.SYSTEM;
    }

    public MessageType getType() {
        return type;
    }

    public String getSource() {
        return source;
    }

    public String getMessage() {
        return message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getFormattedMessage() {
        if (source == null) {
            // System messages show [SERVER] after timestamp
            return String.format("[%s] [SERVER] %s", timestamp, message);
        }
        return String.format("[%s] %s: %s", timestamp, source, message);
    }
}
