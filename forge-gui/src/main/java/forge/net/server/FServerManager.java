package forge.net.server;

import forge.FThreads;
import forge.GuiBase;
import forge.LobbyPlayer;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbilityView;
import forge.interfaces.IGameController;
import forge.interfaces.IGuiGame;
import forge.interfaces.ILobbyListener;
import forge.match.LobbySlot;
import forge.match.LobbySlotType;
import forge.match.NextGameDecision;
import forge.net.event.GuiGameEvent;
import forge.net.event.LobbyUpdateEvent;
import forge.net.event.LoginEvent;
import forge.net.event.LogoutEvent;
import forge.net.event.MessageEvent;
import forge.net.event.NetEvent;
import forge.net.event.ReplyEvent;
import forge.net.event.UpdateLobbyPlayerEvent;
import forge.properties.ForgeConstants;
import forge.util.ITriggerEvent;
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

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.log4j.PropertyConfigurator;
import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.support.igd.PortMappingListener;
import org.fourthline.cling.support.model.PortMapping;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

public final class FServerManager {
    private static FServerManager instance = null;

    private boolean isHosting = false;
    private final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();
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

    /**
     * Get the singleton instance of {@link FServerManager}.
     * 
     * @return the singleton FServerManager.
     */
    public static FServerManager getInstance() {
        if (instance == null) {
            PropertyConfigurator.configure(ForgeConstants.ASSETS_DIR + "/src/main/resources/log4jConfig.config");
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
                @Override public final void initChannel(final SocketChannel ch) {
                    final ChannelPipeline p = ch.pipeline();
                    p.addLast(
                            new ObjectEncoder(),
                            new ObjectDecoder(ClassResolvers.cacheDisabled(null)),
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
    }

    public boolean isHosting() {
        return isHosting;
    }

    public void broadcast(final NetEvent event) {
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
            event.updateForClient(client);
            client.send(event);
        }
    }

    public void setLobby(final ServerGameLobby lobby) {
        this.localLobby = lobby;
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

    private void mapNatPort(final int port) {
        final String localAddress;
        try {
            localAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (final UnknownHostException e) {
            throw new RuntimeException(e);
        }

        final PortMapping portMapping = new PortMapping(port, localAddress, PortMapping.Protocol.TCP, "Forge");
        if (upnpService != null) {
            // Safeguard shutdown call, to prevent lingering port mappings
            upnpService.shutdown();
        }
        upnpService = new UpnpServiceImpl(new PortMappingListener(portMapping));
        upnpService.getControlPoint().search();
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
        @SuppressWarnings("unchecked")
        @Override
        public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
            System.out.println("Server received: " + msg);
            final RemoteClient client = clients.get(ctx.channel());
            if (msg instanceof ReplyEvent) {
                client.setReply(((ReplyEvent) msg).getIndex(), ((ReplyEvent) msg).getReply());
            } else if (msg instanceof GuiGameEvent) {
                final GuiGameEvent event = (GuiGameEvent) msg;
                final GameView gameView = localLobby.getGameView();
                final IGameController controller = localLobby.getController(client.getIndex());
                final Object[] args = event.getObjects();

                FThreads.invokeInBackgroundThread(new Runnable() {
                    @Override public final void run() {
                        Serializable reply = null;
                        boolean doReply = false;

                        switch (event.getMethod()) {
                        // From GameController
                        case "useMana":
                            controller.useMana((byte) args[0]);
                            break;
                        case "tryUndoLastAction":
                            reply = controller.tryUndoLastAction();
                            doReply = true;
                            break;
                        case "selectPlayer":
                            controller.selectPlayer((PlayerView) args[0], (ITriggerEvent) args[1]);
                            break;
                        case "selectCard":
                            reply = controller.selectCard((CardView) args[0], (List<CardView>) args[1], (ITriggerEvent) args[2]);
                            doReply = true;
                            break;
                        case "selectButtonOk":
                            controller.selectButtonOk();
                            break;
                        case "selectButtonCancel":
                            controller.selectButtonCancel();
                            break;
                        case "selectAbility":
                            controller.selectAbility((SpellAbilityView) args[0]);
                            break;
                        case "passPriorityUntilEndOfTurn":
                            reply = controller.passPriorityUntilEndOfTurn();
                            doReply = true;
                            break;
                        case "passPriority":
                            reply = controller.passPriority();
                            doReply = true;
                            break;
                        case "nextGameDecision":
                            controller.nextGameDecision((NextGameDecision) args[0]);
                            break;
                        case "mayLookAtAllCards":
                            reply = controller.mayLookAtAllCards();
                            doReply = true;
                            break;
                        case "getActivateDescription":
                            reply = controller.getActivateDescription((CardView) args[0]);
                            doReply = true;
                            break;
                        case "concede":
                            controller.concede();
                            break;
                        case "alphaStrike":
                            controller.alphaStrike();
                            break;
                        // From GameView
                        case "getPlayers":
                            reply = (Serializable) gameView.getPlayers();
                            doReply = true;
                            break;
                        case "getTitle":
                            reply = gameView.getTitle();
                            doReply = true;
                            break;
                        case "isCommander":
                            reply = gameView.isCommander();
                            doReply = true;
                            break;
                        case "getGameType":
                            reply = gameView.getGameType();
                            doReply = true;
                            break;
                        case "getPoisonCountersToLose":
                            reply = gameView.getPoisonCountersToLose();
                            doReply = true;
                            break;
                        case "getNumGamesInMatch":
                            reply = gameView.getNumGamesInMatch();
                            doReply = true;
                            break;
                        case "getTurn":
                            reply = gameView.getTurn();
                            doReply = true;
                            break;
                        case "getPhase":
                            reply = gameView.getPhase();
                            doReply = true;
                            break;
                        case "getPlayerTurn":
                            reply = gameView.getPlayerTurn();
                            doReply = true;
                            break;
                        case "getStack":
                            reply = (Serializable) gameView.getStack();
                            doReply = true;
                            break;
                        case "peekStack":
                            reply = gameView.peekStack();
                            doReply = true;
                            break;
                        case "getStormCount":
                            reply = gameView.getStormCount();
                            doReply = true;
                            break;
                        case "isFirstGameInMatch":
                            reply = gameView.isFirstGameInMatch();
                            doReply = true;
                            break;
                        case "getNumPlayedGamesInMatch":
                            reply = gameView.getNumPlayedGamesInMatch();
                            doReply = true;
                            break;
                        case "isGameOver":
                            reply = gameView.isGameOver();
                            doReply = true;
                            break;
                        case "isMatchOver":
                            reply = gameView.isMatchOver();
                            doReply = true;
                            break;
                        case "getWinningTeam":
                            reply = gameView.getWinningTeam();
                            doReply = true;
                            break;
                        case "getGameLog":
                            reply = gameView.getGameLog();
                            doReply = true;
                            break;
                        case "getCombat":
                            reply = gameView.getCombat();
                            doReply = true;
                            break;
                        case "isMatchWonBy":
                            reply = gameView.isMatchWonBy((LobbyPlayer) args[0]);
                            doReply = true;
                            break;
                        case "getOutcomesOfMatch":
                            reply = (Serializable) gameView.getOutcomesOfMatch();
                            doReply = true;
                            break;
                        // TODO case "getWinningPlayer":
                        case "isWinner":
                            reply = gameView.isWinner((LobbyPlayer) args[0]);
                            doReply = true;
                            break;
                        case "getGamesWonBy":
                            reply = gameView.getGamesWonBy((LobbyPlayer) args[0]);
                            doReply = true;
                            break;
                        case "getDeck":
                            reply = gameView.getDeck((String) args[0]);
                            doReply = true;
                            break;
                        case "getAnteResult":
                            reply = gameView.getAnteResult((PlayerView) args[0]);
                            doReply = true;
                            break;
                        default:
                            System.err.println(String.format("Unknown incoming client command %s", event.getMethod()));
                            break;
                        }

                        if (doReply) {
                            client.send(new ReplyEvent(event.getId(), reply));
                        }
                    }
                });
            }
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
                final int index = localLobby.connectPlayer(event.getUsername(), event.getAvatarIndex());
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
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            final RemoteClient client = clients.remove(ctx.channel());
            localLobby.disconnectPlayer(client.getIndex());
            broadcast(new LogoutEvent(client.getUsername()));
            super.channelInactive(ctx);
        }
    }

}
