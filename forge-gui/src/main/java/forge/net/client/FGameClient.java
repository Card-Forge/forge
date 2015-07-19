package forge.net.client;

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
import java.util.concurrent.TimeoutException;

import com.google.common.collect.Lists;

import forge.game.player.PlayerView;
import forge.interfaces.IGuiGame;
import forge.interfaces.ILobbyListener;
import forge.net.ReplyPool;
import forge.net.event.IdentifiableNetEvent;
import forge.net.event.LobbyUpdateEvent;
import forge.net.event.MessageEvent;
import forge.net.event.NetEvent;

public class FGameClient implements IToServer {

    private final IGuiGame clientGui;
    private final List<ILobbyListener> lobbyListeners = Lists.newArrayList();
    private final ReplyPool replies = new ReplyPool();
    private Channel channel;

    public FGameClient(final String username, final String roomKey, final IGuiGame clientGui) {
        this.clientGui = clientGui;
    }

    final IGuiGame getGui() {
        return clientGui;
    }
    final ReplyPool getReplyPool() {
        return replies;
    }

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
                            new GameClientHandler(FGameClient.this));
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

    public void close() {
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
