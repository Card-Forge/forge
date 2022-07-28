package forge.adventure.world;

import forge.adventure.data.DifficultyData;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.pointofintrest.PointOfInterestChanges;
import forge.adventure.stage.WorldStage;
import forge.adventure.util.Config;
import forge.adventure.util.SaveFileData;
import forge.adventure.util.SignalList;
import forge.deck.Deck;
import forge.deck.DeckgenUtil;
import forge.localinstance.properties.ForgeConstants;
import forge.player.GamePlayerUtil;

import java.io.*;
import java.util.Date;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Represents everything that will be saved, like the player and the world.
 */
public class WorldSave   {

    static final public int AUTO_SAVE_SLOT =-1;
    static final public int QUICK_SAVE_SLOT =-2;
    static final public int INVALID_SAVE_SLOT =-3;
    static final WorldSave currentSave=new WorldSave();
    public WorldSaveHeader header = new WorldSaveHeader();
    private final AdventurePlayer player=new AdventurePlayer();
    private final World world=new World();
    private final PointOfInterestChanges.Map pointOfInterestChanges=  new PointOfInterestChanges.Map();


    private final SignalList onLoadList=new SignalList();

    public final World getWorld()
    {
        return world;
    }
    public AdventurePlayer getPlayer()
    {
        return player;
    }

    public void onLoad(Runnable run)
    {
        onLoadList.add(run);
    }
    public PointOfInterestChanges getPointOfInterestChanges(String id)
    {
        if(!pointOfInterestChanges.containsKey(id))
            pointOfInterestChanges.put(id,new PointOfInterestChanges());
        return pointOfInterestChanges.get(id);
    }

    static public boolean load(int currentSlot) {

        String fileName = WorldSave.getSaveFile(currentSlot);
        if(!new File(fileName).exists())
            return false;
        new File(getSaveDir()).mkdirs();
        try {
            try(FileInputStream fos  = new FileInputStream(fileName);
                InflaterInputStream inf = new InflaterInputStream(fos);
                ObjectInputStream oos = new ObjectInputStream(inf))
            {
                currentSave.header = (WorldSaveHeader) oos.readObject();
                SaveFileData mainData=(SaveFileData)oos.readObject();
                currentSave.player.load(mainData.readSubData("player"));
                GamePlayerUtil.getGuiPlayer().setName(currentSave.player.getName());
                try {
                    currentSave.world.load(mainData.readSubData("world"));
                    currentSave.pointOfInterestChanges.load(mainData.readSubData("pointOfInterestChanges"));
                    WorldStage.getInstance().load(mainData.readSubData("worldStage"));

                } catch (Exception e) {
                    System.err.println("Generating New World");
                    currentSave.world.generateNew(0);
                }

                currentSave.onLoadList.emit();

            }
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    public static boolean isSafeFile(String name) {
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
        return ForgeConstants.USER_ADVENTURE_DIR + Config.instance().getPlane();
    }

    public static String getSaveFile(int slot) {
        return ForgeConstants.USER_ADVENTURE_DIR + Config.instance().getPlane() + File.separator + filename(slot);
    }

    public static WorldSave getCurrentSave() {
        return currentSave;
    }

    public static WorldSave generateNewWorld(String name, boolean male, int race, int avatarIndex, int startingColorIdentity, DifficultyData diff, boolean isFantasy, long seed) {
        currentSave.world.generateNew(seed);
        currentSave.pointOfInterestChanges.clear();
        Deck starterDeck = isFantasy ? DeckgenUtil.getRandomOrPreconOrThemeDeck("", false, false, false) : Config.instance().starterDecks()[startingColorIdentity];
        currentSave.player.create(name, startingColorIdentity, starterDeck, male, race, avatarIndex, isFantasy, diff);
        currentSave.player.setWorldPosY((int) (currentSave.world.getData().playerStartPosY * currentSave.world.getData().height * currentSave.world.getTileSize()));
        currentSave.player.setWorldPosX((int) (currentSave.world.getData().playerStartPosX * currentSave.world.getData().width * currentSave.world.getTileSize()));
        currentSave.onLoadList.emit();
        return currentSave;
        //return currentSave = ret;
    }

    public boolean autoSave() {
        return save("auto save",AUTO_SAVE_SLOT);
    }
    public boolean quickSave() {
        return save("quick save",QUICK_SAVE_SLOT);
    }
    public boolean quickLoad() {
        return load(QUICK_SAVE_SLOT);
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
                header.saveDate= new Date();
                oos.writeObject(header);
                SaveFileData mainData=new SaveFileData();
                mainData.store("player",currentSave.player.save());
                mainData.store("world",currentSave.world.save());
                mainData.store("worldStage", WorldStage.getInstance().save());
                mainData.store("pointOfInterestChanges",currentSave.pointOfInterestChanges.save());

                oos.writeObject(mainData);
            }

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        Config.instance().getSettingData().lastActiveSave = WorldSave.filename(currentSlot);
        Config.instance().saveSettings();
        return true;
    }

    public void clearChanges() {
        pointOfInterestChanges.clear();
    }

}
