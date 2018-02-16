package forge.net.client;

import com.google.common.collect.Lists;
import forge.LobbyPlayer;
import forge.game.*;
import forge.game.player.RegisteredPlayer;
import forge.interfaces.ILobbyListener;
import forge.match.LobbySlot;
import forge.player.LobbyPlayerHuman;
import forge.trackable.TrackableObject;
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
    protected void beforeCall(final ProtocolMethod protocolMethod, final Object[] args) {
        switch (protocolMethod) {
            case openView:
                if (this.tracker == null) {
                    int maxAttempts = 5;
                    for (int numAttempts = 0; numAttempts < maxAttempts; numAttempts++) {
                        try {

                            this.tracker = createTracker();

                            for (PlayerView myPlayer : (TrackableCollection<PlayerView>) args[0]) {
                                if (myPlayer.getTracker() == null) {
                                    myPlayer.setTracker(this.tracker);
                                }
                            }

                            final TrackableCollection<PlayerView> myPlayers = (TrackableCollection<PlayerView>) args[0];
                            client.setGameControllers(myPlayers);

                        } catch (Exception e) {
                            System.err.println("Failed: attempt number: " + numAttempts + " - " + e.toString());
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e1) {
                                e1.printStackTrace();
                            }
                        }
                    }
                }
                break;
            default:
                break;
        }
        if (!(this.tracker == null)) {
            updateTrackers(args);
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
        gameRules.setGamesPerMatch(gameView.getNumGamesInMatch());
        gameRules.setPoisonCountersToLose(gameView.getPoisonCountersToLose());

        return gameRules;
    }

    /**
     * This method creates the necessary objects and state to retrieve a <b>Tracker</b> object.
     *
     * Near as I can tell, that means that we need to create a <b>Match</b>.
     *
     * Creating a <b>Match</b> requires that we have:
     *  * <b>GameRules</b>
     *  * <b>RegisteredPlayers</b>
     *  * Title
     *
     * @return Tracker
     */
    private Tracker createTracker() {

        // retrieve what we can from the existing (but incomplete) state
        final IGuiGame gui = client.getGui();
        GameView gameView = gui.getGameView();

        final GameType gameType = gameView.getGameType();
        final GameRules gameRules = createGameRules(gameType, gameView);
        final List<RegisteredPlayer> registeredPlayers = createRegisteredPlayers(gameType);

        // pull the title from the existing (incomplete) GameView
        final String title = gameView.getTitle();

        // create a valid match object and game
        Match match = new Match(gameRules, registeredPlayers, title);
        Game game = match.createGame();

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
                    playerSlot.getAvatarIndex()
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
                    EnumMap props = (EnumMap) trackableObject.getProps();
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

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        // Don't use send() here, as this.channel is not yet set!
        ctx.channel().writeAndFlush(new LoginEvent(FModel.getPreferences().getPref(FPref.PLAYER_NAME), Integer.parseInt(FModel.getPreferences().getPref(FPref.UI_AVATARS).split(",")[0])));
    }

}