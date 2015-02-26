package forge.net;

import forge.game.GameRules;
import forge.net.game.LoginEvent;
import forge.net.game.LogoutEvent;
import forge.net.game.MessageEvent;
import forge.net.game.NetEvent;
import forge.net.game.RegisterDeckEvent;
import forge.net.game.client.ILobbyListener;
import forge.net.game.server.RemoteClient;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public final class FServerManager {
    private static FServerManager instance = null;

    private final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();
    private final Map<Integer, NetGame> games = Maps.newTreeMap();
    private int id = 0;
    private final Map<Channel, RemoteClient> clients = Maps.newTreeMap();
    private final List<ILobbyListener> lobbyListeners = Lists.newArrayListWithExpectedSize(1);

    private FServerManager() {
    }

    private int nextId() {
        return id++;
    }

    public static FServerManager getInstance() {
        if (instance == null) {
            instance = new FServerManager();
        }
        return instance;
    }

    public void startServer(final int port) {
        try {
            final ServerBootstrap b = new ServerBootstrap()
             .group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline p = ch.pipeline();
                    p.addLast(
                            new ObjectEncoder(),
                            new ObjectDecoder(ClassResolvers.cacheDisabled(null)),
                            new MessageHandler(),
                            new RegisterClientHandler(),
                            new ToLobbyListenersHandler(),
                            new DeregisterClientHandler(),
                            new GameServerHandler());
                }
             });

            // Bind and start to accept incoming connections.
            final ChannelFuture ch = b.bind(port).sync().channel().closeFuture();
            new Thread(new Runnable() {
                @Override public void run() {
                    try {
                        ch.sync();
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        stopServer();
                    }
                    
                }
            }).start();
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void stopServer() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    public void broadcast(final NetEvent event) {
        for (final RemoteClient client : clients.values()) {
            client.send(event);
        }
    }

    public void registerLobbyListener(final ILobbyListener lobbyListener) {
        lobbyListeners.add(lobbyListener);
    }

    public NetGame hostGame(final GameRules rules) {
        final int id = nextId();
        final NetGame game = new NetGame(rules);
        games.put(id, game);
        return game;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        stopServer();
    }

    private class MessageHandler extends ChannelInboundHandlerAdapter {
        @Override public final void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
            final RemoteClient client = clients.get(ctx.channel());
            if (msg instanceof MessageEvent) {
                broadcast(new MessageEvent(client.getUsername(), ((MessageEvent) msg).getMessage()));
            }
            super.channelRead(ctx, msg);
        }
    }

    private class GameServerHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
            System.out.println("Server received: " + msg);
        }

        @Override
        public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }

    private class RegisterClientHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelActive(final ChannelHandlerContext ctx) throws Exception {
            final RemoteClient client = new RemoteClient(ctx.channel());
            clients.put(ctx.channel(), client);
            games.get(0).addClient(client);
            System.out.println("User connected to server at " + ctx.channel().remoteAddress());
            super.channelActive(ctx);
        }

        @Override
        public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
            final RemoteClient client = clients.get(ctx.channel());
            if (msg instanceof LoginEvent) {
                client.setUsername(((LoginEvent) msg).getUsername());
            } else if (msg instanceof RegisterDeckEvent) {
                games.get(0).registerDeck(client, ((RegisterDeckEvent) msg).getDeck());
            }
            super.channelRead(ctx, msg);
        }
    }

    private class ToLobbyListenersHandler extends ChannelInboundHandlerAdapter {
        @Override public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
            final RemoteClient client = clients.get(ctx.channel());
            if (msg instanceof LoginEvent) {
                final LoginEvent event = (LoginEvent) msg;
                for (final ILobbyListener lobbyListener : lobbyListeners) {
                    lobbyListener.login(client);
                }
                broadcast(event);
            } else if (msg instanceof MessageEvent) {
                final MessageEvent event = (MessageEvent) msg;
                for (final ILobbyListener lobbyListener : lobbyListeners) {
                    lobbyListener.message(client.getUsername(), event.getMessage());
                }
                broadcast(event);
            }
            super.channelRead(ctx, msg);
        }

        @Override public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
            final RemoteClient client = clients.get(ctx.channel());
            for (final ILobbyListener lobbyListener : lobbyListeners) {
                lobbyListener.logout(client);
            }
            super.channelInactive(ctx);
        }
    }

    private class DeregisterClientHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            final RemoteClient client = clients.remove(ctx.channel());
            // TODO remove client from games
            broadcast(new LogoutEvent(client.getUsername()));
            super.channelInactive(ctx);
        }
    }
}
