package forge.screens.constructed;

import forge.gamemodes.match.LocalLobby;
import forge.gamemodes.net.event.UpdateLobbyPlayerEvent;
import forge.interfaces.IPlayerChangeListener;
import forge.screens.home.NewGameMenu;

public class ConstructedScreen extends LobbyScreen {
    public ConstructedScreen() {
        super(null, NewGameMenu.getMenu(), new LocalLobby());

        setPlayerChangeListener(new IPlayerChangeListener() {
            @Override
            public final void update(final int index, final UpdateLobbyPlayerEvent event) {
                getLobby().applyToSlot(index, event);
            }
        });
    }
}
