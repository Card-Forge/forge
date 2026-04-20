package forge.gamemodes.net.client;

import forge.game.*;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.gamemodes.net.GameEventProxy;
import forge.gamemodes.net.GameProtocolHandler;
import forge.gui.GuiBase;
import forge.util.IHasForgeLog;
import forge.gamemodes.net.IRemote;
import forge.gamemodes.net.ProtocolMethod;
import forge.gamemodes.net.ReplyPool;
import forge.gamemodes.net.event.LoginEvent;
import forge.gamemodes.net.server.RemoteClientGuiGame;
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
        // Libgdx blocking prompts deadlock if run on the GL thread — route return-value
        // protocol methods to a background thread.
        if (GuiBase.getInterface().isLibgdxPort()
                && !protocolMethod.getReturnType().equals(Void.TYPE)) {
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
                    if (this.tracker == null) {
                        this.tracker = new Tracker();
                        if (gameView.getGameLog() == null) {
                            gameView.initGameLog();
                        }
                    }
                    if (gameView.getTracker() == null) {
                        updateTrackers(new Object[]{gameView});
                    }
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
            // Register all objects from the incoming GameView in the tracker's ID
            // lookup table synchronously on the IO thread. copyChangedProps() also
            // does this, but it runs on EDT asynchronously — too late for the
            // subsequent handleGameEvents.beforeCall which needs to resolve IdRefs.
            if (protocolMethod == ProtocolMethod.setGameView && args.length > 0 && args[0] instanceof GameView gv) {
                gv.updateObjLookup();
                // updateObjLookup skips objects already in the tracker, so CardViews
                // registered on the first setGameView become stale (wrong zone, etc.).
                // Replace tracker entries with incoming server CardViews so that
                // IdRef resolution in handleGameEvents gets current state.
                refreshTrackerCardViews(gv);
            }
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
            if (obj instanceof TrackableObject trackableObject) {
                if (trackableObject.getTracker() == null) {
                    trackableObject.setTracker(this.tracker);
                    // walk the props
                    EnumMap props = trackableObject.getProps();
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

    private void replicateProps(final Object[] objs) {
        for (Object obj: objs) {
            if (obj instanceof PlayerView pv) {
                replicatePlayerView(pv);
            }
            else if (obj instanceof PlayerZoneUpdate pzu) {
                replicatePlayerView(pzu.getPlayer());
            }
            else if (obj instanceof PlayerZoneUpdates pzu) {
                Iterator itrPlayerZoneUpdates = pzu.iterator();
                while (itrPlayerZoneUpdates.hasNext()) {
                    PlayerView newPlayerView = ((PlayerZoneUpdate)itrPlayerZoneUpdates.next()).getPlayer();
                    // PlayerZoneUpdates aren't a TrackableCollection, so updateTrackers
                    // doesn't reach them automatically
                    updateTrackers(new Object[]{newPlayerView});
                    replicatePlayerView(newPlayerView);
                }
            }
        }
    }

    private void replicatePlayerView(final PlayerView newPlayerView) {
        if (RemoteClientGuiGame.useDeltaSync) {
            return; // Delta sync manages state — don't overwrite from protocol snapshots
        }
        PlayerView existingPlayerView = tracker.getObj(TrackableTypes.PlayerViewType, newPlayerView.getId());
        existingPlayerView.copyChangedProps(newPlayerView);
        netLog.trace("replicated PlayerView properties - {}", existingPlayerView.toString());
    }

    /**
     * Replace tracker CardView entries with incoming server CardViews.
     * <p>
     * {@code updateObjLookup()} skips objects already in the tracker, so CardViews
     * registered on the first {@code setGameView} become stale — their zone, state,
     * and other properties no longer reflect the server's current game state.
     * When {@link GameEventProxy} resolves IdRefs from the tracker, it gets these
     * stale CardViews, causing issues like card-back images in the game log
     * (the stale zone is Library, so {@code canBeShownTo} returns false).
     */
    private void refreshTrackerCardViews(final GameView gv) {
        if (gv.getPlayers() == null) { return; }
        for (final PlayerView pv : gv.getPlayers()) {
            final EnumMap<?, ?> props = pv.getProps();
            if (props == null) { continue; }
            for (final Object value : props.values()) {
                if (value instanceof TrackableCollection<?> collection) {
                    for (final Object item : collection) {
                        if (item instanceof CardView cv) {
                            tracker.putObj(TrackableTypes.CardViewType, cv.getId(), cv);
                        }
                    }
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
                BuildInfo.getVersionString()
        ));
    }

}