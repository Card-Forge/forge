package forge.adventure.world;

import forge.adventure.util.Res;
import forge.deck.Deck;
import forge.localinstance.properties.ForgeProfileProperties;

import java.io.*;
import java.util.HashMap;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class WorldSave {

    static WorldSave currentSave=new WorldSave();
    public WorldSaveHeader header = new WorldSaveHeader();
    private final AdventurePlayer player=new AdventurePlayer();
    private final World world=new World();
    private final HashMap<String,PointOfInterestChanges> pointOfInterestChanges=new HashMap<>();
    public Difficulty difficulty;

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

        String fileName = WorldSave.GetSaveFile(currentSlot);
        new File(GetSaveDir()).mkdirs();
        try {
            try(FileInputStream fos  = new FileInputStream(fileName);
                InflaterInputStream inf = new InflaterInputStream(fos);
                ObjectInputStream oos = new ObjectInputStream(inf))
            {
                currentSave.header = (WorldSaveHeader) oos.readObject();
                currentSave.player.readFromSaveFile(oos);
                currentSave.world.readFromSaveFile(oos);
                currentSave.difficulty = (Difficulty) oos.readObject();
            }
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
            return false;
        } finally {

        }
        return true;
    }

    static public int FilenameToSlot(String name) {
        if (name == "autosave.sav")
            return -2;
        if (name == "quicksave.sav")
            return -1;
        if (!name.contains("_") || !name.endsWith(".sav"))
            return -3;
        return Integer.valueOf(name.split("_")[0]);
    }

    static public String Filename(int slot) {
        if (slot == -2)
            return "autosave.sav";
        if (slot == -1)
            return "quicksave.sav";
        return slot + "_saveslot.sav";
    }

    public static String GetSaveDir() {
        return ForgeProfileProperties.getUserDir() + File.separator + "Adventure" + File.separator + Res.CurrentRes.GetPlane();
    }

    public static String GetSaveFile(int slot) {
        return ForgeProfileProperties.getUserDir() + File.separator + "Adventure" + File.separator + Res.CurrentRes.GetPlane() + File.separator + Filename(slot);
    }

    public static WorldSave getCurrentSave() {
        return currentSave;
    }

    public static WorldSave generateNewWorld(String name, boolean male, int race, int avatarIndex, Deck startingDeck, Difficulty diff, long seed) {

        currentSave.world.generateNew(seed);
        currentSave.player.create(name, startingDeck, male, race, avatarIndex);
        currentSave.difficulty = diff;
        currentSave.player.setWorldPosY((int) (currentSave.world.getData().playerStartPosY * currentSave.world.getData().height * currentSave.world.getTileSize()));
        currentSave.player.setWorldPosX((int) (currentSave.world.getData().playerStartPosX * currentSave.world.getData().width * currentSave.world.getTileSize()));
        return currentSave;
        //return currentSave = ret;
    }

    public boolean save(String text, int currentSlot) {
        header.name = text;

        String fileName = WorldSave.GetSaveFile(currentSlot);
        new File(GetSaveDir()).mkdirs();

        try {
            try(FileOutputStream fos =  new FileOutputStream(fileName);
                DeflaterOutputStream def= new DeflaterOutputStream(fos);
                ObjectOutputStream oos = new ObjectOutputStream(def))
            {
                oos.writeObject(header);
                player.writeToSaveFile(oos);
                world.writeToSaveFile(oos);
                oos.writeObject(difficulty);
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

    public enum Difficulty {
        Easy,
        Medium,
        Hard
    }
}
