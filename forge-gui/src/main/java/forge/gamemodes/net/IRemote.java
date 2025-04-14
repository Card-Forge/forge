package forge.gamemodes.net;

import forge.gamemodes.net.event.IdentifiableNetEvent;
import forge.gamemodes.net.event.NetEvent;

import java.util.concurrent.TimeoutException;

public interface IRemote {
    void send(NetEvent event);
    Object sendAndWait(IdentifiableNetEvent event) throws TimeoutException;
}
