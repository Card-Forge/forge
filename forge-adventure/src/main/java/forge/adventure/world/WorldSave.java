package forge.adventure.world;

import forge.adventure.util.Res;
import forge.deck.Deck;
import forge.localinstance.properties.ForgeProfileProperties;

import java.io.*;

public class WorldSave {
    public void Save(String text, int currentSlot) {
        header.name=text;

        String fileName=WorldSave.GetSaveFile(currentSlot);
        new File(GetSaveDir()).mkdirs();
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(fileName);
            ObjectOutputStream oos = new ObjectOutputStream (fos) ;
            oos.writeObject(header);
            oos.close();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void Load(int currentSlot) {

    }
    static public int FilenameToSlot(String name)
    {
        if(name=="autosave.sav")
            return -2;
        if(name=="quicksave.sav")
            return -1;
        if(!name.contains("_")||!name.endsWith(".sav"))
            return -3;
        return Integer.valueOf(name.split("_")[0]);
    }
    static public String Filename(int slot)
    {
        if(slot==-2)
            return "autosave.sav";
        if(slot==-1)
            return "quicksave.sav";
        return slot+"_saveslot.sav";
    }


    public static String GetSaveDir() {
        return ForgeProfileProperties.getUserDir()+File.separator+"Adventure"+File.separator+ Res.CurrentRes.GetPlane();
    }
    public static String GetSaveFile(int slot) {
        return ForgeProfileProperties.getUserDir()+File.separator+"Adventure"+File.separator+ Res.CurrentRes.GetPlane()+File.separator+Filename(slot);
    }


    public enum Difficulty
    {
        Easy,
        Medium,
        Hard
    }
    public AdventurePlayer player;
    public World world;
    public Difficulty difficulty;

    static WorldSave currentSave;
    public WorldSaveHeader header=new WorldSaveHeader();

    public static WorldSave getCurrentSave()
    {
        return currentSave;
    }
    public static WorldSave GenerateNewWorld(String name , Deck startingDeck, Difficulty diff)
    {
        currentSave=new WorldSave();
        currentSave.world=new World();
        currentSave.world.GenerateNew();
        currentSave.player=new AdventurePlayer(name,startingDeck,diff);
        currentSave.player.setWorldPosY((int)(currentSave.world.GetData().playerStartPosY*currentSave.world.GetData().height*currentSave.world.GetTileSize()));
        currentSave.player.setWorldPosX((int)(currentSave.world.GetData().playerStartPosX*currentSave.world.GetData().width*currentSave.world.GetTileSize()));
        return currentSave;
        //return currentSave = ret;
    }
}
