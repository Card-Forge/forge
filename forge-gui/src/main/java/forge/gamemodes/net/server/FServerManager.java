package forge.gamemodes.net.server;

import com.google.common.collect.ImmutableList;
import forge.ai.LobbyPlayerAi;
import forge.ai.PlayerControllerAi;
import forge.game.Game;
import forge.game.player.Player;
import forge.gamemodes.match.HostedMatch;
import forge.gamemodes.match.LobbySlot;
import forge.gamemodes.match.LobbySlotType;
import forge.gamemodes.net.CompatibleObjectDecoder;
import forge.gamemodes.net.CompatibleObjectEncoder;
import forge.gamemodes.net.event.*;
import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiGame;
import forge.gui.util.SOptionPane;
import forge.interfaces.IGameController;
import forge.interfaces.ILobbyListener;
import forge.model.FModel;
import forge.player.PlayerControllerHuman;
import forge.util.BuildInfo;
import forge.util.IterableUtil;
import forge.util.Localizer;
import forge.localinstance.properties.ForgeNetPreferences;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import org.jupnp.UpnpService;
import org.jupnp.UpnpServiceImpl;
import org.jupnp.support.igd.PortMappingListener;
import org.jupnp.support.model.PortMapping;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public final class FServerManager {
    private static final int RECONNECT_TIMEOUT_SECONDS = 300;

    private static FServerManager instance = null;
    private final Map<Channel, RemoteClient> clients = new ConcurrentHashMap<>();
    private final Map<String, RemoteClient> disconnectedClients = new ConcurrentHashMap<>();
    private final Map<String, Timer> reconnectTimers = new ConcurrentHashMap<>();
    private boolean isHosting = false;
    private EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private EventLoopGroup workerGroup = new NioEventLoopGroup();
    private UpnpService upnpService = null;
    private ServerGameLobby localLobby;
    private ILobbyListener lobbyListener;
    private boolean UPnPMapped = false;
    private int port;
    private static final Localizer localizer = Localizer.getInstance();
    private final Thread shutdownHook = new Thread(() -> {
        if (isHosting()) {
            stopServer(false);
        }
    });

    private FServerManager() {
    }


    RemoteClient getClient(final Channel ch) {
        return clients.get(ch);
    }

    IGameController getController(final int index) {
        return localLobby.getController(index);
    }

    /**
     * Get the singleton instance of {@link FServerManager}.
     *
     * @return the singleton FServerManager.
     */
    public static FServerManager getInstance() {
        if (instance == null) {
            instance = new FServerManager();
        }
        return instance;
    }

    public void startServer(final int port) {
        this.port = port;
        String UPnPOption = FModel.getNetPreferences().getPref(ForgeNetPreferences.FNetPref.UPnP);
        boolean startUPnP;
        if(UPnPOption.equalsIgnoreCase("ASK")) {
            startUPnP = callUPnPDialog();
        } else {
            startUPnP = UPnPOption.equalsIgnoreCase("ALWAYS");
        }
        System.out.println("Starting Multiplayer Server");
        try {
            final ServerBootstrap b = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(final SocketChannel ch) throws Exception {
                            final ChannelPipeline p = ch.pipeline();
                            p.addLast(
                                    new CompatibleObjectEncoder(),
                                    new CompatibleObjectDecoder(9766 * 1024, ClassResolvers.cacheDisabled(null)),
                                    new MessageHandler(),
                                    new RegisterClientHandler(),
                                    new LobbyInputHandler(),
                                    new DeregisterClientHandler(),
                                    new GameServerHandler());
                        }
                    });

            // Bind and start to accept incoming connections.
            final ChannelFuture ch = b.bind(port).sync().channel().closeFuture();
            new Thread(() -> {
                try {
                    ch.sync();
                } catch (final InterruptedException e) {
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                } finally {
                    stopServer();
                }
            }).start();
            if(startUPnP) {
                mapNatPort();
            }
            Runtime.getRuntime().addShutdownHook(shutdownHook);
            isHosting = true;
        } catch (final InterruptedException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean callUPnPDialog() {
        switch (SOptionPane.showOptionDialog(localizer.getMessageorUseDefault("lblUPnPQuestion", String.format("Attempt to open port %d automatically?", port), port),
                localizer.getMessageorUseDefault("lblUPnPTitle", "UPnP option"),
                null,
                ImmutableList.of(localizer.getMessageorUseDefault("lblJustOnce", "Just Once"),
                        localizer.getMessageorUseDefault("lblNotNow", "Not Now"),
                        localizer.getMessageorUseDefault("lblAlways", "Always"),
                        localizer.getMessageorUseDefault("lblNever", "Never")), 0)) {
            case 2:
                FModel.getNetPreferences().setPref(ForgeNetPreferences.FNetPref.UPnP, "ALWAYS");
                FModel.getNetPreferences().save();
            case 0:
                //case 2 falls in here
                return true;
            case 3:
                FModel.getNetPreferences().setPref(ForgeNetPreferences.FNetPref.UPnP, "NEVER");
                FModel.getNetPreferences().save();
            default:
                //case 1 defaults to here
                return false;
        }
    }

    public void stopServer() {
        stopServer(true);
    }

    private void stopServer(final boolean removeShutdownHook) {
        // Cancel all reconnect timers
        for (final Timer timer : reconnectTimers.values()) {
            timer.cancel();
        }
        reconnectTimers.clear();
        disconnectedClients.clear();

        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        if (upnpService != null) {
            upnpService.shutdown();
            upnpService = null;
        }
        if (removeShutdownHook) {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        }
        isHosting = false;
        // create new EventLoopGroups for potential restart
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
    }

    public boolean isHosting() {
        return isHosting;
    }

    public void broadcast(final NetEvent event) {
        if (event instanceof MessageEvent msgEvent) {
            lobbyListener.message(msgEvent.getSource(), msgEvent.getMessage());
        }
        broadcastTo(event, clients.values());
    }

    public void broadcastExcept(final NetEvent event, final RemoteClient notTo) {
        broadcastExcept(event, Collections.singleton(notTo));
    }

    public void broadcastExcept(final NetEvent event, final Collection<RemoteClient> notTo) {
        Predicate<RemoteClient> filter = Predicate.not(notTo::contains);
        broadcastTo(event, IterableUtil.filter(clients.values(), filter));
    }

    private void broadcastTo(final NetEvent event, final Iterable<RemoteClient> to) {
        for (final RemoteClient client : to) {
            broadcastTo(event, client);
        }
    }

    private void broadcastTo(final NetEvent event, final RemoteClient to) {
        event.updateForClient(to);
        to.send(event);
    }

    public void setLobby(final ServerGameLobby lobby) {
        this.localLobby = lobby;
    }

    public void unsetReady() {
        if (this.localLobby != null && this.localLobby.getSlot(0) != null) {
                this.localLobby.getSlot(0).setIsReady(false);
                updateLobbyState();
        }
    }

    public boolean isMatchActive() {
        return this.localLobby != null && this.localLobby.isMatchActive();
    }

    public void setLobbyListener(final ILobbyListener listener) {
        this.lobbyListener = listener;
    }

    public void updateLobbyState() {
        final LobbyUpdateEvent event = new LobbyUpdateEvent(localLobby.getData());
        broadcast(event);
    }

    public void updateSlot(final int index, final UpdateLobbyPlayerEvent event) {
        localLobby.applyToSlot(index, event);

        if (event.getReady() != null) {
            broadcastReadyState(localLobby.getSlot(index).getName(), event.getReady());
        }
    }

    private void broadcastReadyState(String playerName, boolean isReady) {
        int readyCount = 0;
        int totalPlayers = 0;
        for (int i = 0; i < localLobby.getNumberOfSlots(); i++) {
            LobbySlot slot = localLobby.getSlot(i);
            if (slot.getType() == LobbySlotType.LOCAL || slot.getType() == LobbySlotType.REMOTE) {
                totalPlayers++;
                if (slot.isReady()) {
                    readyCount++;
                }
            }
        }
        if (isReady) {
            broadcast(new MessageEvent(String.format("%s is ready (%d/%d players ready)",
                playerName, readyCount, totalPlayers)));
            if (readyCount == totalPlayers && totalPlayers > 1) {
                broadcast(new MessageEvent("All players ready to start game!"));
            }
        } else {
            broadcast(new MessageEvent(String.format("%s is not ready (%d/%d players ready)",
                playerName, readyCount, totalPlayers)));
        }
    }

    public IGuiGame getGui(final int index) {
        final LobbySlot slot = localLobby.getSlot(index);
        final LobbySlotType type = slot.getType();
        if (type == LobbySlotType.LOCAL) {
            final IGuiGame gui = GuiBase.getInterface().getNewGuiGame();
            gui.setNetGame();
            return gui;
        } else if (type == LobbySlotType.REMOTE) {
            for (final RemoteClient client : clients.values()) {
                if (client.getIndex() == index) {
                    return new NetGuiGame(client, index);
                }
            }
        }
        return null;
    }

    // inspired by:
    //  https://stackoverflow.com/a/34873630
    //  https://stackoverflow.com/a/901943
    private static String getRoutableAddress(boolean preferIpv4, boolean preferIPv6) throws SocketException, UnknownHostException {
        try (DatagramSocket socket = new DatagramSocket()) {
            // Connect to a well-known external address (Google's public DNS server)
            socket.connect(InetAddress.getByName("8.8.8.8"), 12345); // Use a valid port instead of 0
            InetAddress localAddress = socket.getLocalAddress();

            // Check if the local address belongs to a valid network interface
            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(localAddress);
            if (networkInterface != null && networkInterface.isUp()) {
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && preferIpv4) {
                        return addr.getHostAddress();
                    }
                    if (addr instanceof Inet6Address && preferIPv6) {
                        return addr.getHostAddress();
                    }
                }
            }
        }
        return "localhost";
    }

    public static String getLocalAddress() {
        try {
            return getRoutableAddress(true, false);
        } catch (final Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            return "localhost";
        }
    }

    public static String getExternalAddress() {
        BufferedReader in = null;
        try {
            URL whatismyip = new URL("https://checkip.amazonaws.com");
            in = new BufferedReader(new InputStreamReader(
                    whatismyip.openStream()));
            return in.readLine();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private void mapNatPort() {
        final String localAddress = getLocalAddress();
        final PortMapping portMapping = new PortMapping(port, localAddress, PortMapping.Protocol.TCP, "Forge");
        // Shutdown existing UPnP service if already running
        if (upnpService != null) {
            upnpService.shutdown();
        }

        try {
            // Create a new UPnP service instance
            upnpService = new UpnpServiceImpl(GuiBase.getInterface().getUpnpPlatformService());
            upnpService.startup();

            // Add a PortMappingListener
            upnpService.getRegistry().addListener(new PortMappingListener(portMapping));
            // Trigger device discovery
            upnpService.getControlPoint().search();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    // --- Reconnection helper methods ---

    public boolean handleCommand(final String messageText) {
        if (messageText == null || !messageText.startsWith("/")) { return false; }
        final String trimmed = messageText.trim();
        final String lower = trimmed.toLowerCase();
        if (lower.equals("/skipreconnect") || lower.startsWith("/skipreconnect ")) {
            return handleSkipReconnectCommand(trimmed);
        } else if (lower.equals("/skiptimeout") || lower.startsWith("/skiptimeout ")) {
            return handleSkipTimeoutCommand(trimmed);
        }
        return false;
    }

    private boolean handleSkipReconnectCommand(final String command) {
        final String target = resolveDisconnectedTarget(command, "/skipreconnect");
        if (target == null) { return true; } // error already broadcast
        final RemoteClient client = disconnectedClients.remove(target);
        final Timer timer = reconnectTimers.remove(target);
        if (timer != null) { timer.cancel(); }
        if (isMatchActive()) {
            convertToAI(client.getIndex(), target);
        }
        localLobby.disconnectPlayer(client.getIndex());
        broadcast(new MessageEvent(String.format("Host forced AI takeover for %s.", target)));
        return true;
    }

    private boolean handleSkipTimeoutCommand(final String command) {
        final String target = resolveDisconnectedTarget(command, "/skiptimeout");
        if (target == null) { return true; }
        final Timer timer = reconnectTimers.remove(target);
        if (timer != null) { timer.cancel(); }
        broadcast(new MessageEvent(
            String.format("Timeout disabled for %s. Waiting indefinitely for reconnect.", target)));
        return true;
    }

    private String resolveDisconnectedTarget(final String command, final String prefix) {
        if (disconnectedClients.isEmpty()) {
            broadcast(new MessageEvent("No players are currently disconnected."));
            return null;
        }
        final String arg = command.length() > prefix.length()
            ? command.substring(prefix.length()).trim() : "";
        if (arg.isEmpty()) {
            if (disconnectedClients.size() == 1) {
                return disconnectedClients.keySet().iterator().next();
            }
            broadcast(new MessageEvent(
                String.format("Multiple disconnected players. Specify a name: %s <name>", prefix)));
            return null;
        }
        if (!disconnectedClients.containsKey(arg)) {
            broadcast(new MessageEvent(String.format("No disconnected player named '%s'.", arg)));
            return null;
        }
        return arg;
    }

    private static String formatTime(final int totalSeconds) {
        return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
    }

    private PlayerControllerHuman findRemoteController(final int slotIndex) {
        final IGameController controller = localLobby.getController(slotIndex);
        if (controller instanceof PlayerControllerHuman) {
            return (PlayerControllerHuman) controller;
        }
        return null;
    }

    private void pauseNetGuiGame(final int slotIndex) {
        final HostedMatch hostedMatch = localLobby.getHostedMatch();
        if (hostedMatch == null) { return; }
        final Game game = hostedMatch.getGame();
        if (game == null) { return; }

        for (final Player p : game.getPlayers()) {
            final IGuiGame gui = hostedMatch.getGuiForPlayer(p);
            if (gui instanceof NetGuiGame && ((NetGuiGame) gui).getSlotIndex() == slotIndex) {
                ((NetGuiGame) gui).pause();
                return;
            }
        }
    }

    private void resumeAndResync(final RemoteClient client) {
        final int slotIndex = client.getIndex();
        final HostedMatch hostedMatch = localLobby.getHostedMatch();
        if (hostedMatch == null) { return; }
        final Game game = hostedMatch.getGame();
        if (game == null) { return; }

        // Match by slot index — player names may be deduped by the game engine
        // so name matching is unreliable
        for (final Player p : game.getPlayers()) {
            final IGuiGame gui = hostedMatch.getGuiForPlayer(p);
            if (gui instanceof NetGuiGame && ((NetGuiGame) gui).getSlotIndex() == slotIndex) {
                final NetGuiGame netGui = (NetGuiGame) gui;
                netGui.resume();

                // Send current GameView before openView (matches HostedMatch.startGame() ordering)
                netGui.updateGameView();

                // Send full game state to the reconnected client
                netGui.openView(new forge.trackable.TrackableCollection<>(netGui.getLocalPlayers()));

                // Replay current prompt
                final PlayerControllerHuman pch = findRemoteController(slotIndex);
                if (pch != null) {
                    pch.getInputQueue().updateObservers();
                }
                return;
            }
        }
    }

    private void handleReconnectTimeout(final String username) {
        reconnectTimers.remove(username);
        final RemoteClient client = disconnectedClients.remove(username);
        if (client == null) { return; }

        // If match already ended, just clean up
        if (!isMatchActive()) {
            localLobby.disconnectPlayer(client.getIndex());
            return;
        }

        System.out.println("Reconnect timeout for " + username + ". Converting to AI.");
        convertToAI(client.getIndex(), username);

        // Reset lobby slot
        localLobby.disconnectPlayer(client.getIndex());

        broadcast(new MessageEvent(String.format("%s did not reconnect in time. AI has taken over.", username)));
    }

    private void convertToAI(final int slotIndex, final String username) {
        final HostedMatch hostedMatch = localLobby.getHostedMatch();
        if (hostedMatch == null) { return; }
        final Game game = hostedMatch.getGame();
        if (game == null) { return; }

        for (final Player p : game.getPlayers()) {
            final IGuiGame gui = hostedMatch.getGuiForPlayer(p);
            if (gui instanceof NetGuiGame && ((NetGuiGame) gui).getSlotIndex() == slotIndex) {
                final LobbyPlayerAi aiLobbyPlayer = new LobbyPlayerAi(p.getName(), null);
                final PlayerControllerAi aiCtrl = new PlayerControllerAi(game, p, aiLobbyPlayer);
                p.dangerouslySetController(aiCtrl);

                // Clear InputQueue to unblock the game thread (waiting on cdlDone)
                final PlayerControllerHuman pch = findRemoteController(slotIndex);
                if (pch != null) {
                    pch.getInputQueue().clearInputs();
                }
                return;
            }
        }
    }

    private class MessageHandler extends ChannelInboundHandlerAdapter {
        @Override
        public final void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
            if (msg instanceof MessageEvent) {
                final String text = ((MessageEvent) msg).getMessage();
                if (text != null && text.startsWith("/")) {
                    return; // Suppress slash commands from remote clients
                }
                final RemoteClient client = clients.get(ctx.channel());
                String username = client.getUsername();
                // Append (Host) indicator for the host player
                if (client.getIndex() == 0) {
                    username = username + " (Host)";
                }
                broadcast(new MessageEvent(username, text));
            }
            super.channelRead(ctx, msg);
        }
    }

    private class RegisterClientHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelActive(final ChannelHandlerContext ctx) throws Exception {
            final RemoteClient client = new RemoteClient(ctx.channel());
            clients.put(ctx.channel(), client);
            System.out.println("Client connected to server at " + ctx.channel().remoteAddress());
            updateLobbyState();
            super.channelActive(ctx);
        }

        @Override
        public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
            final RemoteClient client = clients.get(ctx.channel());
            if (msg instanceof LoginEvent) {
                // Login is handled by LobbyInputHandler; join message
                // is broadcast there after name deduplication.
            } else if (msg instanceof UpdateLobbyPlayerEvent event) {
                localLobby.applyToSlot(client.getIndex(), event);
                if (event.getName() != null) {
                    String oldName = client.getUsername();
                    String newName = event.getName();
                    if (!newName.equals(oldName)) {
                        client.setUsername(newName);
                        broadcast(new MessageEvent(String.format("%s changed their name to %s", oldName, newName)));
                    }
                }
                if (event.getReady() != null) {
                    broadcastReadyState(client.getUsername(), event.getReady());
                }
                // Return to prevent duplicate processing by LobbyInputHandler
                return;
            }
            super.channelRead(ctx, msg);
        }
    }

    private class LobbyInputHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
            final RemoteClient client = clients.get(ctx.channel());
            if (msg instanceof LoginEvent event) {
                final String username = event.getUsername();

                // Check if this is a reconnecting player
                final RemoteClient disconnected = disconnectedClients.remove(username);
                if (disconnected != null) {
                    // Cancel timeout timer
                    final Timer timer = reconnectTimers.remove(username);
                    if (timer != null) {
                        timer.cancel();
                    }

                    // Remove the temporary client entry for the new channel
                    clients.remove(ctx.channel());

                    // Swap channel on the original RemoteClient
                    disconnected.swapChannel(ctx.channel());

                    // Re-register under the new channel
                    clients.put(ctx.channel(), disconnected);

                    // Resume and resync
                    resumeAndResync(disconnected);

                    broadcast(new MessageEvent(String.format("%s has reconnected.", username)));
                    System.out.println("Player reconnected: " + username);
                } else {
                    // Normal login flow
                    final int index = localLobby.connectPlayer(event.getUsername(), event.getAvatarIndex(), event.getSleeveIndex());
                    if (index == -1) {
                        ctx.close();
                    } else {
                        client.setIndex(index);
                        // Update client username to match deduplicated slot name
                        final String slotName = localLobby.getSlot(index).getName();
                        client.setUsername(slotName);
                        // Warn if client version differs from host
                        final String clientVersion = event.getVersion();
                        final String hostVersion = BuildInfo.getVersionString();
                        if (clientVersion == null) {
                            broadcast(new MessageEvent(String.format(
                                "Warning: Could not determine %s's Forge version. "
                                + "Please use the same version as the host to avoid network compatibility issues.",
                                slotName)));
                        } else if (!clientVersion.equals(hostVersion)) {
                            broadcast(new MessageEvent(String.format(
                                "Warning: %s is using Forge version %s (host: %s). "
                                + "Please use the same version as the host to avoid network compatibility issues.",
                                slotName, clientVersion, hostVersion)));
                        }
                        broadcast(new MessageEvent(String.format("%s joined the lobby.", slotName)));
                        updateLobbyState();
                    }
                }
            } else if (msg instanceof UpdateLobbyPlayerEvent event) {
                updateSlot(client.getIndex(), event);
            }
            // Note: MessageEvent is handled by MessageHandler, not here
            // to avoid duplicate display on host's chat
            super.channelRead(ctx, msg);
        }
    }

    private class DeregisterClientHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
            final RemoteClient client = clients.remove(ctx.channel());
            if (client == null) {
                // Already handled (e.g. reconnect swapped the channel)
                super.channelInactive(ctx);
                return;
            }
            final String username = client.getUsername();

            if (isMatchActive() && client.hasValidSlot()) {
                // Game is active — enter reconnection mode
                // Pause the NetGuiGame so sends become no-ops
                pauseNetGuiGame(client.getIndex());

                // Unblock any pending sendAndWait calls
                client.getReplyPool().cancelAll();

                // Store for reconnection lookup
                disconnectedClients.put(username, client);

                // Start periodic countdown timer (ticks every 30s)
                final int[] remaining = {RECONNECT_TIMEOUT_SECONDS};
                final Timer timer = new Timer("reconnect-timeout-" + username, true);
                reconnectTimers.put(username, timer);
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        remaining[0] -= 30;
                        if (remaining[0] <= 0) {
                            cancel();
                            handleReconnectTimeout(username);
                        } else {
                            broadcast(new MessageEvent(
                                String.format("%s: %s remaining to reconnect.", username, formatTime(remaining[0]))));
                        }
                    }
                }, 30_000L, 30_000L);

                broadcast(new MessageEvent(
                    String.format("%s disconnected. Waiting %s for reconnect...", username, formatTime(RECONNECT_TIMEOUT_SECONDS))));
                lobbyListener.message(null, "(Host can use /skipreconnect to replace disconnected player with AI, or /skiptimeout to wait indefinitely.)");
                System.out.println("Player disconnected mid-game: " + username + " (slot " + client.getIndex() + "). Waiting for reconnect.");
            } else {
                // Normal disconnect (lobby or no valid slot)
                localLobby.disconnectPlayer(client.getIndex());
                broadcast(new MessageEvent(String.format("%s left the lobby.", username)));
                broadcast(new LogoutEvent(username));
            }
            super.channelInactive(ctx);
        }
    }
}
