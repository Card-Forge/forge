package forge.gamemodes.net;

import java.util.concurrent.TimeoutException;

import forge.gamemodes.net.event.IdentifiableNetEvent;
import forge.gamemodes.net.event.NetEvent;

public interface IRemote {
    void send(NetEvent event);
    Object sendAndWait(IdentifiableNetEvent event) throws TimeoutException;
}
