package forge.gamemodes.net;

import forge.gamemodes.net.event.GuiGameEvent;

public final class GameProtocolSender {

    private final IRemote remote;
    public GameProtocolSender(final IRemote remote) {
        this.remote = remote;
    }

    public void send(final ProtocolMethod method, final Object... args) {
        method.checkArgs(args);
        remote.send(new GuiGameEvent(method, args));
    }

    public void write(final ProtocolMethod method, final Object... args) {
        method.checkArgs(args);
        remote.write(new GuiGameEvent(method, args));
    }

    @SuppressWarnings("unchecked")
    public <T> T sendAndWait(final ProtocolMethod method, final Object... args) {
        method.checkArgs(args);
        final Object returned = remote.sendAndWait(new GuiGameEvent(method, args));
        method.checkReturnValue(returned);
        return (T) returned;
    }
}
