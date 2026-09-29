package forge.gamemodes.net.server;

import forge.gamemodes.match.input.InputSynchronized;
import forge.player.PlayerControllerHuman;

/**
 * The server this process is hosting, if any. Match and input code ask here instead of
 * calling {@link FServerManager#getInstance()}, so a game that never hosts never loads
 * the network stack.
 */
public final class HostingServer {

    @FunctionalInterface
    public interface AfkTimeout {
        AfkTimeout NOOP = () -> {};
        void cancel();
    }

    /** What match and input code need from a running server. */
    public interface Server {
        AfkTimeout armAfkTimeout(PlayerControllerHuman controller, InputSynchronized input);
    }

    private static volatile Server active;

    private HostingServer() {
    }

    static void set(final Server server) {
        active = server;
    }

    public static boolean isHosting() {
        return active != null;
    }

    public static AfkTimeout armAfkTimeout(final PlayerControllerHuman controller, final InputSynchronized input) {
        final Server server = active;
        return server == null ? AfkTimeout.NOOP : server.armAfkTimeout(controller, input);
    }
}
