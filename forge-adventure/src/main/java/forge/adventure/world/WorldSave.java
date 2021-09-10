package forge.adventure.world;

import forge.adventure.data.DifficultyData;
import forge.adventure.util.Config;
import forge.deck.Deck;
import forge.localinstance.properties.ForgeProfileProperties;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.HashMap;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Represents everything that will be saved, like the player and the world.
 */
public class WorldSave {

    static final int AUTO_SAVE_SLOT =-1;
    static final int QUICK_SAVE_SLOT =-2;
    static final int INVALID_SAVE_SLOT =-3;
    static WorldSave currentSave=new WorldSave();
    public WorldSaveHeader header = new WorldSaveHeader();
    private final AdventurePlayer player=new AdventurePlayer();
    private final World world=new World();
    private final HashMap<String,PointOfInterestChanges> pointOfInterestChanges=new HashMap<>();



    public final World getWorld()
    {
        return world;
    }
    public AdventurePlayer getPlayer()
    {
        return player;
    }

    public PointOfInterestChanges getPointOfInterestChanges(String id)
    {
        if(!pointOfInterestChanges.containsKey(id))
            pointOfInterestChanges.put(id,new PointOfInterestChanges());
        return pointOfInterestChanges.get(id);
    }

    static public boolean load(int currentSlot) {

        String fileName = WorldSave.getSaveFile(currentSlot);
        new File(getSaveDir()).mkdirs();
        try {
            try(FileInputStream fos  = new FileInputStream(fileName);
                InflaterInputStream inf = new InflaterInputStream(fos);
                ObjectInputStream oos = new ObjectInputStream(inf))
            {
                currentSave.header = (WorldSaveHeader) oos.readObject();
                currentSave.player.readFromSaveFile(oos);
                currentSave.world.readFromSaveFile(oos);
            }
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
            return false;
        } finally {

        }
        return true;
    }
    public static boolean isSafeFile(@NotNull String name) {
        return filenameToSlot(name)!= INVALID_SAVE_SLOT;
    }
    static public int filenameToSlot(String name) {
        if (name.equals("auto_save.sav"))
            return AUTO_SAVE_SLOT;
        if (name.equals("quick_save.sav"))
            return QUICK_SAVE_SLOT;
        if (!name.contains("_") || !name.endsWith(".sav"))
            return INVALID_SAVE_SLOT;
        return Integer.valueOf(name.split("_")[0]);
    }

    static public String filename(int slot) {
        if (slot == AUTO_SAVE_SLOT)
            return "auto_save.sav";
        if (slot == QUICK_SAVE_SLOT)
            return "quick_save.sav";
        return slot + "_save_slot.sav";
    }

    public static String getSaveDir() {
        return ForgeProfileProperties.getUserDir() + File.separator + "Adventure" + File.separator + Config.instance().getPlane();
    }

    public static String getSaveFile(int slot) {
        return ForgeProfileProperties.getUserDir() + File.separator + "Adventure" + File.separator + Config.instance().getPlane() + File.separator + filename(slot);
    }

    public static WorldSave getCurrentSave() {
        return currentSave;
    }

    public static WorldSave generateNewWorld(String name, boolean male, int race, int avatarIndex, int startingDeckIndex, DifficultyData diff, long seed) {

        currentSave.world.generateNew(seed);

        Deck starterDeck = Config.instance().starterDecks()[startingDeckIndex];
        currentSave.player.create(name, starterDeck, male, race, avatarIndex,diff);
        currentSave.player.setWorldPosY((int) (currentSave.world.getData().playerStartPosY * currentSave.world.getData().height * currentSave.world.getTileSize()));
        currentSave.player.setWorldPosX((int) (currentSave.world.getData().playerStartPosX * currentSave.world.getData().width * currentSave.world.getTileSize()));
        return currentSave;
        //return currentSave = ret;
    }

    public boolean save(String text, int currentSlot) {
        header.name = text;

        String fileName = WorldSave.getSaveFile(currentSlot);
        new File(getSaveDir()).mkdirs();

        try {
            try(FileOutputStream fos =  new FileOutputStream(fileName);
                DeflaterOutputStream def= new DeflaterOutputStream(fos);
                ObjectOutputStream oos = new ObjectOutputStream(def))
            {
                oos.writeObject(header);
                player.writeToSaveFile(oos);
                world.writeToSaveFile(oos);
            }

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void dispose() {

        header.dispose();
        player.dispose();
        world.dispose();
    }

}
