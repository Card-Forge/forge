package forge.adventure.world;

import com.badlogic.gdx.graphics.Pixmap;

public class WorldSave {
    public AdventurePlayer player;
    public World world;
    static WorldSave currentSave;

    public static WorldSave getCurrentSave()
    {
        return currentSave;
    }
    public static Pixmap GenerateNewWorld()
    {
        WorldSave ret=new WorldSave();
        ret.world=new World();
        return ret.world.GenerateNew();
        //return currentSave = ret;
    }
}
