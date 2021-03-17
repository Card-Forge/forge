package forge.gamemodes.net.server;

import forge.gamemodes.match.LobbySlot;
import forge.gamemodes.match.LobbySlotType;
import forge.gamemodes.net.CompatibleObjectDecoder;
import forge.gamemodes.net.CompatibleObjectEncoder;
import forge.gamemodes.net.event.LobbyUpdateEvent;
import forge.gamemodes.net.event.LoginEvent;
import forge.gamemodes.net.event.LogoutEvent;
import forge.gamemodes.net.event.MessageEvent;
import forge.gamemodes.net.event.NetEvent;
import forge.gamemodes.net.event.UpdateLobbyPlayerEvent;
import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiGame;
import forge.interfaces.IGameController;
import forge.interfaces.ILobbyListener;
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
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.support.igd.PortMappingListener;
import org.fourthline.cling.support.model.PortMapping;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

public final class FServerManager {
    private static FServerManager instance = null;

    private byte[] externalAddress = new byte[]{8,8,8,8};
    private boolean isHosting = false;
    private EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private EventLoopGroup workerGroup = new NioEventLoopGroup();
    private UpnpService upnpService = null;
    private final Map<Channel, RemoteClient> clients = Maps.newTreeMap();
    private ServerGameLobby localLobby;
    private ILobbyListener lobbyListener;
    private final Thread shutdownHook = new Thread(new Runnable() {
        @Override public final void run() {
            if (isHosting()) {
                stopServer(false);
            }
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
        try {
            final ServerBootstrap b = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public final void initChannel(final SocketChannel ch) throws Exception {
                            final ChannelPipeline p = ch.pipeline();
                            p.addLast(
                                    new CompatibleObjectEncoder(),
                                    new CompatibleObjectDecoder(9766*1024, ClassResolvers.cacheDisabled(null)),
                                    new MessageHandler(),
                                    new RegisterClientHandler(),
                                    new LobbyInputHandler(),
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
            mapNatPort(port);
            Runtime.getRuntime().addShutdownHook(shutdownHook);
            isHosting = true;
        } catch (final InterruptedException e) {
            e.printStackTrace();
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
        broadcastTo(event, Iterables.filter(clients.values(), Predicates.not(Predicates.in(notTo))));
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
        if (this.localLobby != null) {
            if (this.localLobby.getSlot(0) != null) {
                this.localLobby.getSlot(0).setIsReady(false);
                updateLobbyState();
            }
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
    }

    public IGuiGame getGui(final int index) {
        final LobbySlot slot = localLobby.getSlot(index);
        final LobbySlotType type = slot.getType();
        if (type == LobbySlotType.LOCAL) {
            return GuiBase.getInterface().getNewGuiGame();
        } else if (type == LobbySlotType.REMOTE) {
            for (final RemoteClient client : clients.values()) {
                if (client.getIndex() == index) {
                    return new NetGuiGame(client);
                }
            }
        }
        return null;
    }

    // inspired by:
    //  https://stackoverflow.com/a/34873630
    //  https://stackoverflow.com/a/901943
    private String getRoutableAddress(boolean preferIpv4, boolean preferIPv6) throws SocketException, UnknownHostException {
        try (DatagramSocket s = new DatagramSocket()) {
            s.connect(InetAddress.getByAddress(this.externalAddress), 0);
            NetworkInterface n = NetworkInterface.getByInetAddress(s.getLocalAddress());
            Enumeration<InetAddress> en = n.getInetAddresses();
            while (en.hasMoreElements()) {
                InetAddress addr = en.nextElement();
                if (addr instanceof Inet4Address) {
                    if (preferIPv6) {
                        continue;
                    }
                    return addr.getHostAddress();
                }
                if (addr instanceof Inet6Address) {
                    if (preferIpv4) {
                        continue;
                    }
                    return addr.getHostAddress();
                }
            }
        }
        return null;
    }

    public String getLocalAddress() {
        try {
            return getRoutableAddress(true, false);
        }
        catch (final Exception e) {
            e.printStackTrace();
            return "localhost";
        }
    }

    public static String getExternalAddress() {
        BufferedReader in = null;
        try {
            URL whatismyip = new URL("http://checkip.amazonaws.com");
            in = new BufferedReader(new InputStreamReader(
                    whatismyip.openStream()));
            String ip = in.readLine();
            return ip;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private void mapNatPort(final int port) {
        final String localAddress = getLocalAddress();
        final PortMapping portMapping = new PortMapping(port, localAddress, PortMapping.Protocol.TCP, "Forge");
        if (upnpService != null) {
            // Safeguard shutdown call, to prevent lingering port mappings
            upnpService.shutdown();
        }
        try {
            upnpService = new UpnpServiceImpl(new PortMappingListener(portMapping));
            upnpService.getControlPoint().search();
        }catch (Error e){
            e.printStackTrace();
        }
    }

    private class MessageHandler extends ChannelInboundHandlerAdapter {
        @Override
        public final void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
            final RemoteClient client = clients.get(ctx.channel());
            if (msg instanceof MessageEvent) {
                broadcast(new MessageEvent(client.getUsername(), ((MessageEvent) msg).getMessage()));
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
                final String username = ((LoginEvent) msg).getUsername();
                client.setUsername(username);
                broadcast(new MessageEvent(String.format("%s joined the room", username)));
                updateLobbyState();
            } else if (msg instanceof UpdateLobbyPlayerEvent) {
                localLobby.applyToSlot(client.getIndex(), (UpdateLobbyPlayerEvent) msg);
            }
            super.channelRead(ctx, msg);
        }
    }

    private class LobbyInputHandler extends ChannelInboundHandlerAdapter {
        @Override public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
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
            final String username = client.getUsername();
            localLobby.disconnectPlayer(client.getIndex());
            broadcast(new MessageEvent(String.format("%s left the room", username)));
            broadcast(new LogoutEvent(username));
            super.channelInactive(ctx);
        }
    }

}
