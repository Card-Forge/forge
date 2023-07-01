package forge.interfaces;

import forge.gamemodes.match.LobbySlotType;

public interface IUpdateable{
    void update(boolean fullUpdate);
    void update(int slot, LobbySlotType type);
}
