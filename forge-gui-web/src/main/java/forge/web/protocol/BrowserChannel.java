package forge.web.protocol;

import com.google.gson.JsonObject;

/** Where a message to the browser goes. Implementations must be safe to call from any thread. */
public interface BrowserChannel {
    void send(JsonObject message);

    /** The WebSocket close code that tells the page not to reconnect until it is shown again. */
    int CLOSED_BY_SERVER = 4000;

    /** Ends the connection, as when the server is full or another tab of the browser took over. */
    default void close() {
    }

    /** Sends one of the messages in {@link ToBrowser}. */
    default void send(final Record message) {
        send(MessageJson.encode(message));
    }
}
