package forge.gamemodes.net.server;

import forge.gamemodes.match.GameLobby;
import forge.gamemodes.match.LobbySlot;
import forge.gamemodes.match.LobbySlotType;
import forge.gui.interfaces.IGuiGame;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;

public final class ServerGameLobby extends GameLobby {

    public ServerGameLobby() {
        super(true);
        addSlot(new LobbySlot(LobbySlotType.LOCAL, localName(), localAvatarIndices()[0], localSleeveIndices()[0],0, true, false, Collections.emptySet()));
        addSlot(new LobbySlot(LobbySlotType.OPEN, null, -1, -1, 1, false, false, Collections.emptySet()));
    }

    /**
     * Connect a player to the first available open slot.
     * This method is synchronized to prevent race conditions when multiple
     * clients connect simultaneously (which could assign the same slot to
     * multiple clients).
     *
     * @param name the player's name
     * @param avatarIndex the avatar index
     * @param sleeveIndex the sleeve index
     * @return the assigned slot index, or -1 if no slots available
     */
    public synchronized int connectPlayer(final String name, final int avatarIndex, final int sleeveIndex) {
        final int nSlots = getNumberOfSlots();
        for (int index = 0; index < nSlots; index++) {
            final LobbySlot slot = getSlot(index);
            if (slot.getType() == LobbySlotType.OPEN) {
                connectPlayer(name, avatarIndex, sleeveIndex, slot);
                return index;
            }
        }
        return -1;
    }
    private void connectPlayer(final String name, final int avatarIndex, final int sleeveIndex, final LobbySlot slot) {
        slot.setType(LobbySlotType.REMOTE);
        slot.setName(name);
        slot.setAvatarIndex(avatarIndex);
        slot.setSleeveIndex(sleeveIndex);
        updateView(false);
    }
    public void disconnectPlayer(final int index) {
        final LobbySlot slot = getSlot(index);
        slot.setType(LobbySlotType.OPEN);
        slot.setName(StringUtils.EMPTY);
        slot.setIsReady(false);
        updateView(false);
    }

    @Override
    public boolean hasControl() {
        return true;
    }

    @Override
    public boolean mayEdit(final int index) {
        final LobbySlotType type = getSlot(index).getType();
        return type != LobbySlotType.REMOTE && type != LobbySlotType.OPEN;
    }

    @Override
    public boolean mayControl(final int index) {
        return getSlot(index).getType() != LobbySlotType.REMOTE;
    }

    @Override
    public boolean mayRemove(final int index) {
        return index >= 2;
    }

    @Override
    protected IGuiGame getGui(final int index) {
        return FServerManager.getInstance().getGui(index);
    }

    @Override
    protected void onGameStarted() {
    }
}
