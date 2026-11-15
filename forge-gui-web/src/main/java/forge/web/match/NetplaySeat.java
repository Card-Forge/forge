package forge.web.match;

import com.google.common.primitives.Ints;
import forge.game.Game;
import forge.game.GameEndReason;
import forge.game.player.Player;
import forge.gamemodes.match.GameLobby.GameLobbyData;
import forge.gamemodes.match.HostedMatch;
import forge.gamemodes.match.LobbySlot;
import forge.gamemodes.match.LobbySlotType;
import forge.gamemodes.net.ChatMessage;
import forge.gamemodes.net.NetworkLogConfig;
import forge.gamemodes.net.client.ClientGameLobby;
import forge.gamemodes.net.client.FGameClient;
import forge.gamemodes.net.event.NetEvent;
import forge.gamemodes.net.event.UpdateLobbyPlayerEvent;
import forge.gamemodes.net.server.FServerManager;
import forge.gamemodes.net.server.HostingServer;
import forge.gamemodes.net.server.RemoteClient;
import forge.gamemodes.net.server.RemoteClientGuiGame;
import forge.gamemodes.net.server.ServerGameLobby;
import forge.gui.interfaces.IDraftEventHandler;
import forge.interfaces.ILobbyListener;
import forge.interfaces.IUpdateable;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.player.PlayerControllerHuman;
import forge.util.IHasForgeLog;
import forge.util.Localizer;
import forge.web.lobby.Lobby;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

/**
 * The netplay seat one browser plays from. It is a client of a server inside this process, and that server opens no socket.
 * NetConnectUtil.host is not used because it starts a server on a port and takes a desktop or mobile lobby screen.
 */
public final class NetplaySeat implements IHasForgeLog {
    private static final long JOIN_TIMEOUT_SECONDS = 15;
    /** How long a new table waits for the last table's connections to close. */
    private static final long FREE_SEATS_TIMEOUT_MILLIS = 5_000;
    /** More seats than any table has, so the check covers every connection a table can hold. */
    private static final int MOST_SEATS = 16;

    private final FServerManager server = FServerManager.getInstance();
    // The fields below are set on the host UI thread or a joining task, and read from the web server's and netplay client's threads
    /** The lobby this session owns, or null when another browser is hosting the game. */
    private volatile ServerGameLobby hosted;
    /** Whether this session owns the server as host. The server runs longer than any one game, so the open lobby cannot tell. */
    private volatile boolean startedServer;
    private volatile ClientGameLobby joined;
    private volatile FGameClient client;
    private volatile int webSeat = -1;
    /** Receives the draft and pool events the host sends this seat. Set before a table is opened or joined. */
    private volatile IDraftEventHandler draftHandler;

    public void setDraftHandler(final IDraftEventHandler handler) {
        draftHandler = handler;
    }

    /** Sends an event to the host through this seat's client. A draft pick reaches the draft host this way. */
    public void sendToHost(final NetEvent event) {
        final FGameClient c = client;
        if (c != null) {
            c.send(event);
        }
    }

    public ServerGameLobby hostedLobby() {
        return hosted;
    }

    public ClientGameLobby clientLobby() {
        return joined;
    }

    /** Whether this seat runs the netplay server of the current table. */
    public boolean isHost() {
        return hosted != null;
    }

    /** The browser's seat, or -1 before it has one. */
    public int webSeat() {
        return webSeat;
    }

    /** Opens a table this machine hosts and takes a seat at it. Every other seat gets an AI. */
    public void openHost(final String playerName, final WebGuiGame gui, final Runnable onUpdate,
            final BiConsumer<String, String> onChat) {
        endMatch();
        awaitOldSeatsFreed();
        // The server costs nothing to leave running, so it stays up between games
        if (!HostingServer.isHosting()) {
            NetworkLogConfig.activateNetworkLogging();
            server.startLoopbackServer();
        }
        startedServer = true;
        openHosted(playerName, gui, onUpdate, onChat);
        // The browser took the first open seat. The host fills the other seats
        for (int i = 0; i < hosted.getNumberOfSlots(); i++) {
            if (i != webSeat) {
                final LobbySlot slot = hosted.getSlot(i);
                slot.setType(LobbySlotType.AI);
                slot.setName(hosted.randomName());
                slot.setIsReady(true);
            }
        }
        // The client took its seat before these seats were filled, so its copy of the table is out of date
        pushLobby();
    }

