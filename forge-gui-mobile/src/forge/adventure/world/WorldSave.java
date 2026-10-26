package forge.adventure.world;

import com.badlogic.gdx.utils.TimeUtils;
import forge.Forge;
import com.badlogic.gdx.Gdx;
import forge.adventure.data.DifficultyData;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.pointofintrest.PointOfInterest;
import forge.adventure.pointofintrest.PointOfInterestChanges;
import forge.adventure.scene.MapViewScene;
import forge.adventure.scene.SaveLoadScene;
import forge.adventure.stage.PointOfInterestMapSprite;
import forge.adventure.stage.WorldStage;
import forge.adventure.util.*;
import forge.card.CardEdition;
import forge.card.ColorSet;
import forge.deck.Deck;
import forge.localinstance.properties.ForgeConstants;
import forge.player.GamePlayerUtil;

import java.io.*;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Represents everything that will be saved, like the player and the world.
 */
public class WorldSave {

    static final public int AUTO_SAVE_SLOT = -1;
    static final public int QUICK_SAVE_SLOT = -2;
    static final public int INVALID_SAVE_SLOT = -3;
    static final WorldSave currentSave = new WorldSave();
    public WorldSaveHeader header = new WorldSaveHeader();
    private final AdventurePlayer player = new AdventurePlayer();
    private final World world = new World();
    private final PointOfInterestChanges.Map pointOfInterestChanges = new PointOfInterestChanges.Map();


