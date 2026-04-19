package forge.gamemodes.net.server;

import com.google.common.collect.ImmutableList;
import forge.ai.LobbyPlayerAi;
import forge.ai.PlayerControllerAi;
import forge.game.Game;
import forge.game.player.Player;
import forge.gamemodes.match.HostedMatch;
import forge.gamemodes.match.LobbySlot;
import forge.gamemodes.match.LobbySlotType;
import forge.gamemodes.match.input.InputSynchronized;
import forge.gamemodes.net.CompatibleObjectDecoder;
import forge.gamemodes.net.CompatibleObjectEncoder;
import forge.gamemodes.net.NetworkLogConfig;
import forge.util.IHasForgeLog;
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
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

import org.jupnp.UpnpService;
import org.jupnp.UpnpServiceImpl;
import org.jupnp.model.meta.Device;
import org.jupnp.registry.Registry;
import org.jupnp.support.igd.PortMappingListener;
import org.jupnp.support.model.PortMapping;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

public final class FServerManager implements IHasForgeLog {

    static final int HEARTBEAT_TIMEOUT_SECONDS = Integer.getInteger("forge.net.heartbeatTimeout", 45);
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

    private final Map<Integer, RemoteClientGuiGame> playerGuis = new ConcurrentHashMap<>(); // Store RemoteClientGuiGame instances for reuse

    // Network byte tracking for monitoring actual bandwidth usage
    private final forge.gamemodes.net.NetworkByteTracker byteTracker =
            FModel.getNetPreferences().getPrefBoolean(forge.localinstance.properties.ForgeNetPreferences.FNetPref.NET_BANDWIDTH_LOGGING) ? new forge.gamemodes.net.NetworkByteTracker() : null;

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
    public forge.gamemodes.net.NetworkByteTracker getByteTracker() {
        return byteTracker;
    }

