package forge.screens.constructed;

import forge.ai.LobbyPlayerAi;
import forge.deck.Deck;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.player.LobbyPlayerHuman;
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
        LobbyPlayerHuman humanLobbyPlayer = new LobbyPlayerHuman("Human");
        RegisteredPlayer humanRegisteredPlayer = new RegisteredPlayer(humanDeck);
        humanRegisteredPlayer.setPlayer(humanLobbyPlayer);
        launchParams.players.add(humanRegisteredPlayer);

        Deck aiDeck = Utils.generateRandomDeck(2);
        LobbyPlayerAi aiLobbyPlayer = new LobbyPlayerAi("AI Player");
        RegisteredPlayer aiRegisteredPlayer = new RegisteredPlayer(aiDeck);
        aiRegisteredPlayer.setPlayer(aiLobbyPlayer);
        launchParams.players.add(aiRegisteredPlayer);

        return false; //TODO: Support launching match
    }
}
