package forge.net;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

import java.util.List;

import com.google.common.collect.Lists;

import forge.game.GameView;
import forge.game.player.PlayerView;
import forge.interfaces.IGuiGame;
import forge.model.FModel;
import forge.net.game.GuiGameEvent;
import forge.net.game.LobbyUpdateEvent;
import forge.net.game.LoginEvent;
import forge.net.game.MessageEvent;
import forge.net.game.NetEvent;
import forge.net.game.client.ILobbyListener;
import forge.net.game.client.IToServer;
import forge.properties.ForgePreferences.FPref;

public class FGameClient implements IToServer {
    private final IGuiGame clientGui;
    public FGameClient(final String username, final String roomKey, final IGuiGame clientGui) {
        this.clientGui = clientGui;
    }

    private final List<ILobbyListener> lobbyListeners = Lists.newArrayList();

    static final int SIZE = Integer.parseInt(System.getProperty("size", "256"));

    private Channel channel;
    public void connect(final String host, final int port) {
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
                            new ObjectEncoder(),
                            new ObjectDecoder(ClassResolvers.cacheDisabled(null)),
                            new MessageHandler(),
                            new LobbyUpdateHandler(),
                            new GameClientHandler());
                }
             });

            // Start the connection attempt.
            channel = b.connect(host, port).sync().channel();
            final ChannelFuture ch = channel.closeFuture();
            new Thread(new Runnable() {
                @Override public void run() {
                    try {
                        ch.sync();
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        group.shutdownGracefully();
                    }
                }
            }).start();
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void send(final NetEvent event) {
        channel.writeAndFlush(event);
    }

    public void addLobbyListener(final ILobbyListener listener) {
        lobbyListeners.add(listener);
    }

    private class GameClientHandler extends ChannelInboundHandlerAdapter {
        /**
         * Creates a client-side handler.
         */
        public GameClientHandler() {
        }

        @Override
        public void channelActive(final ChannelHandlerContext ctx) {
            // Don't use send here, as this.channel is not yet set!
            ctx.channel().writeAndFlush(new LoginEvent(FModel.getPreferences().getPref(FPref.PLAYER_NAME)));
        }

        @SuppressWarnings("unchecked")
        @Override
        public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
            System.out.println("Client received: " + msg);
            if (msg instanceof GuiGameEvent) {
                final GuiGameEvent event = (GuiGameEvent) msg;
                switch (event.getMethod()) {
                case "setGameView":
                    clientGui.setGameView((GameView) event.getObject());
                    break;
                case "openView":
                    clientGui.openView((Iterable<PlayerView>) event.getObjects());
                default:
                    break;
                }
            }
        }

        @Override
        public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
            cause.printStackTrace();
            ctx.close();
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
                    listener.update(((LobbyUpdateEvent) msg).getState());
                }
            }
            super.channelRead(ctx, msg);
        }
    }
}
