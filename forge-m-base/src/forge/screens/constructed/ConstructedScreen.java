package forge.screens.constructed;

import forge.deck.Deck;
import forge.game.GameType;
import forge.game.player.LobbyPlayer;
import forge.game.player.RegisteredPlayer;
import forge.net.FServer;
import forge.screens.LaunchScreen;
import forge.utils.Utils;

public class ConstructedScreen extends LaunchScreen {
    public ConstructedScreen() {
        super("Constructed");
    }

    @Override
    protected void doLayoutAboveBtnStart(float startY, float width, float height) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected boolean buildLaunchParams(LaunchParams launchParams) {
        launchParams.gameType = GameType.Constructed;

        //TODO: Allow picking decks
        Deck humanDeck = Utils.generateRandomDeck(2);
        if (humanDeck == null) { return false; }
        LobbyPlayer humanLobbyPlayer = FServer.getLobby().getGuiPlayer();
        RegisteredPlayer humanRegisteredPlayer = new RegisteredPlayer(humanDeck);
        humanRegisteredPlayer.setPlayer(humanLobbyPlayer);
        launchParams.players.add(humanRegisteredPlayer);

        Deck aiDeck = Utils.generateRandomDeck(2);
        if (aiDeck == null) { return false; }
        LobbyPlayer aiLobbyPlayer = FServer.getLobby().getAiPlayer();
        RegisteredPlayer aiRegisteredPlayer = new RegisteredPlayer(aiDeck);
        aiRegisteredPlayer.setPlayer(aiLobbyPlayer);
        launchParams.players.add(aiRegisteredPlayer);

        return true;
    }
}
