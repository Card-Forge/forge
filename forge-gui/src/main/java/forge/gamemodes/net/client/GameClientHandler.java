package forge.gamemodes.net.client;

import forge.game.*;
import forge.game.player.PlayerView;
import forge.gamemodes.net.GameProtocolHandler;
import forge.gamemodes.net.IRemote;
import forge.gamemodes.net.ProtocolMethod;
import forge.gamemodes.net.ReplyPool;
import forge.gamemodes.net.event.LoginEvent;
import forge.gui.interfaces.IGuiGame;
import forge.util.BuildInfo;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.player.PlayerZoneUpdate;
import forge.player.PlayerZoneUpdates;
import forge.trackable.TrackableCollection;
import forge.trackable.TrackableObject;
import forge.trackable.TrackableTypes;
import forge.trackable.Tracker;
import io.netty.channel.ChannelHandlerContext;

import java.util.EnumMap;
import java.util.Iterator;

final class GameClientHandler extends GameProtocolHandler<IGuiGame> {
    private final FGameClient client;
    private final IGuiGame gui;
    private Tracker tracker;

    /**
     * Creates a client-side game handler.
     */
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

    @SuppressWarnings("unchecked")
    @Override
    protected void beforeCall(final ChannelHandlerContext ctx, final ProtocolMethod protocolMethod, final Object[] args) {
        switch (protocolMethod) {
            case setGameView:
                // IMPORTANT: Set gameView immediately in the Netty thread so it's available
                // for subsequent beforeCall handlers (especially openView which needs it).
                // The actual setGameView method will also run in EDT, but we need it now.
                if (args.length > 0 && args[0] instanceof GameView) {
                    GameView gameView = (GameView) args[0];
                    // Ensure the incoming gameView has a tracker before setGameView tries to
                    // copy properties (copyChangedProps requires tracker for collection handling)
                    if (this.tracker != null && gameView.getTracker() == null) {
                        gameView.setTracker(this.tracker);
                        // Also update trackers on nested objects
                        updateTrackers(new Object[]{gameView});
                    }
                    gui.setGameView(gameView);
                }
                break;
            case openView:
                gui.setNetGame();
                // Use the tracker from the existing server gameView (already initialized
                // by ensureTrackerInitialized during the first setGameView call).
                // No need to create a local Match/Game — the server's gameView already
                // has the correct players and tracker.
                this.tracker = gui.getGameView().getTracker();

                for (PlayerView myPlayer : (TrackableCollection<PlayerView>) args[0]) {
                    if (myPlayer.getTracker() == null) {
                        myPlayer.setTracker(this.tracker);
                    }
                }

                final TrackableCollection<PlayerView> myPlayers = (TrackableCollection<PlayerView>) args[0];
                client.setGameControllers(myPlayers);

                break;
            default:
                break;
        }
        if (!(this.tracker == null)) {
            updateTrackers(args);
            replicateProps(args);
        }
    }

    /**
     * This method is used to recursively update the <b>tracker</b>
     * references on all objects and their props.
     *
     * @param objs
     */
    private void updateTrackers(final Object[] objs) {
        for (Object obj: objs) {
            if (obj instanceof TrackableObject) {
                TrackableObject trackableObject = ((TrackableObject) obj);
                if (trackableObject.getTracker() == null) {
                    trackableObject.setTracker(this.tracker);
                    // walk the props
                    EnumMap props = trackableObject.getProps();
                    if (!(props == null)) {
                        for (Object propObj : props.values()) {
                            updateTrackers(new Object[]{propObj});
                        }
                    }
                }
            } else if (obj instanceof TrackableCollection) {
                TrackableCollection collection = ((TrackableCollection) obj);
                Iterator itrCollection = collection.iterator();
                while (itrCollection.hasNext()) {
                    Object objCollection = itrCollection.next();
                    updateTrackers(new Object[]{objCollection});
                }
            }
        }
    }

    private void replicateProps(final Object[] objs) {
        for (Object obj: objs) {
            if (obj instanceof PlayerView) {
                replicatePlayerView((PlayerView) obj);
            }
            else if (obj instanceof PlayerZoneUpdate) {
                replicatePlayerView(((PlayerZoneUpdate) obj).getPlayer());
            }
            else if (obj instanceof PlayerZoneUpdates) {
                Iterator itrPlayerZoneUpdates = ((PlayerZoneUpdates) obj).iterator();
                while (itrPlayerZoneUpdates.hasNext()) {
                    PlayerView newPlayerView = ((PlayerZoneUpdate)itrPlayerZoneUpdates.next()).getPlayer();
                    /**
                     * FIXME: this should be handled by the original call to updateTrackers
                     * However, PlayerZoneUpdates aren't a TrackableCollection.
                     * So, additional logic will be needed. Leaving here for now.
                     */
                    updateTrackers(new Object[]{newPlayerView});
                    replicatePlayerView(newPlayerView);
                }
            }
            /*
            else {
                System.err.println("replicateProps - did not handle : " + obj.getClass().toString());
            }
             */
        }
    }

    private void replicatePlayerView(final PlayerView newPlayerView) {
        PlayerView existingPlayerView = tracker.getObj(TrackableTypes.PlayerViewType, newPlayerView.getId());
        existingPlayerView.copyChangedProps(newPlayerView);
        forge.gamemodes.net.NetworkDebugLogger.trace("replicated PlayerView properties - %s", existingPlayerView.toString());
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        // Don't use send() here, as this.channel is not yet set!
        String loginName = client.getUsername();
        if (loginName == null || loginName.isEmpty()) {
            loginName = FModel.getPreferences().getPref(FPref.PLAYER_NAME);
        }
        ctx.channel().writeAndFlush(new LoginEvent(
                loginName,
                Integer.parseInt(FModel.getPreferences().getPref(FPref.UI_AVATARS).split(",")[0]),
                Integer.parseInt(FModel.getPreferences().getPref(FPref.UI_SLEEVES).split(",")[0]),
                BuildInfo.getVersionString()
        ));
    }

}