package forge.gamemodes.net;

import forge.gamemodes.net.event.IdentifiableNetEvent;
import forge.gamemodes.net.event.NetEvent;

public interface IRemote {
    void send(NetEvent event);
    Object sendAndWait(IdentifiableNetEvent event);
    default void write(NetEvent event) { send(event); }
}
