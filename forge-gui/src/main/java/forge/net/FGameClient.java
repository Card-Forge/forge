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

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import forge.FThreads;
import forge.UiCommand;
import forge.assets.FSkinProp;
import forge.deck.CardPool;
import forge.game.GameEntityView;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.phase.PhaseType;
import forge.game.player.DelayedReveal;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbilityView;
import forge.game.zone.ZoneType;
import forge.interfaces.IButton;
import forge.interfaces.IGuiGame;
import forge.interfaces.ILobbyListener;
import forge.match.MatchButtonType;
import forge.model.FModel;
import forge.net.game.GuiGameEvent;
import forge.net.game.IdentifiableNetEvent;
import forge.net.game.LoginEvent;
import forge.net.game.MessageEvent;
import forge.net.game.NetEvent;
import forge.net.game.ReplyEvent;
import forge.net.game.client.IToServer;
import forge.player.PlayerZoneUpdates;
import forge.properties.ForgePreferences.FPref;
import forge.trackable.TrackableCollection;
import forge.util.ITriggerEvent;

public class FGameClient implements IToServer {
    private final IGuiGame clientGui;
    public FGameClient(final String username, final String roomKey, final IGuiGame clientGui) {
        this.clientGui = clientGui;
    }

    private final List<ILobbyListener> lobbyListeners = Lists.newArrayList();
    private final ReplyPool replies = new ReplyPool();

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

    private void setGameControllers(final Iterable<PlayerView> myPlayers) {
        for (final PlayerView p : myPlayers) {
            clientGui.setGameController(p, new NetGameController(this));
        }
    }

    private class GameClientHandler extends ChannelInboundHandlerAdapter {
        /**
         * Creates a client-side handler.
         */
        public GameClientHandler() {
        }

        @Override
        public void channelActive(final ChannelHandlerContext ctx) {
            // Don't use send() here, as this.channel is not yet set!
            ctx.channel().writeAndFlush(new LoginEvent(FModel.getPreferences().getPref(FPref.PLAYER_NAME), Integer.parseInt(FModel.getPreferences().getPref(FPref.UI_AVATARS).split(",")[0])));
        }

