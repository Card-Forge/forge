package forge.gamemodes.net.client;

import com.google.common.collect.Lists;
import forge.game.player.PlayerView;
import forge.gamemodes.net.CompatibleObjectDecoder;
import forge.gamemodes.net.CompatibleObjectEncoder;
import forge.gamemodes.net.ReplyPool;
import forge.gamemodes.net.event.IdentifiableNetEvent;
import forge.gamemodes.net.event.LobbyUpdateEvent;
import forge.gamemodes.net.event.MessageEvent;
import forge.gamemodes.net.event.NetEvent;
import forge.gui.interfaces.IGuiGame;
import forge.interfaces.ILobbyListener;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;


import java.util.List;
import java.util.concurrent.TimeoutException;

public class FGameClient implements IToServer {
    private final IGuiGame clientGui;
    private final String hostname;
    private final Integer port;
    private final String username;
    private final List<ILobbyListener> lobbyListeners = Lists.newArrayList();
    private final ReplyPool replies = new ReplyPool();
    private Channel channel;

    // Session info for reconnection support
    private String sessionId;
    private String sessionToken;
    private boolean wasConnected = false;
    private boolean isReconnecting = false;

    public FGameClient(String username, String roomKey, IGuiGame clientGui, String hostname, int port) {
        this.username = username;
        this.clientGui = clientGui;
        this.hostname = hostname;
        this.port = port;
    }

    /**
     * Get the username for this client.
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Store session credentials for reconnection.
     * Called when receiving session info from server.
     * @param sessionId the session identifier
     * @param token the session token
     */
    public void setSessionCredentials(String sessionId, String token) {
        this.sessionId = sessionId;
        this.sessionToken = token;
        this.wasConnected = true;
    }

    /**
     * Get the session ID.
     * @return the session ID, or null if not set
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Get the session token.
     * @return the session token, or null if not set
     */
    public String getSessionToken() {
        return sessionToken;
    }

    /**
     * Check if this client has reconnection credentials.
     * @return true if credentials are available
     */
    public boolean canReconnect() {
        return wasConnected && sessionId != null && sessionToken != null;
    }

    /**
     * Check if this is a reconnection attempt.
     * @return true if currently reconnecting
     */
    public boolean isReconnecting() {
        return isReconnecting;
    }

    /**
     * Attempt to reconnect to the server.
     * @return true if reconnection was initiated successfully
     */
    public boolean reconnect() {
        if (!canReconnect()) {
            return false;
        }

        try {
            isReconnecting = true;
            connect();
            // The GameClientHandler.channelActive() will send the ReconnectRequestEvent
            return true;
        } catch (Exception e) {
            System.err.println("Reconnection failed: " + e.getMessage());
            isReconnecting = false;
            return false;
        }
    }

    /**
     * Reset the reconnection flag after reconnection is complete.
     */
    void clearReconnecting() {
        isReconnecting = false;
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
                            new CompatibleObjectEncoder(null), // Client doesn't need byte tracking
                            new CompatibleObjectDecoder(9766*1024, ClassResolvers.cacheDisabled(null)),
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
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                } finally {
                    group.shutdownGracefully();
                }
            }).start();
        } catch (final InterruptedException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public void close() {
        if (channel != null)
            channel.close();
    }

    @Override
    public void send(final NetEvent event) {
        System.out.println("Client sent " + event);
        channel.writeAndFlush(event);
    }

    @Override
    public Object sendAndWait(final IdentifiableNetEvent event) throws TimeoutException {
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
            if (msg instanceof MessageEvent) {
                final MessageEvent event = (MessageEvent) msg;
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
            if (msg instanceof LobbyUpdateEvent) {
                for (final ILobbyListener listener : lobbyListeners) {
                    final LobbyUpdateEvent event = (LobbyUpdateEvent) msg;
                    listener.update(event.getState(), event.getSlot());
                }
            }
            super.channelRead(ctx, msg);
        }

        @Override
        public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
            for (final ILobbyListener listener : lobbyListeners) {
                listener.close();
            }
            super.channelInactive(ctx);
        }
    }
}
