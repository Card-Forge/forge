package forge.gamemodes.net;

import forge.gamemodes.net.event.GuiGameEvent;

public final class GameProtocolSender {

    private final IRemote remote;
    private final Integer gameViewId;
    public GameProtocolSender(final IRemote remote) {
        this(remote, null);
    }
    public GameProtocolSender(final IRemote remote, final int gameViewId) {
        this(remote, Integer.valueOf(gameViewId));
    }
    private GameProtocolSender(final IRemote remote, final Integer gameViewId) {
        this.remote = remote;
        this.gameViewId = gameViewId;
    }

    private GuiGameEvent event(final ProtocolMethod method, final Object... args) {
        return gameViewId == null ? new GuiGameEvent(method, args)
                : GuiGameEvent.forGameView(method, gameViewId, args);
    }
    private GuiGameEvent event(final ProtocolMethod method, boolean replace, final Object... args) {
        return gameViewId == null ? new GuiGameEvent(method, replace, args)
                : GuiGameEvent.forGameView(method, gameViewId, replace, args);
    }

    public void send(final ProtocolMethod method, final Object... args) {
        method.checkArgs(args);
        remote.send(event(method, args));
    }
    public void send(final ProtocolMethod method, boolean replace, final Object... args) {
        method.checkArgs(args);
        remote.send(event(method, replace, args));
    }

    public void write(final ProtocolMethod method, final Object... args) {
        method.checkArgs(args);
        remote.write(event(method, args));
    }

    @SuppressWarnings("unchecked")
    public <T> T sendAndWait(final ProtocolMethod method, final Object... args) {
        method.checkArgs(args);
        final Object returned = remote.sendAndWait(event(method, args));
        method.checkReturnValue(returned);
        return (T) returned;
    }
}
