package forge.gamemodes.net.client;

import com.google.common.collect.Lists;
import forge.game.player.PlayerView;
import forge.gamemodes.net.CompatibleObjectDecoder;
import forge.gamemodes.net.CompatibleObjectEncoder;
import forge.gamemodes.net.NetworkLogConfig;
import forge.util.IHasForgeLog;
import forge.gamemodes.net.ReplyPool;
import forge.gamemodes.net.event.*;
import forge.gui.interfaces.IGuiGame;
import forge.interfaces.ILobbyListener;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class FGameClient implements IToServer, IHasForgeLog {

    public enum ReconnectState { CONNECTED, RECONNECTING, FAILED, SEAT_LOST }

    public enum DisconnectMode {
        /**
         * Simulates a hung client or a client-to-server network break. The client
         * stops sending heartbeats; the server's READER_IDLE fires, the server closes
         * the channel, and the client picks up the FIN via channelInactive and
         * reconnects. Reconnect is triggered by the channel event.
         */
        OUTBOUND,
        /**
         * Simulates a hung server or a server-to-client network break. The client
         * keeps sending heartbeats (so the server doesn't close), but nothing is
         * arriving back. The client's own READER_IDLE eventually fires and reconnect
         * is triggered by that timeout.
         */
        INBOUND
    }

    static final int HEARTBEAT_INTERVAL_SECONDS = Integer.getInteger("forge.net.heartbeatInterval", 15);
    static final int READER_IDLE_SECONDS = 45;
    private static final int[] BACKOFF_SECONDS = {1, 5, 15, 45, 60};
    private static final int RESUME_WATCH_SECONDS = 5;
    private final IGuiGame clientGui;
    private final String hostname;
    private final Integer port;
    private final String username;
    private final List<ILobbyListener> lobbyListeners = Lists.newArrayList();
    private final ReplyPool replies = new ReplyPool();
    private volatile boolean disconnectSimulated;
    private volatile Channel channel;

    private ReconnectState reconnectState = ReconnectState.CONNECTED;
    private boolean shuttingDown = false;
    private final Object reconnectLock = new Object();
    private final ScheduledExecutorService reconnectScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Forge-Reconnect");
        t.setDaemon(true);
        return t;
    });
    private volatile ScheduledFuture<?> resumeWatch;
    private volatile ScheduledFuture<?> pendingAttempt;

    public FGameClient(String username, String roomKey, IGuiGame clientGui, String hostname, int port) {
        this.username = username;
        this.clientGui = clientGui;
        this.hostname = hostname;
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    final IGuiGame getGui() {
        return clientGui;
    }
    final ReplyPool getReplyPool() {
        return replies;
    }

    public void connect() {
        final EventLoopGroup group = new NioEventLoopGroup();
        boolean attached = false;
        try {
            final Bootstrap b = new Bootstrap()
             .group(group)
             .channel(NioSocketChannel.class)
             .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(final SocketChannel ch) throws Exception {
                    final ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast(
                            new LoggingHandler(LogLevel.INFO),
                            new CompatibleObjectEncoder(null), // Client doesn't need byte tracking
                            new CompatibleObjectDecoder(9766*1024, ClassResolvers.cacheDisabled(null)),
                            new IdleStateHandler(READER_IDLE_SECONDS, HEARTBEAT_INTERVAL_SECONDS, 0, TimeUnit.SECONDS),
                            new MessageHandler(),
                            new LobbyUpdateHandler(),
                            new GameClientHandler(FGameClient.this));
                }
             });

            // Start the connection attempt.
            final Channel newChannel = b.connect(this.hostname, this.port).sync().channel();
            synchronized (reconnectLock) {
                if (shuttingDown) {
                    newChannel.close();
                    return;
                }
                channel = newChannel;
            }
            final ChannelFuture ch = newChannel.closeFuture();
            attached = true;
            // A reconnect on a brand-new channel can't inherit the prior pipeline's
            // simulated-disconnect blockers, so clear the gate that would otherwise
            // keep dropping outbound writes after /simulatedisconnect.
            disconnectSimulated = false;
            new Thread(() -> {
                try {
                    ch.sync();
                } catch (final InterruptedException e) {
                    netLog.error(e, "Client channel interrupted");
                } finally {
                    group.shutdownGracefully();
                }
            }).start();
        } catch (final InterruptedException e) {
            netLog.error(e, "Client connect interrupted");
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            if (!attached) {
                group.shutdownGracefully();
            }
        }
    }

    public void close() {
        beginShutdown();
        if (channel != null)
            channel.close();
        reconnectScheduler.shutdownNow();
        NetworkLogConfig.deactivateNetworkLogging();
    }

    @Override
    public void send(final NetEvent event) {
        if (disconnectSimulated) {
            return;
        }
        netLog.info("Client sent {}", event);
        channel.writeAndFlush(event);
    }

    /**
     * Test-only hook that silently drops traffic in one direction while keeping the
     * TCP connection open. Used by the {@code /simulatedisconnect} chat command to
     * exercise the two reconnect-detection paths. See {@link DisconnectMode}.
     */
    public void simulateDisconnect(final DisconnectMode mode) {
        netLog.info("[simulateDisconnect] mode={} - suspending network traffic", mode);
        // Pipeline modifications run on the event loop thread for atomicity
        channel.eventLoop().execute(() -> {
            if (mode == DisconnectMode.OUTBOUND) {
                // Gate user-initiated sends so they don't race past the pipeline writeBlocker
                disconnectSimulated = true;
                // Disable our own READER_IDLE so only the server's timer fires
                channel.pipeline().remove(IdleStateHandler.class);
                channel.pipeline().addFirst("writeBlocker", new ChannelOutboundHandlerAdapter() {
                    @Override
                    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
                        netLog.info("[writeBlocker] Dropped: {}", msg.getClass().getSimpleName());
                        promise.setSuccess();
                    }
                });
            } else {
                // INBOUND: drop reads, keep writes so heartbeats still flow to the server
                channel.pipeline().addFirst("readBlocker", new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) {
                        // Drop inbound messages; IdleStateEvents still propagate via userEventTriggered
                        // below so the client's READER_IDLE can still fire
                    }

                    @Override
                    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
                        if (evt instanceof IdleStateEvent) {
                            ctx.fireUserEventTriggered(evt);
                        }
                    }
                });
            }
            netLog.info("[simulateDisconnect] Pipeline modified (mode={})", mode);
        });
    }

    @Override
    public Object sendAndWait(final IdentifiableNetEvent event) {
        replies.initialize(event.getId());

        send(event);

        // Wait for reply
        return replies.get(event.getId());
    }

    List<ILobbyListener> getLobbyListeners() {
        return lobbyListeners;
    }

    public void addLobbyListener(final ILobbyListener listener) {
        lobbyListeners.add(listener);
    }

    void setGameControllers(final Iterable<PlayerView> myPlayers) {
        for (final PlayerView p : myPlayers) {
            NetGameController controller = new NetGameController(this);
            clientGui.setOriginalGameController(p, controller);
            controller.replayActiveYields();
        }
    }

    public ReconnectState getReconnectState() {
        synchronized (reconnectLock) {
            return reconnectState;
        }
    }

    public static int getTotalReconnectAttempts() {
        return BACKOFF_SECONDS.length;
    }

    /**
     * True when {@code c} is the channel currently considered live by this
     * client. Used by handlers to filter out late callbacks arriving on a
     * channel that's been closed and replaced during reconnect — without this
     * gate, a buffered {@code setGameView} on the dying channel would flip
     * state to {@code CONNECTED} even though no real reconnect has happened.
     */
    boolean isActiveChannel(final Channel c) {
        return channel != null && channel == c;
    }

    /**
     * Entry point from {@link LobbyUpdateHandler#channelInactive}. Short-circuits
     * during deliberate shutdown so the intentional close doesn't kick off a
     * retry loop. Proactively closes the old channel so a FIN reaches the server
     * ASAP — this collapses the window where the server's view of the session
     * lags behind the client's, preventing the reconnect LoginEvent from being
     * rejected as a duplicate login.
     */
    void onDisconnected() {
        synchronized (reconnectLock) {
            if (shuttingDown || reconnectState != ReconnectState.CONNECTED) {
                return;
            }
            reconnectState = ReconnectState.RECONNECTING;
        }
        if (channel != null && channel.isOpen()) {
            channel.close();
        }
        channel = null;
        notifyReconnectState(ReconnectState.RECONNECTING, 0, BACKOFF_SECONDS[0]);
        scheduleAttempt(0);
    }

    public void beginShutdown() {
        synchronized (reconnectLock) {
            shuttingDown = true;
        }
    }

    private void scheduleAttempt(final int attemptIndex) {
        if (attemptIndex >= BACKOFF_SECONDS.length) {
            transitionToFailed();
            return;
        }
        pendingAttempt = reconnectScheduler.schedule(() -> attemptReconnect(attemptIndex),
                BACKOFF_SECONDS[attemptIndex], TimeUnit.SECONDS);
    }

    private void attemptReconnect(final int attemptIndex) {
        synchronized (reconnectLock) {
            if (shuttingDown || reconnectState != ReconnectState.RECONNECTING) {
                return;
            }
        }
        netLog.info("[Reconnect] Attempt {}/{} - connecting to {}:{}",
                attemptIndex + 1, BACKOFF_SECONDS.length, hostname, port);
        try {
            connect();
            awaitResumeOrLobby(attemptIndex);
        } catch (Exception e) {
            netLog.info("[Reconnect] Attempt {} failed: {}", attemptIndex + 1, e.getMessage());
            continueOrFail(attemptIndex);
        }
    }

    private void awaitResumeOrLobby(final int attemptIndex) {
        cancelResumeWatch();
        resumeWatch = reconnectScheduler.schedule(() -> {
            netLog.info("[Reconnect] Attempt {} - no response in {}s",
                    attemptIndex + 1, RESUME_WATCH_SECONDS);
            continueOrFail(attemptIndex);
        }, RESUME_WATCH_SECONDS, TimeUnit.SECONDS);
    }

    private void cancelResumeWatch() {
        if (resumeWatch != null) {
            resumeWatch.cancel(false);
            resumeWatch = null;
        }
    }

    private void continueOrFail(final int attemptIndex) {
        synchronized (reconnectLock) {
            if (shuttingDown || reconnectState != ReconnectState.RECONNECTING) return;
        }
        final int next = attemptIndex + 1;
        if (next >= BACKOFF_SECONDS.length) {
            transitionToFailed();
            return;
        }
        notifyReconnectState(ReconnectState.RECONNECTING, next, BACKOFF_SECONDS[next]);
        scheduleAttempt(next);
    }

    private void transitionToFailed() {
        synchronized (reconnectLock) {
            if (reconnectState != ReconnectState.RECONNECTING) return;
            reconnectState = ReconnectState.FAILED;
        }
        notifyReconnectState(ReconnectState.FAILED, -1, 0);
    }

    void onResumeResponseReceived() {
        synchronized (reconnectLock) {
            if (shuttingDown || reconnectState != ReconnectState.RECONNECTING) return;
            reconnectState = ReconnectState.CONNECTED;
        }
        cancelResumeWatch();
        notifyReconnectState(ReconnectState.CONNECTED, 0, 0);
    }

    void onSeatLost() {
        synchronized (reconnectLock) {
            if (shuttingDown || reconnectState != ReconnectState.RECONNECTING) return;
            reconnectState = ReconnectState.SEAT_LOST;
        }
        cancelResumeWatch();
        notifyReconnectState(ReconnectState.SEAT_LOST, -1, 0);
    }

    /**
     * User clicked "Cancel" on the reconnect banner. Stops the retry loop and
     * any pending resume-watch, and marks the session as shutting down so a
     * subsequent {@code channelInactive} doesn't restart the loop. The caller
     * is responsible for UI teardown (hiding the banner, navigating away).
     */
    public void cancelReconnect() {
        synchronized (reconnectLock) {
            if (reconnectState != ReconnectState.RECONNECTING) return;
            shuttingDown = true;
        }
        cancelResumeWatch();
        final ScheduledFuture<?> pending = pendingAttempt;
        if (pending != null) {
            pending.cancel(false);
            pendingAttempt = null;
        }
        if (channel != null && channel.isOpen()) {
            channel.close();
        }
        channel = null;
        netLog.info("[Reconnect] User cancelled - retry loop stopped.");
    }

    /**
     * Triggered by the "Attempt Rejoin" button on the FAILED modal. Starts the
     * full backoff schedule from attempt 0 — same behaviour as the automatic
     * retry loop that kicks in on channelInactive.
     */
    public void rejoinManually() {
        synchronized (reconnectLock) {
            if (reconnectState != ReconnectState.FAILED) return;
            reconnectState = ReconnectState.RECONNECTING;
            shuttingDown = false;
        }
        notifyReconnectState(ReconnectState.RECONNECTING, 0, 0);
        reconnectScheduler.execute(() -> attemptReconnect(0));
    }

    /**
     * State-change hook for UI. Dispatches to {@link forge.gamemodes.net.NetworkGuiGame}
     * when the GUI supports it; always logs.
     */
    private void notifyReconnectState(final ReconnectState state, final int attemptIndex, final int nextDelaySeconds) {
        netLog.info("[Reconnect] state={} attempt={} nextDelay={}s", state, attemptIndex, nextDelaySeconds);
        if (clientGui instanceof forge.gamemodes.net.NetworkGuiGame networkGui) {
            networkGui.onReconnectStateChanged(state, attemptIndex, nextDelaySeconds);
        }
    }

    private class MessageHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
            if (msg instanceof HeartbeatEvent) {
                // Echo from server; its only purpose is to keep our READER_IDLE timer from firing
                return;
            }
            if (msg instanceof MessageEvent event) {
                for (final ILobbyListener listener : lobbyListeners) {
                    listener.message(event.getSource(), event.getMessage());
                }
            }
            super.channelRead(ctx, msg);
        }
    }

    private class LobbyUpdateHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
            if (msg instanceof SeatLostEvent) {
                if (isActiveChannel(ctx.channel())) {
                    onSeatLost();
                }
                return;
            }
            if (msg instanceof LobbyUpdateEvent event) {
                // Suppress lobby UI updates while reconnecting so the lobby screen doesn't flash
                // back over the in-game UI; the resume payload (setGameView) is what tells us
                // we're back in the match. Seat-lost is signaled explicitly via SeatLostEvent.
                if (isActiveChannel(ctx.channel()) && getReconnectState() == ReconnectState.RECONNECTING) {
                    return;
                }
                for (final ILobbyListener listener : lobbyListeners) {
                    listener.update(event.getState(), event.getSlot());
                }
            }
            super.channelRead(ctx, msg);
        }

        @Override
        public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) throws Exception {
            if (evt instanceof IdleStateEvent ise) {
                if (ise.state() == IdleState.WRITER_IDLE) {
                    ctx.writeAndFlush(new HeartbeatEvent());
                } else if (ise.state() == IdleState.READER_IDLE) {
                    ctx.close();
                }
            }
            super.userEventTriggered(ctx, evt);
        }

        @Override
        public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
            netLog.info("[Disconnect] Channel became inactive, notifying {} listeners", lobbyListeners.size());
            netLog.info("[Disconnect] Remote address was: {}", ctx.channel().remoteAddress());
            if (isActiveChannel(ctx.channel())) {
                onDisconnected();
                final boolean notifyListeners;
                synchronized (reconnectLock) {
                    notifyListeners = !shuttingDown && reconnectState != ReconnectState.RECONNECTING;
                }
                if (notifyListeners) {
                    for (final ILobbyListener listener : lobbyListeners) {
                        listener.close();
                    }
                }
            }
            super.channelInactive(ctx);
        }
    }
}
