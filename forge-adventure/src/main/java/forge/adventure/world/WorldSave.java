package forge.adventure.world;

import forge.adventure.util.Res;
import forge.deck.Deck;
import forge.localinstance.properties.ForgeProfileProperties;

import java.io.*;

public class WorldSave {

    static WorldSave currentSave;
    public WorldSaveHeader header = new WorldSaveHeader();
    public AdventurePlayer player;
    public World world;
    public Difficulty difficulty;

    static public void Load(int currentSlot) {
        if (currentSave != null)
            currentSave.dispose();
        currentSave = new WorldSave();
        String fileName = WorldSave.GetSaveFile(currentSlot);
        new File(GetSaveDir()).mkdirs();
        FileInputStream fos = null;
        try {
            fos = new FileInputStream(fileName);
            ObjectInputStream oos = new ObjectInputStream(fos);
            currentSave.header = (WorldSaveHeader) oos.readObject();
            currentSave.player = (AdventurePlayer) oos.readObject();
            currentSave.world = (World) oos.readObject();
            currentSave.difficulty = (Difficulty) oos.readObject();
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
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

    public static WorldSave GenerateNewWorld(String name, boolean male, int race, int avatarIndex, Deck startingDeck, Difficulty diff) {
        currentSave = new WorldSave();
        currentSave.world = new World();
        currentSave.world.GenerateNew();
        currentSave.player = new AdventurePlayer(name, startingDeck, male, race, avatarIndex);
        currentSave.difficulty = diff;
        currentSave.player.setWorldPosY((int) (currentSave.world.GetData().playerStartPosY * currentSave.world.GetData().height * currentSave.world.GetTileSize()));
        currentSave.player.setWorldPosX((int) (currentSave.world.GetData().playerStartPosX * currentSave.world.GetData().width * currentSave.world.GetTileSize()));
        return currentSave;
        //return currentSave = ret;
    }

    public void Save(String text, int currentSlot) {
        header.name = text;

        String fileName = WorldSave.GetSaveFile(currentSlot);
        new File(GetSaveDir()).mkdirs();
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(fileName);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(header);
            oos.writeObject(player);
            oos.writeObject(world);
            oos.writeObject(difficulty);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
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
