package forge.gamemodes.net.client;

import forge.game.*;
import forge.game.player.PlayerView;
import forge.gamemodes.net.CompatibleObjectDecoder;
import forge.gamemodes.net.CompatibleObjectEncoder;
import forge.gamemodes.net.GameProtocolHandler;
import forge.gui.GuiBase;
import forge.util.IHasForgeLog;
import forge.gamemodes.net.IRemote;
import forge.gamemodes.net.ProtocolMethod;
import forge.gamemodes.net.ReplyPool;
import forge.gamemodes.net.event.LoginEvent;
import forge.gui.interfaces.IGuiGame;
import forge.util.BuildInfo;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.trackable.TrackableCollection;
import forge.trackable.TrackableObject;
import forge.trackable.Tracker;
import io.netty.channel.ChannelHandlerContext;

import java.util.Iterator;
import java.util.Map;

final class GameClientHandler extends GameProtocolHandler<IGuiGame> implements IHasForgeLog {

    private final FGameClient client;
    private final IGuiGame gui;
    private Tracker tracker;

    public GameClientHandler(final FGameClient client) {
        super(true);
        this.client = client;
        this.gui = client.getGui();
        this.tracker = null;
    }

    @Override
    protected ReplyPool getReplyPool(final ChannelHandlerContext ctx) {
        return client.getReplyPool();
    }

    @Override
    protected IRemote getRemote(final ChannelHandlerContext ctx) {
        return client;
    }

    @Override
    protected IGuiGame getToInvoke(final ChannelHandlerContext ctx) {
        return gui;
    }

    @Override
    protected boolean shouldDispatchToGuiThread(final ProtocolMethod protocolMethod) {
        // Libgdx modal prompts block via WaitCallback and deadlock on the GL thread. Return-value
        // methods always block; message/showErrorDialog return void but still open a blocking modal.
        if (GuiBase.getInterface().isLibgdxPort()
                && (!protocolMethod.getReturnType().equals(Void.TYPE)
                    || protocolMethod == ProtocolMethod.message
                    || protocolMethod == ProtocolMethod.showErrorDialog)) {
            return false;
        }
        return super.shouldDispatchToGuiThread(protocolMethod);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void beforeCall(final ChannelHandlerContext ctx, final ProtocolMethod protocolMethod, final Object[] args) {
        switch (protocolMethod) {
            case setGameView:
                if (args.length > 0 && args[0] instanceof GameView gameView) {
                    // Each full view restarts delta sync on the host, so the client starts a new tracker as the host does per game
                    this.tracker = new Tracker();
                    // Encoder uses the tracker to emit IdRef for client→server CardView args
                    // (presence check only — stale detection is server-only).
                    // Ephemerals absent from the tracker serialize as full objects in both directions.
                    CompatibleObjectEncoder encoder = ctx.pipeline().get(CompatibleObjectEncoder.class);
                    if (encoder != null) {
                        encoder.setTracker(this.tracker);
                    }
                    CompatibleObjectDecoder decoder = ctx.pipeline().get(CompatibleObjectDecoder.class);
                    if (decoder != null) {
                        decoder.setTracker(this.tracker);
                    }
                    if (gameView.getGameLog() == null) {
                        gameView.initGameLog();
                    }
                    if (gameView.getTracker() == null) {
                        updateTrackers(new Object[]{gameView});
                    }
                }
                if (client.isActiveChannel(ctx.channel())
                        && client.getReconnectState() == FGameClient.ReconnectState.RECONNECTING) {
                    client.onResumeResponseReceived();
                }
                break;
            case openView:
                gui.setNetGame();
                final TrackableCollection<PlayerView> myPlayers = (TrackableCollection<PlayerView>) args[0];
                for (PlayerView myPlayer : myPlayers) {
                    if (myPlayer.getTracker() == null) {
                        myPlayer.setTracker(this.tracker);
                    }
                }
                client.setGameControllers(myPlayers);
                break;
            default:
                break;
        }
        if (this.tracker != null) {
            updateTrackers(args);
            // On the IO thread: the GUI's own lookup runs on the EDT, too late for the next message's IdRefs
            if (protocolMethod == ProtocolMethod.setGameView && args.length > 0 && args[0] instanceof GameView gv) {
                gv.updateObjLookup();
            }
        }
    }

    /**
     * This method is used to recursively update the <b>tracker</b>
     * references on all objects and their props.
     *
     * <p>Inline-serialized CardViews are intentionally NOT registered in the
     * tracker's id lookup: a tracker miss is the symmetric signal that a
     * CardView is ephemeral, mirroring the host's encoder check.
     *
     * @param objs
     */
    private void updateTrackers(final Object[] objs) {
        for (Object obj: objs) {
            if (obj instanceof TrackableObject trackableObject) {
                if (trackableObject.getTracker() == null) {
                    trackableObject.setTracker(this.tracker);
                    // walk the props
                    Map props = trackableObject.getProps();
                    if (props != null) {
                        for (Object propObj : props.values()) {
                            updateTrackers(new Object[]{propObj});
                        }
                    }
                }
            } else if (obj instanceof TrackableCollection collection) {
                Iterator itrCollection = collection.iterator();
                while (itrCollection.hasNext()) {
                    Object objCollection = itrCollection.next();
                    updateTrackers(new Object[]{objCollection});
                }
            }
        }
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        String loginName = client.getUsername();
        if (loginName == null || loginName.isEmpty()) {
            loginName = FModel.getPreferences().getPref(FPref.PLAYER_NAME);
        }
        // Don't use send() here, as this.channel is not yet set!
        ctx.channel().writeAndFlush(new LoginEvent(
                loginName,
                Integer.parseInt(FModel.getPreferences().getPref(FPref.UI_AVATARS).split(",")[0]),
                Integer.parseInt(FModel.getPreferences().getPref(FPref.UI_SLEEVES).split(",")[0]),
                BuildInfo.getVersionString(),
                GuiBase.getInterface().isLibgdxPort()
        ));
    }

}