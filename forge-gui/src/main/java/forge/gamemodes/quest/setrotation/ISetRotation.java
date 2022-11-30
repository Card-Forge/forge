package forge.gamemodes.quest.setrotation;

import java.util.List;

/**
 * Supplies the current rotation of set codes based on the current quest state.
 * Used for quest worlds that change their sets automatically over time.
 */
public interface ISetRotation {
    public List<String> getCurrentSetCodes(List<String> allowedSetCodes);
}
