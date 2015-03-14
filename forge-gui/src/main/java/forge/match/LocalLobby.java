package forge.match;

import java.util.Collections;

import forge.AIOption;
import forge.GuiBase;
import forge.interfaces.IGuiGame;
import forge.net.game.LobbySlotType;

public final class LocalLobby extends GameLobby {

    public LocalLobby() {
        super(false);

        final String humanName = localName();
        final int[] avatarIndices = localAvatarIndices();

        final LobbySlot slot0 = new LobbySlot(LobbySlotType.LOCAL, humanName, avatarIndices[0], 0, true, Collections.<AIOption>emptySet());
        addSlot(slot0);

        final LobbySlot slot1 = new LobbySlot(LobbySlotType.AI, null, avatarIndices[1], 1, false, Collections.<AIOption>emptySet());
        addSlot(slot1);
    }

    @Override public boolean hasControl() {
        return true;
    }

    @Override public boolean mayEdit(final int index) {
        return true;
    }

    @Override public boolean mayControl(final int index) {
        return true;
    }

    @Override public boolean mayRemove(final int index) {
        return index >= 2;
    }

    @Override public IGuiGame getGui(final int index) {
        return GuiBase.getInterface().getNewGuiGame();
    }
}
