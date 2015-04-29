package forge.net;

import java.util.concurrent.TimeoutException;

import forge.net.event.GuiGameEvent;

public final class GameProtocolSender {

    private final IRemote remote;
    public GameProtocolSender(final IRemote remote) {
        this.remote = remote;
    }

    public void send(final ProtocolMethod method, final Object... args) {
        method.checkArgs(args);
        remote.send(new GuiGameEvent(method, args));
    }

    @SuppressWarnings("unchecked")
    public <T> T sendAndWait(final ProtocolMethod method, final Object... args) {
        method.checkArgs(args);
        try {
            final Object returned = remote.sendAndWait(new GuiGameEvent(method, args));
            method.checkReturnValue(returned);
            return (T) returned;
        } catch (final TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }
}
