package forge.match;

import java.util.Collections;

import forge.AIOption;
import forge.GuiBase;
import forge.interfaces.IGuiGame;

public final class LocalLobby extends GameLobby {

    private IGuiGame gui = null;
    public LocalLobby() {
        super(false);

        final String humanName = localName();
        final int[] avatarIndices = localAvatarIndices();

        final LobbySlot slot0 = new LobbySlot(LobbySlotType.LOCAL, humanName, avatarIndices[0], 0, true, true, Collections.<AIOption>emptySet());
        addSlot(slot0);

        final LobbySlot slot1 = new LobbySlot(LobbySlotType.AI, null, avatarIndices[1], 1, false, true, Collections.<AIOption>emptySet());
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

    @Override protected IGuiGame getGui(final int index) {
        if (gui == null) {
            gui = GuiBase.getInterface().getNewGuiGame();
        }
        return gui;
    }

    @Override protected void onGameStarted() {
        gui = null;
        // Re-randomize random decks after starting a game
        updateView(true);
    }
}