        @SuppressWarnings("unchecked")
        @Override
        public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
            System.out.println("Client received: " + msg);
            if (msg instanceof ReplyEvent) {
                final ReplyEvent event = (ReplyEvent) msg;
                replies.complete(event.getIndex(), event.getReply());
            } else if (msg instanceof GuiGameEvent) {
                final GuiGameEvent event = (GuiGameEvent) msg;
                final String method = event.getMethod();
                final Object[] args = event.getObjects();
                Serializable reply = null;
                boolean doReply = false;

                final IButton btn;
                if (method.startsWith("btn_") && args.length >= 2 && args[0] instanceof PlayerView && args[1] instanceof MatchButtonType) {
                    btn = args[1] == MatchButtonType.OK ? clientGui.getBtnOK((PlayerView) args[0]) : clientGui.getBtnCancel((PlayerView) args[0]);
                } else {
                    btn = null;
                }

                switch (method) {
                case "setGameView":
                    clientGui.setGameView((GameView) args[0]);
                    break;
                case "openView":
                    final TrackableCollection<PlayerView> myPlayers = (TrackableCollection<PlayerView>) args[0];
                    setGameControllers(myPlayers);
                    FThreads.invokeInEdtNowOrLater(new Runnable() {
                        @Override public final void run() {
                            //clientGui.setGameView(new NetGameView(FGameClient.this));
                            clientGui.openView(myPlayers);
                        }
                    });
                    break;
                case "afterGameEnd":
                    clientGui.afterGameEnd();
                    break;
                case "showCombat":
                    clientGui.showCombat();
                    break;
                case "showPromptMessage":
                    clientGui.showPromptMessage((PlayerView) args[0], (String) args[1]);
                    break;
                case "stopAtPhase":
                    reply = clientGui.stopAtPhase((PlayerView) args[0], (PhaseType) args[1]);
                    doReply = true;
                    break;
                case "focusButton":
                    clientGui.focusButton((MatchButtonType) args[0]);
                    break;
                case "flashIncorrectAction":
                    clientGui.flashIncorrectAction();
                    break;
                case "updatePhase":
                    clientGui.updatePhase();
                    break;
                case "updateTurn":
                    FThreads.invokeInEdtNowOrLater(new Runnable() {
                        @Override public final void run() {
                            clientGui.updateTurn((PlayerView) args[0]);
                        }
                    });
                    break;
                case "udpdatePlayerControl":
                    clientGui.updatePlayerControl();
                    break;
                case "enableOverlay":
                    clientGui.enableOverlay();
                    break;
                case "disbleOverlay":
                    clientGui.disableOverlay();
                    break;
                case "finishGame":
                    clientGui.finishGame();
                    break;
                case "showManaPool":
                    clientGui.showManaPool((PlayerView) args[0]);
                    break;
                case "hideManaPool":
                    clientGui.hideManaPool((PlayerView) args[0], args[1]);
                    break;
                case "updateStack":
                    clientGui.updateStack();
                    break;
                case "updateZones":
                    FThreads.invokeInEdtNowOrLater(new Runnable() {
                        @Override public final void run() {
                            clientGui.updateZones((PlayerZoneUpdates) args[0]);
                        }
                    });
                    break;
                case "updateSingleCard":
                    clientGui.updateSingleCard((CardView) args[0]);
                    break;
                case "updateManaPool":
                    clientGui.updateManaPool((Iterable<PlayerView>) args[0]);
                    break;
                case "updateLives":
                    clientGui.updateLives((Iterable<PlayerView>) args[0]);
                    break;
                case "setPanelSelection":
                    clientGui.setPanelSelection((CardView) args[0]);
                    break;
                case "getAbilityToPlay":
                    reply = clientGui.getAbilityToPlay((List<SpellAbilityView>) args[0], (ITriggerEvent) args[1]);
                    doReply = true;
                    break;
                case "assignDamage":
                    reply = (Serializable) clientGui.assignDamage((CardView) args[0], (List<CardView>) args[1], (int) args[2], (GameEntityView) args[3], (boolean) args[4]);
                    doReply = true;
                    break;
                case "message":
                    clientGui.message((String) args[0], (String) args[1]);
                    break;
                case "showErrorDialog":
                    clientGui.showErrorDialog((String) args[0], (String) args[1]);
                    break;
                case "showConfirmDialog":
                    reply = clientGui.showConfirmDialog((String) args[0], (String) args[1], (String) args[2], (String) args[3], (boolean) args[4]);
                    doReply = true;
                    break;
                case "showOptionDialog":
                    reply = clientGui.showOptionDialog((String) args[0], (String) args[1], (FSkinProp) args[2], (String[]) args[3], (int) args[4]);
                    doReply = true;
                    break;
                case "showCardOptionDialog":
                    reply = clientGui.showCardOptionDialog((CardView) args[0], (String) args[1], (String) args[2], (FSkinProp) args[3], (String[]) args[4], (int) args[5]);
                    doReply = true;
                    break;
                case "showInputDialog":
                    reply = clientGui.showInputDialog((String) args[0], (String) args[1], (FSkinProp) args[2], (String) args[3], (String[]) args[4]);
                    doReply = true;
                    break;
                case "confirm":
                    reply = clientGui.confirm((CardView) args[0], (String) args[1], (boolean) args[2], (String[]) args[3]);
                    doReply = true;
                    break;
                case "getChoices":
                    reply = (Serializable) clientGui.getChoices((String) args[0], (int) args[1], (int) args[2], (Collection<Object>) args[3], args[4], (Function<Object, String>) args[5]); 
                    doReply = true;
                    break;
                case "order":
                    reply = (Serializable) clientGui.order((String) args[0], (String) args[1], (int) args[2], (int) args[3], (List<Object>) args[4], (List<Object>) args[5], (CardView) args[6], (boolean) args[7]); 
                    doReply = true;
                    break;
                case "sideboard":
                    reply = (Serializable) clientGui.sideboard((CardPool) args[0], (CardPool) args[1]);
                    doReply = true;
                    break;
                case "chooseSingleEntityForEffect":
                    reply = clientGui.chooseSingleEntityForEffect((String) args[0], (TrackableCollection<GameEntityView>) args[1], (DelayedReveal) args[2], (boolean) args[3]);
                    doReply = true;
                    break;
                case "setCard":
                    clientGui.setCard((CardView) args[0]);
                    break;
                // TODO case "setPlayerAvatar":
                case "openZones":
                    reply = clientGui.openZones((Collection<ZoneType>) args[0], (Map<PlayerView, Object>) args[1]);
                    doReply = true;
                    break;
                case "restoreOldZones":
                    clientGui.restoreOldZones((Map<PlayerView, Object>) args[0]);
                    break;
                case "isUiSetToSkipPhase":
                    reply = clientGui.isUiSetToSkipPhase((PlayerView) args[0], (PhaseType) args[1]);
                    doReply = true;
                    break;
                // BUTTONS
                case "btn_setEnabled":
                    btn.setEnabled((boolean) args[2]);
                    break;
                case "btn_setVisible":
                    btn.setVisible((boolean) args[2]);
                    break;
                case "btn_setText":
                    btn.setText((String) args[2]);
                    break;
                case "btn_isSelected":
                    reply = btn.isSelected();
                    doReply = true;
                    break;
                case "btn_setSelected":
                    btn.setSelected((boolean) args[2]);
                    break;
                case "btn_requestFocusInWindows":
                    reply = btn.requestFocusInWindow();
                    doReply = true;
                    break;
                case "btn_setCommand":
                    btn.setCommand((UiCommand) args[2]);
                    break;
                case "btn_setTextColor":
                    if (args.length == 3) {
                        btn.setTextColor((FSkinProp) args[2]);
                    } else {
                        btn.setTextColor((int) args[2], (int) args[3], (int) args[4]);
                    }
                default:
                    System.err.println("Unsupported game event " + event.getMethod());
                    break;
                }
                if (doReply) {
                    send(new ReplyEvent(event.getId(), reply));
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
