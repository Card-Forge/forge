package forge.net;

import java.text.SimpleDateFormat;
import java.util.Date;

import forge.model.FModel;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;

public class ChatMessage {
    private static final ForgePreferences prefs = FModel.getPreferences();
    private static final SimpleDateFormat inFormat = new SimpleDateFormat("HH:mm:ss");

    private final String source, message, timestamp;

    public ChatMessage(String source0, String message0) {
        source = source0;
        message = message0;
        timestamp = inFormat.format(new Date());
    }

    public boolean isLocal() {
        return source == null || source.equals(prefs.getPref(FPref.PLAYER_NAME));
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
            return String.format("[%s] %s", timestamp, message);
        }
        return String.format("[%s] %s: %s", timestamp, source, message);
    }
}
