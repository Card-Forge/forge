package forge.screens.match;

import java.util.ArrayList;
import java.util.List;

import forge.Forge;
import forge.game.Game;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.player.LobbyPlayer;
import forge.game.player.Player;
import forge.model.FModel;
import forge.utils.ForgePreferences.FPref;

public class FControl {
    private static Game game;
    private static MatchScreen view;
    private static List<Player> sortedPlayers;

    public static void startGame(final Match match0, final MatchScreen view0) {
        game = match0.createGame();
        view = view0;

        /*if (game.getRules().getGameType() == GameType.Quest) {
            QuestController qc = Singletons.getModel().getQuest();
            // Reset new list when the Match round starts, not when each game starts
            if (game.getMatch().getPlayedGames().isEmpty()) {
                qc.getCards().resetNewList();
            }
            game.subscribeToEvents(qc); // this one listens to player's mulligans ATM
        }*/

        //inputQueue = new InputQueue();

        //game.subscribeToEvents(Singletons.getControl().getSoundSystem());

        LobbyPlayer humanLobbyPlayer = game.getRegisteredPlayers().get(0).getLobbyPlayer(); //FServer.instance.getLobby().getGuiPlayer();
        // The UI controls should use these game data as models
        initMatch(game.getRegisteredPlayers(), humanLobbyPlayer);
        
        // It's important to run match in a different thread to allow GUI inputs to be invoked from inside game. 
        // Game is set on pause while gui player takes decisions
        /*game.getAction().invoke(new Runnable() {
            @Override
            public void run() {
                match.startGame(game);
            }
        });*/
    }

    public static MatchScreen getView() {
        return view;
    }

    public static void endCurrentGame() {
        if (game == null) { return; }

        Forge.back();
        game = null;
    }

    public static void initMatch(final List<Player> players, LobbyPlayer localPlayer) {
        // TODO fix for use with multiplayer

        final String[] indices = FModel.getPreferences().getPref(FPref.UI_AVATARS).split(",");

        // Instantiate all required field slots (user at 0)
        sortedPlayers = shiftPlayersPlaceLocalFirst(players, localPlayer);

        /*final List<VField> fields = new ArrayList<VField>();
        final List<VCommand> commands = new ArrayList<VCommand>();

        int i = 0;
        for (Player p : sortedPlayers) {
            // A field must be initialized after it's instantiated, to update player info.
            // No player, no init.
            VField f = new VField(EDocID.Fields[i], p, localPlayer);
            VCommand c = new VCommand(EDocID.Commands[i], p);
            fields.add(f);
            commands.add(c);

            //setAvatar(f, new ImageIcon(FSkin.getAvatars().get()));
            setAvatar(f, getPlayerAvatar(p, Integer.parseInt(indices[i > 2 ? 1 : 0])));
            f.getLayoutControl().initialize();
            c.getLayoutControl().initialize();
            i++;
        }

        // Replace old instances
        view.setCommandViews(commands);
        view.setFieldViews(fields);

        VPlayers.SINGLETON_INSTANCE.init(players);*/

        initHandViews(localPlayer);
    }

    public static void initHandViews(LobbyPlayer localPlayer) {
        /*final List<VHand> hands = new ArrayList<VHand>();

        int i = 0;
        for (Player p : sortedPlayers) {
            if (p.getLobbyPlayer() == localPlayer) {
                VHand newHand = new VHand(EDocID.Hands[i], p);
                newHand.getLayoutControl().initialize();
                hands.add(newHand);
            }
            i++;
        }

        if (hands.isEmpty()) { // add empty hand for matches without human
            VHand newHand = new VHand(EDocID.Hands[0], null);
            newHand.getLayoutControl().initialize();
            hands.add(newHand);
        }
        view.setHandViews(hands);*/
    }

    private static List<Player> shiftPlayersPlaceLocalFirst(final List<Player> players, LobbyPlayer localPlayer) {
        // get an arranged list so that the first local player is at index 0
        List<Player> sortedPlayers = new ArrayList<Player>(players);
        int ixFirstHuman = -1;
        for (int i = 0; i < players.size(); i++) {
            if (sortedPlayers.get(i).getLobbyPlayer() == localPlayer) {
                ixFirstHuman = i;
                break;
            }
        }
        if (ixFirstHuman > 0) {
            sortedPlayers.add(0, sortedPlayers.remove(ixFirstHuman));
        }
        return sortedPlayers;
    }

    public static boolean mayShowCard(Card c) {
        return true;// game == null || !gameHasHumanPlayer || c.canBeShownTo(getCurrentPlayer());
    }
}
