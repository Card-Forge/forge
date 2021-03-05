package forge.gamemodes.net;

import java.util.Collections;

import forge.gamemodes.match.GameLobby;
import forge.gamemodes.match.LobbySlot;
import forge.gamemodes.match.LobbySlotType;
import forge.interfaces.IGuiGame;

//Temporary lobby instance to use for OnlineLobby before connecting to a server
public final class OfflineLobby extends GameLobby {
    public OfflineLobby() {
        super(true);

        final String humanName = localName();
        final int[] avatarIndices = localAvatarIndices();
        final int[] sleeveIndices = localSleeveIndices();

        final LobbySlot slot0 = new LobbySlot(LobbySlotType.LOCAL, humanName, avatarIndices[0], sleeveIndices[0], 0, true, false, Collections.emptySet());
        addSlot(slot0);

        final LobbySlot slot1 = new LobbySlot(LobbySlotType.OPEN, null, -1, -1,-1, false, false, Collections.emptySet());
        addSlot(slot1);
    }

    @Override
    public boolean hasControl() {
        return true;
    }

    @Override
    public boolean mayEdit(final int index) {
        return index == 0;
    }

    @Override
    public boolean mayControl(final int index) {
        return index == 0;
    }

    @Override
    public boolean mayRemove(final int index) {
        return index > 0;
    }

    @Override
    protected IGuiGame getGui(final int index) {
        return null;
    }

    @Override
    protected void onGameStarted() {
    }
}
