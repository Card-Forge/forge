package forge.gamemodes.net.server;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
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
    private static FServerManager instance = null;
    private final Map<Channel, RemoteClient> clients = new ConcurrentHashMap<>();
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

    // Session management for reconnection support
    private GameSession currentGameSession;
    private final Map<Integer, Timer> disconnectTimeoutTimers = new ConcurrentHashMap<>();
    private final Map<String, Integer> pendingReconnections = new ConcurrentHashMap<>(); // sessionId+token -> playerIndex
    private final Map<Integer, NetGuiGame> playerGuis = new ConcurrentHashMap<>(); // Store NetGuiGame instances for reuse

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
        if (event instanceof MessageEvent) {
            MessageEvent msgEvent = (MessageEvent) event;
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

        // Check if this is a ready state change
        if (event.getReady() != null && event.getReady()) {
            // Count ready players and total players
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

            // Broadcast ready notification
            String playerName = localLobby.getSlot(index).getName();
            broadcast(new MessageEvent(String.format("%s is ready (%d/%d players ready)",
                playerName, readyCount, totalPlayers)));

            // Check if all players are ready
            if (readyCount == totalPlayers && totalPlayers > 1) {
                broadcast(new MessageEvent("All players ready! Starting game..."));
            }
        }
    }

    public IGuiGame getGui(final int index) {
        final LobbySlot slot = localLobby.getSlot(index);
        final LobbySlotType type = slot.getType();
        if (type == LobbySlotType.LOCAL) {
            return GuiBase.getInterface().getNewGuiGame();
        } else if (type == LobbySlotType.REMOTE) {
            // Check if we already have a stored NetGuiGame for this player
            NetGuiGame existingGui = playerGuis.get(index);
            if (existingGui != null) {
                return existingGui;
            }
            // Create a new NetGuiGame and store it
            for (final RemoteClient client : clients.values()) {
                if (client.getIndex() == index) {
                    NetGuiGame newGui = new NetGuiGame(client);
                    playerGuis.put(index, newGui);
                    return newGui;
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

    private class MessageHandler extends ChannelInboundHandlerAdapter {
        @Override
        public final void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
            final RemoteClient client = clients.get(ctx.channel());
            if (msg instanceof MessageEvent) {
                String username = client.getUsername();
                String message = ((MessageEvent) msg).getMessage();

                // Check for host commands
                if (client.getIndex() == 0 && message.startsWith("/skipreconnect")) {
                    handleSkipReconnectCommand(message);
                    return; // Don't broadcast the command itself
                }

                // Append (Host) indicator for the host player
                if (client.getIndex() == 0) {
                    username = username + " (Host)";
                }
                broadcast(new MessageEvent(username, message));
            }
            super.channelRead(ctx, msg);
        }

        private void handleSkipReconnectCommand(String message) {
            // Parse command: /skipreconnect <playerName> or just /skipreconnect (for first disconnected player)
            String[] parts = message.trim().split("\\s+", 2);

            if (currentGameSession == null || !currentGameSession.isGameInProgress()) {
                broadcast(new MessageEvent("No active game session."));
                return;
            }

            String targetPlayerName = null;
            if (parts.length > 1) {
                targetPlayerName = parts[1].trim();
            }

            // Find disconnected player
            int targetIndex = -1;
            String targetUsername = null;

            if (targetPlayerName != null) {
                // Find specific player by name
                for (int i = 0; i < 8; i++) {
                    PlayerSession playerSession = currentGameSession.getPlayerSession(i);
                    if (playerSession != null && playerSession.isDisconnected()) {
                        String playerName = playerSession.getPlayerName();
                        if (playerName != null && playerName.equalsIgnoreCase(targetPlayerName)) {
                            targetIndex = i;
                            targetUsername = playerName;
                            break;
                        }
                    }
                }
            } else {
                // Find first disconnected player
                for (int i = 0; i < 8; i++) {
                    PlayerSession playerSession = currentGameSession.getPlayerSession(i);
                    if (playerSession != null && playerSession.isDisconnected()) {
                        targetIndex = i;
                        targetUsername = playerSession.getPlayerName();
                        break;
                    }
                }
            }

            if (targetIndex == -1) {
                if (targetPlayerName != null) {
                    broadcast(new MessageEvent(String.format("No disconnected player found with name '%s'.", targetPlayerName)));
                } else {
                    broadcast(new MessageEvent("No disconnected players found."));
                }
                return;
            }

            // Cancel the timeout timer and immediately convert to AI
            skipReconnectionTimeout(targetIndex, targetUsername);
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
            if (msg instanceof ReconnectRequestEvent) {
                // Handle reconnection request with credentials
                ReconnectRequestEvent event = (ReconnectRequestEvent) msg;
                handleReconnectRequest(ctx, client, event);
            } else if (msg instanceof LoginEvent) {
                final LoginEvent event = (LoginEvent) msg;
                final String username = event.getUsername();

                // Check if this player can rejoin an existing game session
                if (tryReconnectByUsername(ctx, client, username, event.getAvatarIndex(), event.getSleeveIndex())) {
                    // Successfully reconnected to existing game
                    return;
                }

                // Normal login - no existing session to rejoin
                client.setUsername(username);
                broadcast(new MessageEvent(String.format("%s joined the room", username)));
                updateLobbyState();
            } else if (msg instanceof UpdateLobbyPlayerEvent) {
                UpdateLobbyPlayerEvent updateEvent = (UpdateLobbyPlayerEvent) msg;
                localLobby.applyToSlot(client.getIndex(), updateEvent);

                // Check if this is a ready state change
                if (updateEvent.getReady() != null && updateEvent.getReady()) {
                    // Count ready players and total players
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

                    // Broadcast ready notification
                    String playerName = client.getUsername();
                    broadcast(new MessageEvent(String.format("%s is ready (%d/%d players ready)",
                        playerName, readyCount, totalPlayers)));

                    // Check if all players are ready
                    if (readyCount == totalPlayers && totalPlayers > 1) {
                        broadcast(new MessageEvent("All players ready! Starting game..."));
                    }
                }
            }
            super.channelRead(ctx, msg);
        }

        private void handleReconnectRequest(ChannelHandlerContext ctx, RemoteClient client, ReconnectRequestEvent event) {
            String sessionId = event.getSessionId();
            String token = event.getToken();

            boolean success = handleReconnection(client, sessionId, token);
            if (success) {
                // Send full state back to client
                IGuiGame gui = getGui(client.getIndex());
                if (gui instanceof NetGuiGame) {
                    NetGuiGame netGui = (NetGuiGame) gui;
                    PlayerSession playerSession = currentGameSession.getPlayerSession(client.getIndex());
                    if (playerSession != null) {
                        netGui.sendFullStateForReconnect(sessionId, playerSession.getSessionToken());
                    }
                }
            } else {
                // Send rejection
                ReconnectRejectedEvent rejectEvent = new ReconnectRejectedEvent("Invalid session or token");
                client.send(rejectEvent);
                // Close the connection
                ctx.close();
            }
        }
    }

    private class LobbyInputHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
            final RemoteClient client = clients.get(ctx.channel());
            if (msg instanceof LoginEvent) {
                final LoginEvent event = (LoginEvent) msg;
                final int index = localLobby.connectPlayer(event.getUsername(), event.getAvatarIndex(), event.getSleeveIndex());
                if (index == -1) {
                    ctx.close();
                } else {
                    client.setIndex(index);
                    broadcast(event);
                    updateLobbyState();
                }
            } else if (msg instanceof UpdateLobbyPlayerEvent) {
                updateSlot(client.getIndex(), (UpdateLobbyPlayerEvent) msg);
            } else if (msg instanceof MessageEvent) {
                final MessageEvent event = (MessageEvent) msg;
                lobbyListener.message(event.getSource(), event.getMessage());
            }
            super.channelRead(ctx, msg);
        }
    }

    private class DeregisterClientHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
            final RemoteClient client = clients.remove(ctx.channel());
            if (client == null) {
                super.channelInactive(ctx);
                return;
            }

            final String username = client.getUsername();
            final int playerIndex = client.getIndex();

            // Check if there's an active game session that supports reconnection
            if (currentGameSession != null && currentGameSession.isGameInProgress()) {
                // Mark player as disconnected but don't remove from game
                currentGameSession.markPlayerDisconnected(playerIndex);

                // Pause the game and notify other players
                String pauseMessage = String.format("Waiting for %s to reconnect...", username);
                currentGameSession.pauseGame(pauseMessage);
                broadcastGamePaused(pauseMessage, client);

                // Schedule timeout for reconnection
                scheduleReconnectionTimeout(playerIndex, username);

                broadcast(new MessageEvent(String.format("%s disconnected. Game paused. Waiting for reconnection...", username)));
            } else {
                // Normal disconnect - not in a game or session doesn't support reconnection
                localLobby.disconnectPlayer(playerIndex);
                broadcast(new MessageEvent(String.format("%s left the room", username)));
                broadcast(new LogoutEvent(username));
            }

            super.channelInactive(ctx);
        }
    }

    // Session management methods

    /**
     * Create a new game session for the current game.
     * Call this when starting a network game.
     * @return the created game session
     */
    public GameSession createGameSession() {
        currentGameSession = new GameSession();
        System.out.println("[GameSession] Creating game session, registering " + clients.size() + " remote clients");
        // Register all current players
        for (RemoteClient client : clients.values()) {
            PlayerSession playerSession = currentGameSession.registerPlayer(client.getIndex());
            playerSession.setPlayerName(client.getUsername());
            System.out.println("[GameSession] Registered player index=" + client.getIndex() + ", name=" + client.getUsername());
        }
        return currentGameSession;
    }

    /**
     * Get the current game session.
     * @return the current game session, or null if none exists
     */
    public GameSession getCurrentGameSession() {
        return currentGameSession;
    }

    /**
     * Mark the current game session as in progress.
     */
    public void setGameInProgress(boolean inProgress) {
        if (currentGameSession != null) {
            currentGameSession.setGameInProgress(inProgress);
        }
    }

    /**
     * End the current game session.
     */
    public void endGameSession() {
        // Cancel any pending timeout timers
        for (Timer timer : disconnectTimeoutTimers.values()) {
            timer.cancel();
        }
        disconnectTimeoutTimers.clear();
        pendingReconnections.clear();
        playerGuis.clear(); // Clear stored GUIs so new ones are created for the next game
        currentGameSession = null;
    }

    /**
     * Called when the game ends normally (not due to disconnect).
     * Cleans up the session and allows for a new game.
     */
    public void onGameEnded() {
        if (currentGameSession != null) {
            // Announce the winner
            announceGameWinner();

            // Broadcast returning to lobby message
            broadcast(new MessageEvent("Returning to lobby..."));

            // Mark game as no longer in progress
            currentGameSession.setGameInProgress(false);
            // End the session
            endGameSession();
        }
    }

    /**
     * Announce the game winner to all players.
     */
    private void announceGameWinner() {
        if (localLobby == null) {
            return;
        }

        try {
            HostedMatch hostedMatch = localLobby.getHostedMatch();
            if (hostedMatch != null) {
                forge.game.Match match = hostedMatch.getMatch();
                if (match != null) {
                    forge.game.player.RegisteredPlayer winner = match.getWinner();

                    String message;
                    if (winner != null) {
                        String winnerName = winner.getPlayer().getName();
                        message = String.format("Game ended. Winner: %s", winnerName);
                    } else {
                        message = "Game ended. Draw";
                    }

                    broadcast(new MessageEvent(message));
                }
            }
        } catch (Exception e) {
            // If we can't determine the winner, just announce game end
            broadcast(new MessageEvent("Game ended."));
            System.err.println("Error determining game winner: " + e.getMessage());
        }
    }

    /**
     * Broadcast game paused notification to all clients except the disconnected one.
     */
    private void broadcastGamePaused(String message, RemoteClient exceptClient) {
        GamePausedEvent event = new GamePausedEvent(message);
        broadcastExcept(event, exceptClient);
    }

    /**
     * Broadcast game resumed notification to all clients.
     */
    private void broadcastGameResumed() {
        GameResumedEvent event = new GameResumedEvent();
        broadcast(event);
    }

    /**
     * Schedule a timeout for player reconnection with countdown notifications every 30 seconds.
     */
    private void scheduleReconnectionTimeout(final int playerIndex, final String username) {
        // Cancel any existing timer for this player
        Timer existingTimer = disconnectTimeoutTimers.remove(playerIndex);
        if (existingTimer != null) {
            existingTimer.cancel();
        }

        long timeoutMs = currentGameSession != null ?
                currentGameSession.getDisconnectTimeoutMs() :
                GameSession.DEFAULT_DISCONNECT_TIMEOUT_MS;

        Timer timer = new Timer("ReconnectionTimeout-" + playerIndex);

        // Schedule countdown notifications every 30 seconds
        final long countdownInterval = 30 * 1000; // 30 seconds
        long currentTime = 0;

        while (currentTime < timeoutMs) {
            final long remainingTime = timeoutMs - currentTime;

            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    // Format remaining time as M:SS
                    long remainingSeconds = remainingTime / 1000;
                    long minutes = remainingSeconds / 60;
                    long seconds = remainingSeconds % 60;
                    String timeStr = String.format("%d:%02d", minutes, seconds);

                    broadcast(new MessageEvent(String.format("Waiting for %s to reconnect... (%s remaining)",
                        username, timeStr)));
                }
            }, currentTime);

            currentTime += countdownInterval;
        }

        // Schedule the final timeout handler
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                handleReconnectionTimeout(playerIndex, username);
            }
        }, timeoutMs);

        disconnectTimeoutTimers.put(playerIndex, timer);
    }

    /**
     * Handle reconnection timeout expiry.
     * Converts the disconnected player to AI control instead of removing them.
     */
    private void handleReconnectionTimeout(int playerIndex, String username) {
        disconnectTimeoutTimers.remove(playerIndex);

        if (currentGameSession != null) {
            // Convert to AI control instead of removing the player
            convertPlayerToAI(playerIndex, username);

            broadcast(new MessageEvent(String.format("%s timed out. AI taking over.", username)));

            // If no more disconnected players, resume the game
            if (!currentGameSession.hasDisconnectedPlayers()) {
                currentGameSession.resumeGame();
                broadcastGameResumed();
            }
        }
    }

    /**
     * Skip the reconnection timeout and immediately convert a player to AI.
     * This is called when the host uses the /skipreconnect command.
     * @param playerIndex the player's index
     * @param username the player's username
     */
    private void skipReconnectionTimeout(int playerIndex, String username) {
        // Cancel the timeout timer if it exists
        Timer timer = disconnectTimeoutTimers.remove(playerIndex);
        if (timer != null) {
            timer.cancel();
        }

        if (currentGameSession != null) {
            // Convert to AI control
            convertPlayerToAI(playerIndex, username);

            broadcast(new MessageEvent(String.format("Host skipped reconnection wait. %s replaced with AI.", username)));

            // If no more disconnected players, resume the game
            if (!currentGameSession.hasDisconnectedPlayers()) {
                currentGameSession.resumeGame();
                broadcastGameResumed();
            }
        }
    }

    /**
     * Convert a disconnected player to AI control.
     * This maintains game state while allowing the game to continue.
     */
    private void convertPlayerToAI(int playerIndex, String username) {
        // Update the lobby slot to AI type
        LobbySlot slot = localLobby.getSlot(playerIndex);
        if (slot != null) {
            slot.setType(LobbySlotType.AI);
            slot.setName(username + " (AI)");
        }

        // Get the game and player instance
        if (localLobby.getHostedMatch() != null) {
            forge.game.Match match = localLobby.getHostedMatch().getMatch();
            if (match != null) {
                forge.game.Game game = match.getGame();
                if (game != null) {
                    // Find the player by matching lobby player name
                    forge.game.player.Player targetPlayer = null;
                    for (forge.game.player.Player p : game.getPlayers()) {
                        if (username.equals(p.getLobbyPlayer().getName())) {
                            targetPlayer = p;
                            break;
                        }
                    }

                    if (targetPlayer != null) {
                        // Create an AI controller for this player
                        forge.ai.LobbyPlayerAi aiLobbyPlayer = new forge.ai.LobbyPlayerAi(username + " (AI)", null);
                        forge.ai.PlayerControllerAi aiController = new forge.ai.PlayerControllerAi(game, targetPlayer, aiLobbyPlayer);

                        // Replace the player's controller with the AI controller
                        targetPlayer.dangerouslySetController(aiController);

                        System.out.println("[AI Takeover] Converted player " + username + " at index " + playerIndex + " to AI control");
                    }
                }
            }
        }

        // Mark player as connected in the session (AI is "connected")
        currentGameSession.markPlayerConnected(playerIndex);

        // Update lobby state
        updateLobbyState();
    }

    /**
     * Try to reconnect a player by username to an existing game session.
     * This is called when a player logs in and there's a game waiting for them.
     * @param ctx the channel context
     * @param client the client
     * @param username the player's username
     * @param avatarIndex the avatar index
     * @param sleeveIndex the sleeve index
     * @return true if reconnection was successful
     */
    private boolean tryReconnectByUsername(ChannelHandlerContext ctx, RemoteClient client,
                                           String username, int avatarIndex, int sleeveIndex) {
        System.out.println("[Reconnect] Checking reconnection for username: " + username);
        System.out.println("[Reconnect] currentGameSession: " + currentGameSession);
        if (currentGameSession == null) {
            System.out.println("[Reconnect] No game session exists");
            return false;
        }
        System.out.println("[Reconnect] Game in progress: " + currentGameSession.isGameInProgress());
        if (!currentGameSession.isGameInProgress()) {
            System.out.println("[Reconnect] Game not in progress");
            return false;
        }

        // Look for a disconnected player with this username
        for (int i = 0; i < 8; i++) {
            PlayerSession playerSession = currentGameSession.getPlayerSession(i);
            if (playerSession != null) {
                System.out.println("[Reconnect] Player " + i + ": name=" + playerSession.getPlayerName() +
                    ", disconnected=" + playerSession.isDisconnected());
            }
            if (playerSession != null &&
                playerSession.isDisconnected() &&
                username.equals(playerSession.getPlayerName())) {

                // Found a matching disconnected player - reconnect them
                System.out.println("[Reconnect] Found matching disconnected player at index " + i);

                // Cancel timeout timer atomically to prevent race condition
                disconnectTimeoutTimers.computeIfPresent(i, (key, timer) -> {
                    timer.cancel();
                    return null;  // Remove from map
                });

                // Update client
                client.setIndex(i);
                client.setUsername(username);
                clients.put(ctx.channel(), client);

                // Mark player as connected
                currentGameSession.markPlayerConnected(i);

                broadcast(new MessageEvent(String.format("%s has reconnected!", username)));

                // Get the stored GUI (not a new one) and update it for reconnection
                NetGuiGame netGui = playerGuis.get(i);
                System.out.println("[Reconnect] Stored GUI for index " + i + ": " + netGui);
                if (netGui != null) {
                    // CRITICAL: Update the sender to use the new connection
                    netGui.updateClient(client);

                    // Find the PlayerView for this player to send openView
                    forge.game.GameView gameView = netGui.getGameView();
                    System.out.println("[Reconnect] GameView: " + gameView);
                    if (gameView != null) {
                        forge.game.player.PlayerView playerView = null;
                        for (forge.game.player.PlayerView pv : gameView.getPlayers()) {
                            // Match by lobby player name
                            if (username.equals(pv.getLobbyPlayerName())) {
                                playerView = pv;
                                break;
                            }
                        }

                        if (playerView != null) {
                            System.out.println("[Reconnect] Found PlayerView: " + playerView);

                            // CRITICAL: Send setGameView BEFORE openView!
                            // The client needs gameView to be set before processing openView
                            // because beforeCall for openView uses gui.getGameView() to create the match
                            netGui.setGameView(null);  // Clear first (mimics initial connection)
                            netGui.setGameView(gameView);  // This sends gameView to client
                            System.out.println("[Reconnect] Sent setGameView to client");

                            // Now send openView to trigger client UI transition
                            forge.trackable.TrackableCollection<forge.game.player.PlayerView> myPlayers =
                                new forge.trackable.TrackableCollection<>(playerView);
                            netGui.openView(myPlayers);
                            System.out.println("[Reconnect] Sent openView to trigger UI transition");
                        } else {
                            System.out.println("[Reconnect] WARNING: Could not find PlayerView for " + username);
                        }

                        // Send full game state with session credentials
                        System.out.println("[Reconnect] Sending full game state to reconnected client");
                        netGui.sendFullStateForReconnect(
                            currentGameSession.getSessionId(),
                            playerSession.getSessionToken()
                        );
                    } else {
                        System.out.println("[Reconnect] WARNING: GameView is null in NetGuiGame");
                    }
                } else {
                    System.out.println("[Reconnect] WARNING: No stored NetGuiGame for index " + i);
                }

                // Check if all players are back
                if (currentGameSession.allPlayersConnected()) {
                    currentGameSession.resumeGame();
                    broadcastGameResumed();
                }

                return true;
            }
        }

        System.out.println("[Reconnect] No matching disconnected player found for: " + username);
        return false;
    }

    /**
     * Handle a reconnection request from a client.
     * @param client the reconnecting client
     * @param sessionId the session ID
     * @param token the session token
     * @return true if reconnection was successful
     */
    public boolean handleReconnection(RemoteClient client, String sessionId, String token) {
        if (currentGameSession == null || !sessionId.equals(currentGameSession.getSessionId())) {
            return false;
        }

        // Find the player session by token
        for (int i = 0; i < 8; i++) { // Max 8 players
            PlayerSession playerSession = currentGameSession.getPlayerSession(i);
            if (playerSession != null && playerSession.validateToken(token)) {
                // Cancel timeout timer atomically to prevent race condition
                disconnectTimeoutTimers.computeIfPresent(i, (key, timer) -> {
                    timer.cancel();
                    return null;  // Remove from map
                });

                // Update client index and reconnect
                client.setIndex(i);
                client.setUsername(playerSession.getPlayerName());
                currentGameSession.markPlayerConnected(i);

                broadcast(new MessageEvent(String.format("%s has reconnected!", playerSession.getPlayerName())));

                // Check if all players are back
                if (currentGameSession.allPlayersConnected()) {
                    currentGameSession.resumeGame();
                    broadcastGameResumed();
                }

                return true;
            }
        }

        return false;
    }

    /**
     * Event for game paused notification.
     */
    public static class GamePausedEvent implements NetEvent {
        private static final long serialVersionUID = 1L;
        private final String message;

        public GamePausedEvent(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public void updateForClient(RemoteClient client) {
        }
    }

    /**
     * Event for game resumed notification.
     */
    public static class GameResumedEvent implements NetEvent {
        private static final long serialVersionUID = 1L;

        @Override
        public void updateForClient(RemoteClient client) {
        }
    }
}
