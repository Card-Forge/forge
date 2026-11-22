package forge.gamemodes.net.client;

import forge.gamemodes.net.ProtocolMethod;
import forge.gamemodes.net.event.GuiGameEvent;
import forge.gamemodes.net.event.IdentifiableNetEvent;
import forge.gamemodes.net.event.NetEvent;
import forge.gui.interfaces.IGuiGame;

/**
 * The "server connection" of a replay. The real client handler talks to it as it would to a network client, but
 * nothing leaves: what the viewer presses is either a playback control, which {@link ReplayPlayer} acts on, or
 * something that has no meaning in a recording and is dropped.
 */
final class ReplayClient extends FGameClient {
    private final ReplayPlayer player;

    ReplayClient(final String title, final IGuiGame gui, final ReplayPlayer player0) {
        super(title, gui, "replay", 0);
        player = player0;
    }

    @Override
    public void send(final NetEvent event) {
        if (event instanceof GuiGameEvent action) {
            final ProtocolMethod method = action.getMethod();
            if (method == ProtocolMethod.selectButtonOk || method == ProtocolMethod.selectButtonCancel
                    || method == ProtocolMethod.concede || method == ProtocolMethod.nextGameDecision) {
                player.onControl(method);
            }
        }
    }

    /** Nothing in a recording waits for an answer, so no one is asked. */
    @Override
    public Object sendAndWait(final IdentifiableNetEvent event) {
        send(event);
        return null;
    }

    @Override
    public void close() {
        beginShutdown();
    }
}
