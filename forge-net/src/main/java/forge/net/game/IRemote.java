package forge.net.game;

import java.util.concurrent.TimeoutException;

public interface IRemote {
    void send(NetEvent event);
    Object sendAndWait(IdentifiableNetEvent event) throws TimeoutException;
}
