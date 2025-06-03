package forge.screens.constructed;

import forge.gamemodes.match.LocalLobby;
import forge.screens.home.NewGameMenu;

public class ConstructedScreen extends LobbyScreen {
    public ConstructedScreen() {
        super(null, NewGameMenu.getMenu(), new LocalLobby());

        setPlayerChangeListener((index, event) -> getLobby().applyToSlot(index, event));
    }
}