    public void startServer(final int port) {
        this.port = port;
        String UPnPOption = FModel.getNetPreferences().getPref(ForgeNetPreferences.FNetPref.UPnP);
        boolean startUPnP;
        if (UPnPOption.equalsIgnoreCase("ASK")) {
            startUPnP = callUPnPDialog();
        } else {
            startUPnP = UPnPOption.equalsIgnoreCase("ALWAYS");
        }
        netLog.info("Starting Multiplayer Server");
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
                                    new CompatibleObjectEncoder(byteTracker),
                                    new CompatibleObjectDecoder(9766 * 1024, ClassResolvers.cacheDisabled(null)),
                                    new IdleStateHandler(HEARTBEAT_TIMEOUT_SECONDS, 0, 0, TimeUnit.SECONDS),
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
                    netLog.error(e, "Server channel error");
                } finally {
                    stopServer();
                }
            }).start();
            if (startUPnP) {
                mapNatPort();
            }
            Runtime.getRuntime().addShutdownHook(shutdownHook);
            isHosting = true;
        } catch (final InterruptedException e) {
            netLog.error(e, "Server start interrupted");
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
        clients.clear();
        afkSlots.clear();

        try {
            bossGroup.shutdownGracefully().sync();
            workerGroup.shutdownGracefully().sync();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (upnpService != null) {
            upnpService.shutdown();
            upnpService = null;
        }
        if (removeShutdownHook) {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        }
        isHosting = false;
        UPnPMapped = false;
        NetworkLogConfig.deactivateNetworkLogging();
        // create new EventLoopGroups for potential restart
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
    }

    public boolean isHosting() {
        return isHosting;
    }

    public boolean isUPnPMapped() {
        return UPnPMapped;
    }

    public int getTotalSendErrors() {
        int total = 0;
        for (final RemoteClient client : clients.values()) {
            total += client.getSendErrorCount();
        }
        for (final RemoteClient client : disconnectedClients.values()) {
            total += client.getSendErrorCount();
        }
        return total;
    }

    public void broadcast(final NetEvent event) {
        if (event instanceof MessageEvent msgEvent) {
            lobbyListener.message(msgEvent.getSource(), msgEvent.getMessage());
        }
        broadcastTo(event, clients.values());
    }

    public String formatAfkTimeoutMessage() {
        final int minutes = FModel.getNetPreferences().getPrefInt(ForgeNetPreferences.FNetPref.NET_AFK_TIMEOUT);
        if (minutes <= 0) {
            return Localizer.getInstance().getMessage("lblAfkTimeoutDisabled");
        }
        return Localizer.getInstance().getMessage("lblAfkTimeoutChat", minutes + ":00");
    }

    // Warnings must be one-shot — never use scheduleAtFixedRate here
    private static final ScheduledExecutorService afkExecutor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "AFK-Timeout");
                t.setDaemon(true);
                return t;
            });

    private static final long AFK_REPEAT_TIMEOUT_MS = 10_000L;
    private static final long AFK_WARNING_LEAD_MS = 30_000L;
    private static final int HOST_SLOT = -1;

    private final Set<Integer> afkSlots = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @FunctionalInterface
    public interface AfkTimeout {
        AfkTimeout NOOP = () -> {};
        void cancel();
    }

    /**
     * {@code cancelAll()} is safe here only because this is armed exclusively from
     * {@code InputPassPriority}: the sole replies that can be pending on the channel
     * are sub-prompts like {@code getAbilityToPlay}, which treat null as pass.
     * Extending to other server-side waits (assignCombatDamage, getChoices, order,
     * ...) is blocked on those methods not being null-safe.
     */
    public AfkTimeout armAfkTimeout(final PlayerControllerHuman controller, final InputSynchronized input) {
        if (!isHosting() || localLobby == null) {
            return AfkTimeout.NOOP;
        }
        final HostedMatch hostedMatch = localLobby.getHostedMatch();
        if (hostedMatch == null || controller.getGame() != hostedMatch.getGame()) {
            // Input belongs to a side-game the host started while waiting (e.g. local vs AI)
            return AfkTimeout.NOOP;
        }
        final int fullMinutes = FModel.getNetPreferences().getPrefInt(ForgeNetPreferences.FNetPref.NET_AFK_TIMEOUT);
        if (fullMinutes <= 0) {
            return AfkTimeout.NOOP;
        }
        final String displayName = controller.getPlayer().getName();
        final RemoteClient remoteClient = controller.getGui() instanceof RemoteClientGuiGame remoteGui
                ? remoteGui.getClient()
                : null;
        final int slot = remoteClient != null ? remoteClient.getIndex() : HOST_SLOT;
        final boolean alreadyAfk = afkSlots.contains(slot);
        final long timeoutMs = alreadyAfk ? AFK_REPEAT_TIMEOUT_MS : fullMinutes * 60_000L;
        final long warningDelayMs = alreadyAfk ? -1 : timeoutMs - AFK_WARNING_LEAD_MS;
        final AtomicBoolean settled = new AtomicBoolean(false);

        final ScheduledFuture<?> warningFuture = warningDelayMs > 0
                ? afkExecutor.schedule(() -> {
                    if (settled.get()) { return; }
                    broadcast(new MessageEvent(Localizer.getInstance().getMessage(
                            "lblAfkWarning", displayName, fullMinutes + ":00")));
                }, warningDelayMs, TimeUnit.MILLISECONDS)
                : null;

        final ScheduledFuture<?> timeoutFuture = afkExecutor.schedule(() -> {
            if (!settled.compareAndSet(false, true)) { return; }
            // Only first fire announces — subsequent shortened repeats pass silently
            final boolean firstTimeAfk = afkSlots.add(slot);
            if (firstTimeAfk) {
                broadcast(new MessageEvent(Localizer.getInstance().getMessage(
                        "lblAfkAutoPass", displayName, fullMinutes + ":00")));
            }
            if (remoteClient != null) {
                remoteClient.getReplyPool().cancelAll();
            }
            input.stop();
        }, timeoutMs, TimeUnit.MILLISECONDS);

        return () -> {
            if (!settled.compareAndSet(false, true)) { return; }
            afkSlots.remove(slot);
            if (warningFuture != null) { warningFuture.cancel(false); }
            timeoutFuture.cancel(false);
        };
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
            // Check if we already have a stored RemoteClientGuiGame for this player
            RemoteClientGuiGame existingGui = playerGuis.get(index);
            if (existingGui != null) {
                return existingGui;
            }
            // Create a new RemoteClientGuiGame and store it
            for (final RemoteClient client : clients.values()) {
                if (client.getIndex() == index) {
                    RemoteClientGuiGame newGui = new RemoteClientGuiGame(client);
                    playerGuis.put(index, newGui);
                    return newGui;
                }
            }
        }
        return null;
    }

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
            netLog.error(e, "Failed to get local address");
            return "localhost";
        }
    }

    /**
     * Returns all usable IPv4 addresses from all network interfaces.
     * Each entry maps a friendly display name to its IPv4 address.
     * Results are ordered: routable address first, then others alphabetically.
     */
    public static LinkedHashMap<String, String> getAllLocalAddresses() {
        final LinkedHashMap<String, String> result = new LinkedHashMap<>();
        final String routableAddress = getLocalAddress();

        try {
            final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            final TreeMap<String, String> sorted = new TreeMap<>();

            while (interfaces.hasMoreElements()) {
                final NetworkInterface iface = interfaces.nextElement();
                if (!iface.isUp() || iface.isLoopback()) {
                    continue;
                }
                final Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    final InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        final String ip = addr.getHostAddress();
                        final String name = getFriendlyInterfaceName(iface.getName(), iface.getDisplayName(), ip);
                        if (ip.equals(routableAddress)) {
                            result.put(name, ip);
                        } else {
                            sorted.put(name, ip);
                        }
                    }
                }
            }
            result.putAll(sorted);
        } catch (final SocketException e) {
            netLog.error(e, "Failed to enumerate network interfaces");
            if (result.isEmpty()) {
                result.put("Default", routableAddress);
            }
        }

        if (result.isEmpty()) {
            result.put("Default", routableAddress);
        }
        return result;
    }

    private static String getFriendlyInterfaceName(final String ifName, final String displayName, final String ip) {
        final String lower = ifName.toLowerCase();
        final String lowerDisplay = displayName.toLowerCase();

        if (lower.startsWith("ham") || lowerDisplay.contains("hamachi")) {
            return "Hamachi";
        }
        if (lower.startsWith("zt") || lowerDisplay.contains("zerotier")) {
            return "ZeroTier";
        }
        if (isTailscaleAddress(ip)) {
            return "Tailscale";
        }
        if (lower.startsWith("wg")) {
            return "WireGuard";
        }
        if ((lower.startsWith("tun") && !lower.startsWith("utun")) || lower.startsWith("tap")) {
            return "VPN (" + ifName + ")";
        }
        if (lower.startsWith("utun")) {
            return "VPN Tunnel";
        }
        if (lower.startsWith("feth")) {
            return "Virtual Network";
        }
        if (lower.startsWith("en")) {
            if (lowerDisplay.contains("wi-fi") || lowerDisplay.contains("wifi") || lowerDisplay.contains("airport")) {
                return "Wi-Fi";
            }
            if (lowerDisplay.contains("thunderbolt") || lowerDisplay.contains("ethernet")) {
                return "Ethernet";
            }
            return "LAN (" + ifName + ")";
        }
        if (lower.startsWith("eth") || lower.startsWith("ens") || lower.startsWith("enp")) {
            return "Ethernet";
        }
        if (lower.startsWith("wl")) {
            return "Wi-Fi";
        }
        if (lowerDisplay.contains("radmin")) {
            return "Radmin VPN";
        }
        return displayName;
    }

    private static boolean isTailscaleAddress(final String ip) {
        try {
            final String[] parts = ip.split("\\.");
            if (parts.length == 4) {
                final int first = Integer.parseInt(parts[0]);
                final int second = Integer.parseInt(parts[1]);
                return first == 100 && second >= 64 && second <= 127;
            }
        } catch (final NumberFormatException ignored) { }
        return false;
    }

    public static String getExternalAddress() {
        BufferedReader in = null;
        try {
            URL whatismyip = new URL("https://checkip.amazonaws.com");
            in = new BufferedReader(new InputStreamReader(
                    whatismyip.openStream()));
            return in.readLine();
        } catch (IOException e) {
            netLog.error(e, "Failed to get external address");
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    netLog.error(e, "Failed to close address reader");
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

            final ForgePortMappingListener listener = new ForgePortMappingListener(portMapping);
            upnpService.getRegistry().addListener(listener);
            // Trigger device discovery
            upnpService.getControlPoint().search();

            // If no IGD responds within 5 seconds, report failure
            new Timer("upnp-timeout", true).schedule(new TimerTask() {
                @Override
                public void run() {
                    if (!listener.isCompleted()) {
                        listener.setCompleted();
                        onUPnPResult(false);
                    }
                }
            }, 5000);
        } catch (Exception e) {
            netLog.error(e, "UPnP mapping error");
        }
    }

    private void onUPnPResult(boolean success) {
        String msg = success
            ? localizer.getMessage("lblUPnPSuccess", String.valueOf(port))
            : localizer.getMessage("lblUPnPFailed", String.valueOf(port));
        if (lobbyListener != null) {
            broadcast(new MessageEvent(msg));
        }
    }

    /**
     * Extends jupnp's PortMappingListener to report mapping success or failure.
     * The superclass runs port mapping actions synchronously inside deviceAdded(),
     * so by the time super.deviceAdded() returns, the result is known.
     */
    private class ForgePortMappingListener extends PortMappingListener {
        private volatile boolean completed = false;

        ForgePortMappingListener(PortMapping portMapping) {
            super(portMapping);
        }

        @Override
        public synchronized void deviceAdded(Registry registry, Device device) {
            super.deviceAdded(registry, device);
            if (!completed && !activePortMappings.isEmpty()) {
                completed = true;
                UPnPMapped = true;
                onUPnPResult(true);
            }
        }

        @Override
        protected void handleFailureMessage(String message) {
            super.handleFailureMessage(message);
            if (!completed) {
                completed = true;
                onUPnPResult(false);
            }
        }

        boolean isCompleted() { return completed; }
        void setCompleted() { completed = true; }
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
        if (target == null) { return true; }
        netLog.info("[Reconnect] Host used /skipreconnect for {}", target);
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
        netLog.info("[Reconnect] Host used /skiptimeout for {}", target);
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

    private void pauseRemoteClientGuiGame(final int slotIndex) {
        final HostedMatch hostedMatch = localLobby.getHostedMatch();
        if (hostedMatch == null) { return; }
        final Game game = hostedMatch.getGame();
        if (game == null) { return; }

        for (final Player p : game.getPlayers()) {
            final IGuiGame gui = hostedMatch.getGuiForPlayer(p);
            if (gui instanceof RemoteClientGuiGame ngg && ngg.getClient().getIndex() == slotIndex) {
                ngg.pause();
                netLog.info("[Reconnect] Paused RemoteClientGuiGame for slot {} ({})", slotIndex, p.getName());
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
            if (gui instanceof RemoteClientGuiGame netGui && netGui.getClient().getIndex() == slotIndex) {
                netLog.info("[Reconnect] Resuming RemoteClientGuiGame for slot {} ({})", slotIndex, p.getName());
                netGui.resume();

                // Reset delta sync state — reconnecting client has no prior baseline
                netGui.resetForReconnect();
                netLog.info("[Reconnect] Delta sync state reset for slot {}", slotIndex);

                // Send game state via setGameView protocol (client needs gameView set before openView)
                netGui.updateGameView();
                netGui.openView(new forge.trackable.TrackableCollection<>(netGui.getLocalPlayers()));
                netLog.info("[Reconnect] Sent game state and openView to slot {}", slotIndex);

                // Replay current prompt
                final PlayerControllerHuman pch = findRemoteController(slotIndex);
                if (pch != null) {
                    pch.getInputQueue().updateObservers();
                    netLog.info("[Reconnect] Replayed current prompt for slot {}", slotIndex);
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

        netLog.info("[Reconnect] Timeout for {}. Converting to AI.", username);
        convertToAI(client.getIndex(), username);

        // Reset lobby slot
        localLobby.disconnectPlayer(client.getIndex());

        broadcast(new MessageEvent(String.format("%s did not reconnect in time. AI has taken over.", username)));
    }

    public void convertToAI(final int slotIndex, final String username) {
        final HostedMatch hostedMatch = localLobby.getHostedMatch();
        if (hostedMatch == null) { return; }
        final Game game = hostedMatch.getGame();
        if (game == null) { return; }

        for (final Player p : game.getPlayers()) {
            final IGuiGame gui = hostedMatch.getGuiForPlayer(p);
            if (gui instanceof RemoteClientGuiGame rgc && rgc.getClient().getIndex() == slotIndex) {
                final LobbyPlayerAi aiLobbyPlayer = new LobbyPlayerAi(p.getName(), null);
                final PlayerControllerAi aiCtrl = new PlayerControllerAi(game, p, aiLobbyPlayer);
                p.dangerouslySetController(aiCtrl);
                netLog.info("[Reconnect] Converted slot {} ({}) to AI controller", slotIndex, p.getName());

                // Clear InputQueue to unblock the game thread (waiting on cdlDone)
                final PlayerControllerHuman pch = findRemoteController(slotIndex);
                if (pch != null) {
                    pch.getInputQueue().clearInputs();
                    netLog.info("[Reconnect] Cleared input queue for slot {}", slotIndex);
                }
                return;
            }
        }
    }

    private class MessageHandler extends ChannelInboundHandlerAdapter {
        @Override
        public final void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
            if (msg instanceof HeartbeatEvent) {
                return; // Consumed — arrival resets IdleStateHandler read timer
            }
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
            netLog.info("Client connected to server at {}", ctx.channel().remoteAddress());
            updateLobbyState();
            super.channelActive(ctx);
        }

        @Override
        public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
            final RemoteClient client = clients.get(ctx.channel());
            if (msg instanceof LoginEvent event) {
                final String username = event.getUsername();
                client.setUsername(username);
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
                    netLog.info("[Reconnect] Channel swapped for {} (slot {})", username, disconnected.getIndex());

                    // Resume and resync
                    resumeAndResync(disconnected);

                    broadcast(new MessageEvent(String.format("%s has reconnected.", username)));
                    netLog.info("[Reconnect] Player reconnected: {}", username);
                } else {
                    // Normal login flow
                    final int index = localLobby.connectPlayer(event.getUsername(), event.getAvatarIndex(), event.getSleeveIndex());
                    if (index == -1) {
                        ctx.close();
                    } else {
                        client.setIndex(index);
                        if (index > 0) {
                            broadcast(new MessageEvent(String.format("%s joined the lobby.", event.getUsername())));
                            broadcastTo(new MessageEvent(formatAfkTimeoutMessage()),
                                    Collections.singleton(client));
                        }
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
        public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) throws Exception {
            if (evt instanceof IdleStateEvent ise && ise.state() == IdleState.READER_IDLE) {
                final RemoteClient client = clients.get(ctx.channel());
                final String name = client != null ? client.getUsername() : ctx.channel().remoteAddress().toString();
                final String msg = name + " timed out after " + HEARTBEAT_TIMEOUT_SECONDS
                    + " seconds without a network response. Closing connection.";
                netLog.warn(msg);
                broadcast(new MessageEvent(msg));
                ctx.close();
                return;
            }
            super.userEventTriggered(ctx, evt);
        }

        @Override
        public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
            final RemoteClient client = clients.remove(ctx.channel());
            if (client == null) {
                // Already handled (e.g. reconnect swapped the channel)
                super.channelInactive(ctx);
                return;
            }

            // Cancel any pending replies immediately to unblock game thread
            netLog.info("[Disconnect] Canceling pending replies for disconnected client");
            client.getReplyPool().cancelAll();

            final String username = client.getUsername();
            final int playerIndex = client.getIndex();

            netLog.info("[Disconnect] Client disconnected: index={}, username={}", playerIndex, username);

            if (isMatchActive() && client.hasValidSlot()) {
                // Game is active — enter reconnection mode
                // Pause the RemoteClientGuiGame so sends become no-ops
                pauseRemoteClientGuiGame(playerIndex);

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
                netLog.info("[Disconnect] Player disconnected mid-game: {} (slot {}). Waiting for reconnect.", username, playerIndex);
            } else if (client.hasValidSlot()) {
                // Peer completed registration but match isn't active (or slot was freed earlier)
                localLobby.disconnectPlayer(playerIndex);
                broadcast(new MessageEvent(String.format("%s left the lobby.", username)));
                broadcast(new LogoutEvent(username));
            } else {
                // Peer disconnected before completing registration — probe, crashed handshake, or rejection
                netLog.info("[Disconnect] Unregistered peer disconnected from {}",
                    ctx.channel().remoteAddress());
            }
            super.channelInactive(ctx);
        }
    }
}
