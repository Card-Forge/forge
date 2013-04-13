package forge.net;

public interface IClientSocket {
    boolean isOpen();
    void send(String message);
    void close(String farewell);
}