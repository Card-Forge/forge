package forge.net;

import java.util.Collections;

import forge.AIOption;
import forge.interfaces.IGuiGame;
import forge.match.GameLobby;
import forge.match.LobbySlot;
import forge.match.LobbySlotType;

//Temporary lobby instance to use for OnlineLobby before connecting to a server
public final class OfflineLobby extends GameLobby {
    public OfflineLobby() {
        super(true);

        final String humanName = localName();
        final int[] avatarIndices = localAvatarIndices();

        final LobbySlot slot0 = new LobbySlot(LobbySlotType.LOCAL, humanName, avatarIndices[0], 0, true, false, Collections.<AIOption>emptySet());
        addSlot(slot0);

        final LobbySlot slot1 = new LobbySlot(LobbySlotType.OPEN, null, -1, -1, false, false, Collections.<AIOption>emptySet());
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