    private void openHosted(final String playerName, final WebGuiGame gui, final Runnable onUpdate,
            final BiConsumer<String, String> onChat) {
        hosted = new ServerGameLobby();
        server.setLobby(hosted);
        // The host's seat opens each game of the match, so every game is watched
        gui.whenOpened(() -> {
            final HostedMatch match = hostedMatch();
            DrawPrefetch.watch(match == null ? null : match.getGame());
        });
        // Slot 0 starts as the host's local seat. The browser plays through a client, so the slot is set to open
        hosted.getSlot(0).setType(LobbySlotType.OPEN);
        hosted.setListener(new IUpdateable() {
            @Override public void update(final boolean fullUpdate) {
                pushLobby();
                onUpdate.run();
            }
            @Override public void update(final int slot, final LobbySlotType type) { }
        });
        server.setLobbyListener(new HostChat(onChat));
        // The host gets chat through its own listener. A client listener would only repeat each message
        connect(playerName, gui, onUpdate, (from, text) -> { }, () -> { });
    }

    /** Waits for the last table's connections to close. A seat that closes late is freed in whichever lobby is current at that time. */
    private void awaitOldSeatsFreed() {
        if (!HostingServer.isHosting()) {
            return;
        }
        final long giveUp = System.currentTimeMillis() + FREE_SEATS_TIMEOUT_MILLIS;
        while (anySeatConnected()) {
            if (System.currentTimeMillis() > giveUp) {
                netLog.warn("A connection from the last table is still open; setting up the new one anyway.");
                return;
            }
            try {
                Thread.sleep(10);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private boolean anySeatConnected() {
        for (int i = 0; i < MOST_SEATS; i++) {
            if (server.findClientByIndex(i) != null) {
                return true;
            }
        }
        return false;
    }

    /** Takes a seat in a game another browser on this machine is hosting. Nothing is served from here. */
    public void openGuest(final String playerName, final WebGuiGame gui,
            final Runnable onUpdate, final BiConsumer<String, String> onChat, final Runnable onClosed) {
        endMatch();
        connect(playerName, gui, onUpdate, onChat, onClosed);
    }

    /** Every seat, including the host's, connects to the game as a netplay client. */
    private void connect(final String playerName, final WebGuiGame gui,
            final Runnable onUpdate, final BiConsumer<String, String> onChat, final Runnable onClosed) {
        joined = new ClientGameLobby();
        // AbstractGuiGame.getDeckForPlayer reads the client lobby
        gui.setClientLobby(joined);
        final CountDownLatch ready = new CountDownLatch(1);
        client = new FGameClient(playerName, gui, FServerManager.LOOPBACK);
        client.setDispatchExecutor(gui.dispatchExecutor());
        client.setDraftHandler(draftHandler);
        client.addLobbyListener(new ClientListener(joined, ready, onChat, onClosed, seat -> {
            webSeat = seat;
            onUpdate.run();
        }));
        client.connect();
        try {
            if (!ready.await(JOIN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                // A refused client keeps reconnecting to the one loopback address and would take a seat at the next table
                endMatch();
                throw new IllegalStateException(Localizer.getInstance().getMessage("lblWebLocalGameNoFreeSeat"));
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            endMatch();
            throw new IllegalStateException("Interrupted while joining", e);
        }
    }

    /** Sends the table to every client. A slot edited directly on the server does not send an update by itself. */
    public void pushLobby() {
        if (hosted != null) {
            Lobby.distinctAvatars(hosted);
            server.updateLobbyState();
        }
    }

    /** Sends a change to the browser's own seat the same way any client would. */
    public void updateOwnSeat(final UpdateLobbyPlayerEvent event) {
        if (client != null) {
            client.send(event);
        }
    }

    /** Prepares the match and returns the task that starts it, or null when it cannot start. Only the host can do this. */
    public Runnable prepare() {
        if (hosted == null) {
            throw new IllegalStateException(Localizer.getInstance().getMessage("lblWebSessionHostStarts"));
        }
        return hosted.startGame();
    }

    /** Opens a table where this browser is the only person, at seat webSeat. A game mode then starts a match it built itself on this table. */
    public ServerGameLobby openAlone(final String playerName, final WebGuiGame gui) {
        nameProfile(playerName);
        openHost(playerName, gui, () -> { }, (from, text) -> { });
        return hosted;
    }

    /** Saves the player's name if the profile has none. Otherwise HostedMatch would ask for a name on the host's screen. */
    private static void nameProfile(final String name) {
        if (StringUtils.isBlank(FModel.getPreferences().getPref(FPref.PLAYER_NAME))) {
            FModel.getPreferences().setPref(FPref.PLAYER_NAME, name);
            FModel.getPreferences().save();
        }
    }

    /** The saved avatar or sleeve for a lobby seat. If none is saved, it is the seat number, as in the desktop lobby. */
    public static int storedIndex(final FPref pref, final int seat) {
        final String[] stored = FModel.getPreferences().getPref(pref).split(",");
        final Integer v = seat < stored.length ? Ints.tryParse(stored[seat].trim()) : null;
        return v == null || v < 0 ? seat : v;
    }

    /** Gives the web seat to an AI so the browser spectates. Only the host can do this, because the seat belongs to its server. */
    public void spectate() {
        if (hosted == null) {
            netLog.warn("Only the host can hand its seat to the AI");
            return;
        }
        final RemoteClient seat = server.getClientBySlotIndex(webSeat);
        if (seat == null) {
            netLog.warn("No web seat to hand to the AI");
            return;
        }
        server.convertToAI(seat);
    }

    /** Gives another seat of the running match to an AI. Only the host can do this. Outside a match it does nothing. */
    public void handToAi(final int seat) {
        if (hosted != null) {
            server.replaceWithAI(seat);
        }
    }

    public HostedMatch hostedMatch() {
        return hosted == null ? null : hosted.getHostedMatch();
    }

    /** The host's own player in the running game. Null if this is not the host, no game runs, or the seat was given to the AI. */
    public Player ownPlayer() {
        final HostedMatch match = hostedMatch();
        final RemoteClient seat = hosted == null ? null : server.getClientBySlotIndex(webSeat);
        if (match == null || match.getGame() == null || seat == null) {
            return null;
        }
        for (final Player p : match.getGame().getPlayers()) {
            if (p.getController() instanceof PlayerControllerHuman human && human.getGui() instanceof RemoteClientGuiGame gui
                    && gui.getClient() == seat) {
                return p;
            }
        }
        return null;
    }

    public void endMatch() {
        abandonGame();
        if (hosted != null) {
            // A draft still running keeps its timers. It would deal packs to the players at the next table
            hosted.clearCurrentEvent();
            // Late disconnects go to an empty lobby. Otherwise this table's seats would be held for a reconnect at the next table
            server.setLobby(new ServerGameLobby());
        }
        if (client != null) {
            client.close();
            client = null;
        }
        // The GUIs of all seats are held on the one server, so only the host may clear them
        if (hosted != null) {
            server.clearPlayerGuis();
        }
        hosted = null;
        joined = null;
        webSeat = -1;
    }

    /** Ends a game that is still running at a closing table. Otherwise its thread would wait forever for players who have left. */
    private void abandonGame() {
        final HostedMatch match = hostedMatch();
        final Game game = match == null ? null : match.getGame();
        if (game == null || game.isGameOver()) {
            return;
        }
        // Ending the game clears the players' controllers, so the humans are found first
        final List<PlayerControllerHuman> humans = new ArrayList<>();
        for (final Player p : game.getRegisteredPlayers()) {
            if (p.getController() instanceof PlayerControllerHuman human) {
                humans.add(human);
            }
        }
        game.getAction().invoke(() -> {
            game.setGameOver(GameEndReason.Draw);
            for (final PlayerControllerHuman human : humans) {
                human.getInputQueue().onGameOver(true);
            }
        });
    }

    /** Gives up ownership of the server, so a later shutdown of this seat leaves it running for the next host. */
    public void disownServer() {
        startedServer = false;
    }

    /** Leaves the seat permanently. Only the host stops the server. */
    public void shutdown() {
        endMatch();
        if (startedServer) {
            server.stopServer();
            startedServer = false;
        }
    }

    private record ClientListener(ClientGameLobby clientLobby, CountDownLatch ready, BiConsumer<String, String> onChat,
            Runnable onClosed, IntConsumer onSeat) implements ILobbyListener {
        @Override public void message(final String source, final String message, final ChatMessage.MessageType type) {
            onChat.accept(source, message);
        }
        @Override public void update(final GameLobbyData state, final int slot) {
            clientLobby.setData(state);
            clientLobby.setLocalPlayer(slot);
            if (slot >= 0) {
                onSeat.accept(slot);
                ready.countDown();
            }
        }
        @Override public void close() { onClosed.run(); }
        @Override public ClientGameLobby getLobby() { return clientLobby; }
    }

    /** The host gets chat through this listener. A joined client gets chat through the client's listener. */
    private record HostChat(BiConsumer<String, String> onChat) implements ILobbyListener {
        @Override public void message(final String source, final String message, final ChatMessage.MessageType type) {
            onChat.accept(source, message);
        }
        @Override public void update(final GameLobbyData state, final int slot) { }
        @Override public void close() { }
        @Override public ClientGameLobby getLobby() { return null; }
    }
}
