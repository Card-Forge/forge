package forge.gamemodes.net.server;

import com.google.common.collect.ImmutableList;
import forge.gamemodes.match.LobbySlot;
import forge.gamemodes.match.LobbySlotType;
import forge.gamemodes.net.CompatibleObjectDecoder;
import forge.gamemodes.net.CompatibleObjectEncoder;
import forge.gamemodes.net.NetworkDebugLogger;
import forge.gamemodes.net.event.*;
import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiGame;
import forge.gui.util.SOptionPane;
import forge.interfaces.IGameController;
import forge.interfaces.ILobbyListener;
import forge.model.FModel;
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

    private final Map<Integer, NetGuiGame> playerGuis = new ConcurrentHashMap<>(); // Store NetGuiGame instances for reuse

    // Network byte tracking for monitoring actual bandwidth usage
    private final forge.gamemodes.net.NetworkByteTracker networkByteTracker =
            FModel.getPreferences().getPrefBoolean(forge.localinstance.properties.ForgePreferences.FPref.NET_BANDWIDTH_LOGGING) ? new forge.gamemodes.net.NetworkByteTracker() : null;

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

    /**
     * Get the network byte tracker for monitoring actual bandwidth usage.
     *
     * @return the NetworkByteTracker instance
     */
    public forge.gamemodes.net.NetworkByteTracker getNetworkByteTracker() {
        return networkByteTracker;
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
                    .option(ChannelOption.SO_REUSEADDR, true)  // Allow quick port reuse after server shutdown
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(final SocketChannel ch) throws Exception {
                            final ChannelPipeline p = ch.pipeline();
                            p.addLast(
                                    new CompatibleObjectEncoder(networkByteTracker),
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

    /**
     * Clear stored player GUI instances.
     * Called between games so new GUIs are created for the next game.
     */
    public void clearPlayerGuis() {
        playerGuis.clear();
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

                // Append (Host) indicator for the host player
                if (client.getIndex() == 0) {
                    username = username + " (Host)";
                }
                broadcast(new MessageEvent(username, message));
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
            super.channelActive(ctx);
        }

        @Override
        public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
            final RemoteClient client = clients.get(ctx.channel());
            if (msg instanceof LoginEvent event) {
                final String username = event.getUsername();
                client.setUsername(username);
                if (client.getIndex() == 0) {
                    broadcast(new MessageEvent(String.format("Lobby hosted by %s", username)));
                } else {
                    broadcast(new MessageEvent(String.format("%s joined the lobby", username)));
                }
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
                final int index = localLobby.connectPlayer(event.getUsername(), event.getAvatarIndex(), event.getSleeveIndex());
                if (index == -1) {
                    ctx.close();
                } else {
                    client.setIndex(index);
                    // Warn if client version differs from host
                    final String clientVersion = event.getVersion();
                    final String hostVersion = BuildInfo.getVersionString();
                    if (clientVersion == null) {
                        broadcast(new MessageEvent(String.format(
                            "Warning: Could not determine %s's Forge version. "
                            + "Please use the same version as the host to avoid network compatibility issues.",
                            event.getUsername())));
                    } else if (!clientVersion.equals(hostVersion)) {
                        broadcast(new MessageEvent(String.format(
                            "Warning: %s is using Forge version %s (host: %s). "
                            + "Please use the same version as the host to avoid network compatibility issues.",
                            event.getUsername(), clientVersion, hostVersion)));
                    }
                    broadcast(event);
                    updateLobbyState();
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
                super.channelInactive(ctx);
                return;
            }

            // Cancel any pending replies immediately to unblock game thread
            NetworkDebugLogger.log("[Disconnect] Canceling pending replies for disconnected client");
            client.cancelPendingReplies();

            final String username = client.getUsername();
            final int playerIndex = client.getIndex();

            NetworkDebugLogger.log("[Disconnect] Client disconnected: index=%d, username=%s", playerIndex, username);

            localLobby.disconnectPlayer(playerIndex);
            broadcast(new MessageEvent(String.format("%s left the lobby", username)));
            broadcast(new LogoutEvent(username));

            super.channelInactive(ctx);
        }
    }
}