    private final SignalList onLoadList = new SignalList();
    private static long lastPreviewTimestamp = 0L;
    private static final long COOLDOWN_WINDOW_MS = 800L;
    private static boolean firstCapture = true;
    private static final ExecutorService SAVE_EXECUTOR =
        Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "Forge-Adventure-Save");
            thread.setDaemon(true);
            return thread;
        });

    public final World getWorld() {
        return world;
    }

    public AdventurePlayer getPlayer() {
        return player;
    }

    public void onLoad(Runnable run) {
        onLoadList.add(run);
    }

    public PointOfInterestChanges getPointOfInterestChanges(String id) {
        if (id == null) { // fallback
            return new PointOfInterestChanges();
        }

        PointOfInterestChanges changes = pointOfInterestChanges.get(id);
        if (changes == null) {
            changes = new PointOfInterestChanges();
            pointOfInterestChanges.put(id, changes);
        }

        return changes;
    }

    static public boolean load(int currentSlot) {
        JSONStringLoader.clearCache();
        CardUtil.clearPriceCache();
        Forge.getLocalizer().loadAdventureBundle(Config.instance().getPlanePath(Config.instance().getSettingData().plane) + "languages/");

        Forge.invokeWorldSave = true; // This is for dispose method check
        String fileName = WorldSave.getSaveFile(currentSlot);
        if (!new File(fileName).exists())
            return false;
        new File(getSaveDir()).mkdirs();
        try {
            try (FileInputStream fos = new FileInputStream(fileName);
                 InflaterInputStream inf = new InflaterInputStream(fos);
                 ObjectInputStream oos = new ObjectInputStream(inf)) {
                currentSave.header = (WorldSaveHeader) oos.readObject();
                SaveFileData mainData = (SaveFileData) oos.readObject();
                currentSave.player.load(mainData.readSubData("player"));
                GamePlayerUtil.getGuiPlayer().setName(currentSave.player.getName());
                try {
                    currentSave.world.load(mainData.readSubData("world"));
                    currentSave.pointOfInterestChanges.load(mainData.readSubData("pointOfInterestChanges"));
                    WorldStage.getInstance().load(mainData.readSubData("worldStage"));

                } catch (Exception e) {
                    System.err.println("Generating New World");
                    if (!currentSave.world.generateNew(0))
                        return false;
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
        return filenameToSlot(name) != INVALID_SAVE_SLOT;
    }

    static public int filenameToSlot(String name) {
        if (name.equals("auto_save.sav"))
            return AUTO_SAVE_SLOT;
        if (name.equals("quick_save.sav"))
            return QUICK_SAVE_SLOT;
        if (!name.contains("_") || !name.endsWith(".sav"))
            return INVALID_SAVE_SLOT;
        return Integer.parseInt(name.split("_")[0]);
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

    public static WorldSave generateNewWorld(String name, boolean male, int race, int avatarIndex, ColorSet startingColorIdentity, DifficultyData diff, AdventureModes mode, int customDeckIndex, CardEdition starterEdition, long seed) {
        Forge.getLocalizer().loadAdventureBundle(Config.instance().getPlanePath(Config.instance().getSettingData().plane) + "languages/");
        currentSave.world.generateNew(seed);
        currentSave.pointOfInterestChanges.clear();
        boolean chaos = mode == AdventureModes.Chaos;
        boolean custom = mode == AdventureModes.Custom;

        Deck starterDeck = Config.instance().starterDeck(startingColorIdentity, diff, mode, customDeckIndex, starterEdition);
        currentSave.player.create(name, starterDeck, male, race, avatarIndex, chaos, custom, diff, mode);

        currentSave.player.setWorldPosY((int) (currentSave.world.getData().playerStartPosY * currentSave.world.getData().height * currentSave.world.getTileSize()));
        currentSave.player.setWorldPosX((int) (currentSave.world.getData().playerStartPosX * currentSave.world.getData().width * currentSave.world.getTileSize()));
        currentSave.onLoadList.emit();
        return currentSave;
    }

    public boolean autoSave() {
        return save("auto save" + SaveLoadScene.instance().getSaveFileSuffix(), AUTO_SAVE_SLOT);
    }

    public boolean quickSave() {
        return save("quick save" + SaveLoadScene.instance().getSaveFileSuffix(), QUICK_SAVE_SLOT);
    }

    public boolean quickLoad() {
        return load(QUICK_SAVE_SLOT);
    }

    private void writeSaveSnapshot(
            String text,
            int currentSlot,
            WorldSaveHeader saveHeader,
            SaveFileData mainData) throws IOException {

        File saveDir = new File(getSaveDir());
        if (!saveDir.exists() && !saveDir.mkdirs() && !saveDir.isDirectory()) {
            throw new IOException("Could not create save directory: " + saveDir);
        }

        File currentFile = new File(WorldSave.getSaveFile(currentSlot));
        File backupFile = new File(currentFile.getPath().replace(".sav", ".old"));
        File tempFile = File.createTempFile("forge-save-", ".tmp", saveDir);
        boolean replaced = false;

        try {
            // Write to a separate file. Never expose a partially written .sav to the UI.
            try (FileOutputStream fos = new FileOutputStream(tempFile);
                 DeflaterOutputStream def = new DeflaterOutputStream(fos);
                 ObjectOutputStream oos = new ObjectOutputStream(def)) {
                oos.writeObject(saveHeader);
                oos.writeObject(mainData);
            }

            // Verify the completed compressed stream before replacing the current save.
            validateSaveFile(tempFile);

            // Keep the last known-good save as a recovery copy. Copying does not make
            // the live .sav disappear while the new save is being prepared.
            if (currentFile.isFile()) {
                Files.copy(currentFile.toPath(), backupFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }

            try {
                Files.move(tempFile.toPath(), currentFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                // Some filesystems do not support atomic rename. The source is still
                // a fully written and validated file, so use the best available move.
                Files.move(tempFile.toPath(), currentFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
            replaced = true;
        } finally {
            if (tempFile.exists() && !tempFile.delete()) {
                System.err.println("Could not delete temporary save: " + tempFile);
            }
            // If replacement failed, currentFile was never intentionally deleted or
            // renamed. Leave the .old recovery copy in place for manual recovery.
            if (!replaced && backupFile.exists()) {
                System.err.println("Save replacement failed; recovery copy retained: " + backupFile);
            }
        }
    }

    private void validateSaveFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             InflaterInputStream inflater = new InflaterInputStream(fis);
             ObjectInputStream in = new ObjectInputStream(inflater)) {
            Object headerObject;
            Object dataObject;
            try {
                headerObject = in.readObject();
                dataObject = in.readObject();
            } catch (ClassNotFoundException e) {
                throw new IOException("Could not validate save file: " + file, e);
            }

            if (!(headerObject instanceof WorldSaveHeader)
                    || !(dataObject instanceof SaveFileData)) {
                throw new IOException("Invalid save file contents: " + file);
            }

            // Reading the header decodes a native Pixmap; release it on this worker.
            ((WorldSaveHeader) headerObject).dispose();
        }
    }

    public boolean autoSaveAsync() {
        final int slot = AUTO_SAVE_SLOT;
        final String text =
                "auto save" + SaveLoadScene.instance().getSaveFileSuffix();

        final WorldSaveHeader saveHeader;
        final SaveFileData playerData;
        final SaveFileData worldStageData;
        final SaveFileData poiData;
        final World.AsyncSaveSnapshot worldSnapshot;

        World.AsyncSaveSnapshot capturedWorld = null;
        try {
            // Capture all live game state on the calling thread. The world snapshot
            // copies the biome Pixmap and serializes the maps, but defers PNG encoding.
            header.name = text;
            playerData = currentSave.player.save();
            capturedWorld = currentSave.world.snapshotForAsyncSave();
            worldStageData = WorldStage.getInstance().save();
            poiData = currentSave.pointOfInterestChanges.save();

            String message = getExceptionMessage(playerData, worldStageData, poiData);
            if (!message.isEmpty()) {
                capturedWorld.dispose();
                finish(message);
                return false;
            }

            // Snapshot the header on the calling thread; the worker must not read
            // the live header or its native Pixmap.
            header.saveDate = new Date();
            saveHeader = header.snapshotForAsyncSave();
            worldSnapshot = capturedWorld;
        } catch (Exception e) {
            if (capturedWorld != null) {
                capturedWorld.dispose();
            }
            e.printStackTrace();
            finish("Please check forge.log for errors.");
            return false;
        }

        // Only detached snapshots and serialized data cross onto the worker thread.
        try {
            SAVE_EXECUTOR.execute(() -> {
                String error = null;
                try {
                    // PNG encoding is the expensive part; do it off the UI/game thread.
                    SaveFileData worldData = worldSnapshot.encode();
                    String message = getExceptionMessage(
                            playerData, worldData, worldStageData, poiData);
                    if (!message.isEmpty()) {
                        throw new IOException(message);
                    }

                    SaveFileData mainData = new SaveFileData();
                    mainData.store("player", playerData);
                    mainData.store("world", worldData);
                    mainData.store("worldStage", worldStageData);
                    mainData.store("pointOfInterestChanges", poiData);

                    if (mainData.readString("IOException") != null) {
                        throw new IOException("Could not assemble save data.");
                    }

                    writeSaveSnapshot(text, slot, saveHeader, mainData);
                } catch (Exception e) {
                    e.printStackTrace();
                    error = "Please check forge.log for errors.";
                } finally {
                    // encode() normally consumes the Pixmap; dispose() is also safe
                    // if encoding failed before consuming it.
                    worldSnapshot.dispose();
                }

                final String result = error;
                Gdx.app.postRunnable(() -> {
                    if (result == null) {
                        Config.instance().getSettingData().lastActiveSave =
                                WorldSave.filename(slot);
                        Config.instance().saveSettings();
                    }
                    finish(result);
                });
            });
        } catch (RuntimeException e) {
            // For example, the executor may reject the task during shutdown.
            worldSnapshot.dispose();
            e.printStackTrace();
            finish("Please check forge.log for errors.");
            return false;
        }

        // Means the save was queued, not that disk writing has finished.
        return true;
    }

    public boolean save(String text, int currentSlot) {
        header.name = text;

        String fileName = WorldSave.getSaveFile(currentSlot);
        String oldFileName = fileName.replace(".sav", ".old");
        new File(getSaveDir()).mkdirs();
        File currentFile = new File(fileName);
        File backupFile = new File(oldFileName);
        if (currentFile.exists())
            currentFile.renameTo(backupFile);

        try {
            try (FileOutputStream fos = new FileOutputStream(fileName);
                 DeflaterOutputStream def = new DeflaterOutputStream(fos);
                 ObjectOutputStream oos = new ObjectOutputStream(def)) {
                SaveFileData player = currentSave.player.save();
                SaveFileData world = currentSave.world.save();
                SaveFileData worldStage = WorldStage.getInstance().save();
                SaveFileData poiChanges = currentSave.pointOfInterestChanges.save();

                String message = getExceptionMessage(player, world, worldStage, poiChanges);
                if (!message.isEmpty()) {
                    oos.close();
                    fos.close();
                    restoreBackup(oldFileName, fileName);
                    finish(message);
                    return true;
                }

                SaveFileData mainData = new SaveFileData();
                mainData.store("player", player);
                mainData.store("world", world);
                mainData.store("worldStage", worldStage);
                mainData.store("pointOfInterestChanges", poiChanges);

                if (mainData.readString("IOException") != null) {
                    oos.close();
                    fos.close();
                    restoreBackup(oldFileName, fileName);
                    finish("Please check forge.log for errors.");
                    return true;
                }

                header.saveDate = new Date();
                oos.writeObject(header);
                oos.writeObject(mainData);
            }

        } catch (IOException e) {
            restoreBackup(oldFileName, fileName);
            finish("Please check forge.log for errors.");
            return true;
        }

        Config.instance().getSettingData().lastActiveSave = WorldSave.filename(currentSlot);
        Config.instance().saveSettings();
        if (backupFile.exists())
            backupFile.delete();
        finish(null);
        return true;
    }

    private void finish(String errors) {
        if (errors != null)
            announceError(errors);
    }

    public void restoreBackup(String oldFilename, String currentFilename) {
        File f = new File(currentFilename);
        if (f.exists())
            f.delete();
        File b = new File(oldFilename);
        if (b.exists())
            b.renameTo(new File(currentFilename));
    }

    public String getExceptionMessage(SaveFileData... datas) {
        StringBuilder message = new StringBuilder();

        for (SaveFileData data : datas) {
          String s = data.readString("IOException");
          if (s != null)
              message.append(s).append("\n");
        }

        return message.toString();
    }

    private void announceError(String message) {
        currentSave.player.getCurrentGameStage().setExtraAnnouncement("Error Saving File!\n" + message);
    }

    public void clearChanges() {
        pointOfInterestChanges.clear();
    }

    public void clearBookmarks() {
        for (PointOfInterest poi : currentSave.world.getAllPointOfInterest()) {
            if (poi == null)
                continue;
            PointOfInterestMapSprite mapSprite = WorldStage.getInstance().getMapSprite(poi);
            if (mapSprite != null)
                mapSprite.setBookmarked(false, poi);
            PointOfInterestChanges p = pointOfInterestChanges.get(poi.getID());
            if (p == null)
                continue;
            p.setIsBookmarked(false);
            p.save();
        }
        MapViewScene.instance().clearBookMarks();
    }

    // prevent spam of preview if user repeatedly/accidentally reopen scene that request previews
    public static void requestPreview() {
        final long currentTimestamp = TimeUtils.millis();

        if (firstCapture) {
            firstCapture = false;
            // init once
            currentSave.header.createPreview();
            return;
        }

        // If 800ms has not passed since the last successful generation, skip
        if (currentTimestamp - lastPreviewTimestamp < COOLDOWN_WINDOW_MS) {
            return;
        }

        // Update the timestamp immediately to lock out parallel thread spam on the spot
        lastPreviewTimestamp = currentTimestamp;
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                currentSave.header.createPreview();
            }
        });
    }

    public static void dispose() {
        Forge.safeDispose(currentSave.world);
    }
}
