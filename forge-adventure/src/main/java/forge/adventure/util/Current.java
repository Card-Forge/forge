package forge.adventure.util;

import forge.adventure.world.AdventurePlayer;
import forge.adventure.world.WorldSave;

public class Current {
    public static AdventurePlayer player()
    {
        return WorldSave.getCurrentSave().getPlayer();
    }
}
