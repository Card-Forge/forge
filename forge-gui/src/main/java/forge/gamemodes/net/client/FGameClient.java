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
import java.util.concurrent.TimeUnit;

public class FGameClient implements IToServer, IHasForgeLog {

    static final int HEARTBEAT_INTERVAL_SECONDS = Integer.getInteger("forge.net.heartbeatInterval", 15);
    private final IGuiGame clientGui;
    private final String hostname;
    private final Integer port;
    private final String username;
    private final List<ILobbyListener> lobbyListeners = Lists.newArrayList();
    private final ReplyPool replies = new ReplyPool();
    private volatile boolean disconnectSimulated;
    private Channel channel;

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
                            new IdleStateHandler(0, HEARTBEAT_INTERVAL_SECONDS, 0, TimeUnit.SECONDS),
                            new MessageHandler(),
                            new LobbyUpdateHandler(),
                            new GameClientHandler(FGameClient.this));
                }
             });

            // Start the connection attempt.
            channel = b.connect(this.hostname, this.port).sync().channel();
            final ChannelFuture ch = channel.closeFuture();
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
        }
    }

    public void close() {
        if (channel != null)
            channel.close();
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
     * Simulate a crashed client: stop all network writes and heartbeats
     * while keeping the TCP connection open. The server's idle timeout
     * will detect the silence and close the connection.
     */
    public void simulateDisconnect() {
        netLog.info("[simulateDisconnect] Suspending all network writes.");
        disconnectSimulated = true;
        // Remove the IdleStateHandler to stop heartbeats, and add an outbound
        // handler that drops ALL writes (including game replies that bypass
        // send()). The TCP connection stays open but completely silent.
        // Both pipeline modifications run on the event loop thread for atomicity.
        channel.eventLoop().execute(() -> {
            channel.pipeline().remove(IdleStateHandler.class);
            channel.pipeline().addFirst("writeBlocker", new ChannelOutboundHandlerAdapter() {
                @Override
                public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
                    netLog.info("[writeBlocker] Dropped: {}", msg.getClass().getSimpleName());
                    promise.setSuccess();
                }
            });
            netLog.info("[simulateDisconnect] Pipeline modified: IdleStateHandler removed, writeBlocker added.");
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
            clientGui.setOriginalGameController(p, new NetGameController(this));
        }
    }

    private class MessageHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
            if (msg instanceof MessageEvent event) {
                for (final ILobbyListener listener : lobbyListeners) {
                    listener.message(event.getSource(), event.getMessage(), event.getType());
                }
            }
            super.channelRead(ctx, msg);
        }
    }

    private class LobbyUpdateHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
            if (msg instanceof LobbyUpdateEvent event) {
                for (final ILobbyListener listener : lobbyListeners) {
                    listener.update(event.getState(), event.getSlot());
                }
            }
            super.channelRead(ctx, msg);
        }

        @Override
        public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) throws Exception {
            if (evt instanceof IdleStateEvent ise && ise.state() == IdleState.WRITER_IDLE) {
                ctx.writeAndFlush(new HeartbeatEvent());
            }
            super.userEventTriggered(ctx, evt);
        }

        @Override
        public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
            netLog.info("[Disconnect] Channel became inactive, notifying {} listeners", lobbyListeners.size());
            netLog.info("[Disconnect] Remote address was: {}", ctx.channel().remoteAddress());
            for (final ILobbyListener listener : lobbyListeners) {
                listener.close();
            }
            super.channelInactive(ctx);
        }
    }
}
