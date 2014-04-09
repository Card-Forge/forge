package forge.net;

public interface IConnectionObserver {
    /** Notifies that the client is gone, it's too late to send anything */
    public void onConnectionClosed();

    /** Notifies of an incoming message */
    public void onMessage(String data);
}