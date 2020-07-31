package forge.net.client;

import com.google.common.collect.Lists;
import forge.LobbyPlayer;
import forge.game.*;
import forge.game.player.RegisteredPlayer;
import forge.interfaces.ILobbyListener;
import forge.match.LobbySlot;
import forge.player.LobbyPlayerHuman;
import forge.player.PlayerZoneUpdate;
import forge.player.PlayerZoneUpdates;
import forge.trackable.TrackableObject;
import forge.trackable.TrackableTypes;
import forge.trackable.Tracker;
import io.netty.channel.ChannelHandlerContext;
import forge.game.player.PlayerView;
import forge.interfaces.IGuiGame;
import forge.model.FModel;
import forge.net.GameProtocolHandler;
import forge.net.IRemote;
import forge.net.ProtocolMethod;
import forge.net.ReplyPool;
import forge.net.event.LoginEvent;
import forge.properties.ForgePreferences.FPref;
import forge.trackable.TrackableCollection;

import java.util.*;

final class GameClientHandler extends GameProtocolHandler<IGuiGame> {
    private final FGameClient client;
    private final IGuiGame gui;
    private Tracker tracker;
    private Match match;
    private Game game;

    /**
     * Creates a client-side game handler.
     */
    public GameClientHandler(final FGameClient client) {
        super(true);
        this.client = client;
        this.gui = client.getGui();
        this.tracker = null;
        this.match = null;
        this.game = null;
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
    protected void beforeCall(final ProtocolMethod protocolMethod, final Object[] args) {
        switch (protocolMethod) {
            case openView:
                // only need one **match**
                if (this.match == null) {
                    this.match = createMatch();
                }

                // openView is called **once** per game, for now create a new Game instance each time
                this.game = createGame();

                // get a tracker
                this.tracker = createTracker();

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
     * This method attempts to recreate a <b>GameRules</b> object from existing state.
     *
     * @return GameRules
     */
    private GameRules createGameRules(GameType gameType, GameView gameView) {
        // FIXME: how do we know the rules are the same on each side???
        GameRules gameRules = new GameRules(gameType);
        // is this always safe to do?
        gameRules.setAppliedVariants(Collections.singleton(gameType));
        gameRules.setGamesPerMatch(gameView.getNumGamesInMatch());
        gameRules.setPoisonCountersToLose(gameView.getPoisonCountersToLose());

        return gameRules;
    }

    /**
     * Retrieve the desired GameType from the Lobby
     *
     * @return GameType
     */
    private GameType getGameType() {
        List<ILobbyListener> lobbyListeners = client.getLobbyListeners();
        ILobbyListener lobbyListener = lobbyListeners.get(0);
        ClientGameLobby myLobby = lobbyListener.getLobby();
        return myLobby.getGameType();
    }

    /**
     * This method retrieves enough of the existing (incomplete) game state to
     * recreate a new viable Match object
     *
     * Creating a <b>Match</b> requires that we have:
     *  * <b>GameRules</b>
     *  * <b>RegisteredPlayers</b>
     *  * Title
     *
     * @return Match
     */
    private Match createMatch() {
        // retrieve what we can from the existing (but incomplete) state
        final IGuiGame gui = client.getGui();
        GameView gameView = gui.getGameView();

        final GameType gameType = getGameType();
        final GameRules gameRules = createGameRules(gameType, gameView);
        final List<RegisteredPlayer> registeredPlayers = createRegisteredPlayers(gameType);

        // pull the title from the existing (incomplete) GameView
        final String title = gameView.getTitle();

        // create a valid match object and game
        Match match = new Match(gameRules, registeredPlayers, title);

        return match;
    }

    private Game createGame() {
        this.tracker = null;
        return this.match.createGame();
    }

    /**
     * Ensure the stored GameView is correct and retrieve a <b>Tracker</b> object.
     *
     * @return Tracker
     */
    private Tracker createTracker() {
        // replace the existing incomplete GameView with the newly created one
        gui.setGameView(null);
        gui.setGameView(game.getView());
        return gui.getGameView().getTracker();
    }

    /**
     * This method retrieves existing information about the players to
     * build a list of <b>RegisteredPlayers</b>.
     *
     *
     * @param gameType
     * @return List<RegisteredPlayer>
     */
    private List<RegisteredPlayer> createRegisteredPlayers(GameType gameType) {
        // get all lobby players
        List<ILobbyListener> lobbyListeners = client.getLobbyListeners();
        ILobbyListener lobbyListener = lobbyListeners.get(0);
        ClientGameLobby myLobby = lobbyListener.getLobby();

        List<RegisteredPlayer> players = Lists.newArrayList();
        int playerCount = myLobby.getNumberOfSlots();
        for (int i = 0; i < playerCount; i++) {
            LobbySlot playerSlot = myLobby.getSlot(i);
            RegisteredPlayer player = RegisteredPlayer.forVariants(
                    playerCount,
                    Collections.singleton(gameType),
                    playerSlot.getDeck(),
                    null,
                    false,
                    null,
                    null
            );
            LobbyPlayer lobbyPlayer = new LobbyPlayerHuman(
                    playerSlot.getName(),
                    playerSlot.getAvatarIndex(),
                    playerSlot.getSleeveIndex()
            );
            player.setPlayer(lobbyPlayer);
            player.setTeamNumber(playerSlot.getTeam());
            players.add(player);
        }

        final List<RegisteredPlayer> sortedPlayers = Lists.newArrayList(players);
        Collections.sort(sortedPlayers, new Comparator<RegisteredPlayer>() {
            @Override
            public final int compare(final RegisteredPlayer p1, final RegisteredPlayer p2) {
                final int v1 = p1.getPlayer() instanceof LobbyPlayerHuman ? 0 : 1;
                final int v2 = p2.getPlayer() instanceof LobbyPlayerHuman ? 0 : 1;
                return Integer.compare(v1, v2);
            }
        });

        return sortedPlayers;
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
        System.err.println("replicated PlayerView properties - " + existingPlayerView.toString());
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        // Don't use send() here, as this.channel is not yet set!
        ctx.channel().writeAndFlush(new LoginEvent(FModel.getPreferences().getPref(FPref.PLAYER_NAME), Integer.parseInt(FModel.getPreferences().getPref(FPref.UI_AVATARS).split(",")[0]), Integer.parseInt(FModel.getPreferences().getPref(FPref.UI_SLEEVES).split(",")[0])));
    }

}