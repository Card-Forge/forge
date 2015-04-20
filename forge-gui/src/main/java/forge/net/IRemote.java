package forge.net;

import java.util.concurrent.TimeoutException;

import forge.net.event.IdentifiableNetEvent;
import forge.net.event.NetEvent;

public interface IRemote {
    void send(NetEvent event);
    Object sendAndWait(IdentifiableNetEvent event) throws TimeoutException;
}
