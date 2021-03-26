package forge.gamemodes.net.client;

import forge.gamemodes.match.GameLobby;
import forge.gui.interfaces.IGuiGame;

public final class ClientGameLobby extends GameLobby {
    private int localPlayer = -1;

    public ClientGameLobby() {
        super(true);
    }

    public void setLocalPlayer(final int index) {
        this.localPlayer = index;
    }

    @Override public boolean hasControl() {
        return false;
    }

    @Override public boolean mayEdit(final int index) {
        return index == localPlayer;
    }

    @Override public boolean mayControl(final int index) {
        return false;
    }

    @Override public boolean mayRemove(final int index) {
        return false;
    }

    @Override protected IGuiGame getGui(final int index) {
        return null;
    }

    @Override protected void onGameStarted() {
    }
}
