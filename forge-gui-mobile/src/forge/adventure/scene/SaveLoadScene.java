package forge.adventure.scene;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.Scaling;
import com.github.tommyettinger.textra.TextraButton;
import com.github.tommyettinger.textra.TextraLabel;
import forge.Forge;
import forge.adventure.data.DifficultyData;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.stage.WorldStage;
import forge.adventure.util.Config;
import forge.adventure.util.Controls;
import forge.adventure.util.Current;
import forge.adventure.world.WorldSave;
import forge.adventure.world.WorldSaveHeader;
import forge.screens.TransitionScreen;
import forge.sound.SoundSystem;
import forge.util.TextUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.DateFormat;
import java.util.zip.InflaterInputStream;

/**
 * Scene to load and save the game.
 */
public class SaveLoadScene extends UIScene {
    private static final int NUMBEROFSAVESLOTS = 11;
    private final IntMap<Selectable<TextraButton>> buttons = new IntMap<>();
    IntMap<WorldSaveHeader> previews = new IntMap<>();
    Table layout;
    Modes mode;
    TextField textInput;
    TextraLabel header;
    int currentSlot = 0, lastSelectedSlot = 0;
    Image previewImage;
    TextraLabel previewDate, playerLocation;
    TextraButton saveLoadButton, back;
    Selectable<TextraButton> quickSave;
    Selectable<TextraButton> autoSave;
    SelectBox difficulty;
    ScrollPane scrollPane;
    char ASCII_179 = '│';
    Dialog saveDialog;
    TextraButton sortToggleButton;
    enum SortMode { SLOT, RECENT }
    SortMode sortMode = SortMode.SLOT;

    private SaveLoadScene() {
        super(Forge.isLandscapeMode() ? "ui/save_load.json" : "ui/save_load_portrait.json");

        Table root = new Table();
        layout = new Table();
        scrollPane = new ScrollPane(layout);
        Window window = ui.findActor("saveSlots");
        window.add(root);
        textInput = Controls.newTextField("");
        int c = 0;
        String[] diffList = new String[Config.instance().getConfigData().difficulties.length];
        for (DifficultyData diff : Config.instance().getConfigData().difficulties) {
            diffList[c] = Forge.getLocalizer().getMessageorUseDefault("lbl" + diff.name, diff.name);
            c++;
        }

        difficulty = Controls.newComboBox(diffList, null, o -> {
            //DifficultyData difficulty1 = Config.instance().getConfigData().difficulties[difficulty.getSelectedIndex()];
            return null;
        });
        previewImage = ui.findActor("preview");
        previewDate = ui.findActor("saveDate");
        playerLocation = Controls.newTextraLabel("");
        playerLocation.setText("");
        playerLocation.setX(previewImage.getX());
        playerLocation.setY(previewImage.getY() + 5);
        ui.addActor(playerLocation);
        header = Controls.newTextraLabel(Forge.getLocalizer().getMessage("lblSave"));
        root.row();
        root.add(header).grow();
        root.add(difficulty);
        root.row();
        root.add(scrollPane).colspan(2).width(window.getWidth() - 20);
        autoSave = addSaveSlot(Forge.getLocalizer().getMessage("lblAutoSave"), WorldSave.AUTO_SAVE_SLOT);
        quickSave = addSaveSlot(Forge.getLocalizer().getMessage("lblQuickSave"), WorldSave.QUICK_SAVE_SLOT);
        for (int i = 1; i < NUMBEROFSAVESLOTS; i++)
            addSaveSlot(Forge.getLocalizer().getMessage("lblSlot") + ": " + i, i);

        saveLoadButton = ui.findActor("save");
        saveLoadButton.setText(Forge.getLocalizer().getMessage("lblSave"));
        ui.onButtonPress("save", SaveLoadScene.this::loadSave);
        back = ui.findActor("return");
        ui.onButtonPress("return", SaveLoadScene.this::back);
        difficulty.setSelectedIndex(1);
        difficulty.setAlignment(Align.center);
        difficulty.setX(scrollPane.getWidth() - difficulty.getWidth() + 5);
        difficulty.setY(scrollPane.getTop() - difficulty.getHeight() - 5);

        // Add sort toggle button logic
        sortToggleButton = ui.findActor("sortToggle");
        if (sortToggleButton != null) {
            sortToggleButton.setText("Sort by Recent");
            sortToggleButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    toggleSortMode();
                }
            });
        }
    }

    private void toggleSortMode() {
        if (sortMode == SortMode.SLOT) {
            sortMode = SortMode.RECENT;
            if (sortToggleButton != null) {
                sortToggleButton.setText("Sort by Slot");
            }
        } else {
            sortMode = SortMode.SLOT;
            if (sortToggleButton != null) {
                sortToggleButton.setText("Sort by Recent");
            }
        }
        refreshSaveSlots();
    }

    private void refreshSaveSlots() {
        layout.clear();
        buttons.clear();
        addSaveSlot(Forge.getLocalizer().getMessage("lblAutoSave"), WorldSave.AUTO_SAVE_SLOT);
        addSaveSlot(Forge.getLocalizer().getMessage("lblQuickSave"), WorldSave.QUICK_SAVE_SLOT);
        java.util.List<Integer> slotOrder = new java.util.ArrayList<>();
        for (int i = 1; i < NUMBEROFSAVESLOTS; i++) {
            slotOrder.add(i);
        }
        if (sortMode == SortMode.RECENT) {
            // Sort by most recent save date (descending)
            java.util.Map<Integer, java.util.Date> slotDates = new java.util.HashMap<>();
            for (int i : slotOrder) {
                WorldSaveHeader h = previews.get(i);
                if (h != null && h.saveDate != null) slotDates.put(i, h.saveDate);
            }
            slotOrder.sort((a, b) -> {
                java.util.Date da = slotDates.get(a);
                java.util.Date db = slotDates.get(b);
                if (da == null && db == null) return Integer.compare(a, b);
                if (da == null) return 1;
                if (db == null) return -1;
                return db.compareTo(da);
            });
        }
        for (int i : slotOrder) {
            addSaveSlot(Forge.getLocalizer().getMessage("lblSlot") + ": " + i, i);
        }
        layout.invalidateHierarchy();
    }

    private static SaveLoadScene object;

    public static SaveLoadScene instance() {
        if (object == null)
            object = new SaveLoadScene();
        return object;
    }

    public class SaveSlot extends Selectable<TextraButton> {
        private final int slotNumber;

        public SaveSlot(int slotNumber) {
            super(Controls.newTextButton("..."));
            this.slotNumber = slotNumber;
            SaveSlot self = this;
            actor.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    try {
                        if (!actor.isDisabled()) {
                            selectActor(self);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void onSelect(UIScene scene) {
            super.onSelect(scene);
            updateSlot(slotNumber);
        }
    }

    private Selectable<TextraButton> addSaveSlot(String name, int i) {
        String displayName = name;
        if (previews.containsKey(i)) {
            WorldSaveHeader header = previews.get(i);
            if (header != null) {
                displayName = getSplitHeaderName(header, false);
            }
        }
        layout.add(Controls.newLabel(name)).align(Align.left).pad(2, 5, 2, 10);
        SaveSlot button = new SaveSlot(i);
        button.actor.setText(displayName);
        layout.add(button.actor).fill(true, false).expand(true, false).align(Align.left).expandX();
        buttons.put(i, button);
        layout.row();
        addToSelectable(button);
        return button;

    }

    public boolean select(int slot) {
        if (!buttons.containsKey(slot))
            return false;
        selectActor(buttons.get(slot));
        return updateSlot(slot);
    }

    private boolean updateSlot(int slot) {

        currentSlot = slot;
        if (slot > 0)
            lastSelectedSlot = slot;
        if (previews.containsKey(slot)) {
            WorldSaveHeader worldSaveHeader = previews.get(slot);
            if (worldSaveHeader.preview != null) {
                previewImage.setDrawable(new TextureRegionDrawable(new Texture(worldSaveHeader.preview)));
                previewImage.setScaling(Scaling.fit);
                previewImage.layout();
                previewImage.setVisible(true);
                previewDate.setVisible(true);
                if (worldSaveHeader.saveDate != null)
                    previewDate.setText("[%98]" + DateFormat.getDateInstance().format(worldSaveHeader.saveDate) + " " + DateFormat.getTimeInstance(DateFormat.SHORT).format(worldSaveHeader.saveDate));
                else
                    previewDate.setText("");
                //getLocation
                playerLocation.setText(getSplitHeaderName(worldSaveHeader, true));
            }
        } else {
            if (previewImage != null)
                previewImage.setVisible(false);
            if (previewDate != null)
                previewDate.setVisible(false);
        }
        return true;
    }

    boolean loaded = false;

    public void loadSave() {
        if (loaded)
            return;
        loaded = true;
        switch (mode) {
            case Save:
                if (TileMapScene.instance().currentMap().isInMap()) {
                    //Access to screen should be disabled, but stop the process just in case.
                    //Saving needs to be disabled inside maps until we can capture and load exact map state
                    //Otherwise location based events for quests can be skipped by saving and then loading outside the map
                    Dialog noSave = createGenericDialog("", Forge.getLocalizer().getMessage("lblGameNotSaved"), Forge.getLocalizer().getMessage("lblOK"), null, null, null);
                    showDialog(noSave);
                    return;
                }
                if (currentSlot > 0) {
                    //prevent NPE, allowed saveslot is 1 to 10..
                    textInput.setText(buttons.get(currentSlot).actor.getText());
                    if (saveDialog == null) {
                        saveDialog = createGenericDialog(Forge.getLocalizer().getMessage("lblSave"), null,
                                Forge.getLocalizer().getMessage("lblOK"),
                                Forge.getLocalizer().getMessage("lblAbort"), () -> {
                                    this.save();
                                    removeDialog();
                                }, this::removeDialog);
                        saveDialog.getContentTable().add(Controls.newLabel(Forge.getLocalizer().getMessage("lblNameYourSaveFile"))).colspan(2).pad(2, 15, 2, 15);
                        saveDialog.getContentTable().row();
                        saveDialog.getContentTable().add(Controls.newLabel(Forge.getLocalizer().getMessage("lblName") + ": ")).align(Align.left).pad(2, 15, 2, 2);
                        saveDialog.getContentTable().add(textInput).fillX().expandX().padRight(15);
                        saveDialog.getContentTable().row();
                    }
                    showDialog(saveDialog);
                    stage.setKeyboardFocus(textInput);
                }
                loaded = false;
                break;
            case Load:
                try {
                    MapViewScene.instance().clearBookMarks();
                    Forge.setTransitionScreen(new TransitionScreen(() -> {
                        loaded = false;
                        if (WorldSave.load(currentSlot)) {
                            SoundSystem.instance.changeBackgroundTrack();
                            Forge.switchScene(GameScene.instance());
                        } else {
                            Forge.clearTransitionScreen();
                        }
                    }, null, false, true, Forge.getLocalizer().getMessage("lblLoadingWorld")));
                } catch (Exception e) {
                    Forge.clearTransitionScreen();
                }
                break;
            case NewGamePlus:
                try {
                    Forge.setTransitionScreen(new TransitionScreen(() -> {
                        loaded = false;
                        if (WorldSave.load(currentSlot)) {
                            WorldSave.getCurrentSave().clearChanges();
                            if (WorldSave.getCurrentSave().getWorld().generateNew(0)) {
                                if (difficulty != null)
                                    Current.player().updateDifficulty(Config.instance().getConfigData().difficulties[difficulty.getSelectedIndex()]);
                                Current.player().setWorldPosY((int) (WorldSave.getCurrentSave().getWorld().getData().playerStartPosY * WorldSave.getCurrentSave().getWorld().getData().height * WorldSave.getCurrentSave().getWorld().getTileSize()));
                                Current.player().setWorldPosX((int) (WorldSave.getCurrentSave().getWorld().getData().playerStartPosX * WorldSave.getCurrentSave().getWorld().getData().width * WorldSave.getCurrentSave().getWorld().getTileSize()));
                                Current.player().getQuests().clear();
                                Current.player().resetQuestFlags();
                                Current.player().setCharacterFlag("newGamePlus", 1);
                                Current.player().removeAllQuestItems();
                                AdventurePlayer.current().addQuest("28", true);
                                WorldSave.getCurrentSave().clearBookmarks();
                                WorldStage.getInstance().enterSpawnPOI();
                                SoundSystem.instance.changeBackgroundTrack();
                                Forge.switchScene(GameScene.instance());
                            } else {
                                Forge.clearTransitionScreen();
                            }
                        } else {
                            Forge.clearTransitionScreen();
                        }
                    }, null, false, true, Forge.getLocalizer().getMessage("lblGeneratingWorld")));
                } catch (Exception e) {
                    loaded = false;
                    Forge.clearTransitionScreen();
                }
                break;
        }
    }


    public void save() {
        if (!TileMapScene.instance().currentMap().isInMap()) {
            if (WorldSave.getCurrentSave().save(textInput.getText() + getSaveFileSuffix(), currentSlot)) {
                updateFiles();
                //ensure the dialog is hidden before switching

                Scene restoreScene = Forge.switchToLast();
                if (restoreScene != null) {
                    restoreScene = Forge.switchToLast();
                }

                if (restoreScene == null) {
                    restoreScene = GameScene.instance();
                }

                Forge.switchScene(restoreScene);
            }
        }
    }

    private void updateFiles() {

        File f = new File(WorldSave.getSaveDir());
        f.mkdirs();
        File[] names = f.listFiles();
        if (names == null)
            throw new RuntimeException("Can not find save directory");
        previews.clear();
        for (File name : names) {
            if (WorldSave.isSafeFile(name.getName())) {
                try {
                    try (FileInputStream fos = new FileInputStream(name.getAbsolutePath());
                         InflaterInputStream inf = new InflaterInputStream(fos);
                         ObjectInputStream oos = new ObjectInputStream(inf)) {
                        int slot = WorldSave.filenameToSlot(name.getName());
                        WorldSaveHeader worldSaveHeader = (WorldSaveHeader) oos.readObject();
                        previews.put(slot, worldSaveHeader);
                    }
                } catch (ClassNotFoundException | IOException | GdxRuntimeException e) {
                    //e.printStackTrace();
                }
            }
        }
        refreshSaveSlots();
    }

    private String getSplitHeaderName(WorldSaveHeader worldSaveHeader, boolean getLocation) {
        String noMapData = "[RED]No Map Data!";
        if (worldSaveHeader.name.contains(Character.toString(ASCII_179))) {
            String[] split = TextUtil.split(worldSaveHeader.name, ASCII_179);
            if (getLocation) // unicode symbols with \\uFFxx blackout the stage using TextraTypist 2.x.x
                return split.length > 1 ? split[1].replaceAll("\uFF0A", "• ") : noMapData;
            else
                return split[0];
        }
        return getLocation ? noMapData : worldSaveHeader.name;
    }

    public enum Modes {
        Save,
        Load,
        NewGamePlus
    }

    public void setMode(Modes mode) {
        switch (mode) {
            case Save:
                header.setText(Forge.getLocalizer().getMessage("lblSaveGame"));
                saveLoadButton.setText(Forge.getLocalizer().getMessage("lblSave"));
                break;
            case Load:
                header.setText(Forge.getLocalizer().getMessage("lblLoadGame"));
                saveLoadButton.setText(Forge.getLocalizer().getMessage("lblLoad"));
                break;
            case NewGamePlus:
                header.setText(Forge.getLocalizer().getMessage("lblNewGame") + "+");
                saveLoadButton.setText(Forge.getLocalizer().getMessage("lblStart"));
                break;
        }
        autoSave.actor.setDisabled(mode == Modes.Save);
        quickSave.actor.setDisabled(mode == Modes.Save);
        this.mode = mode;
    }

    @Override
    public void enter() {
        unselectActors();
        select(lastSelectedSlot);
        updateFiles();
        autoSave.actor.setText(Forge.getLocalizer().getMessage("lblAutoSave"));
        quickSave.actor.setText(Forge.getLocalizer().getMessage("lblQuickSave"));
        if (mode == Modes.NewGamePlus) {
            if (difficulty != null) {
                difficulty.setVisible(true);
                difficulty.setSelectedIndex(1);
            }
        } else {
            if (difficulty != null) {
                difficulty.setVisible(false);
            }
        }
        performTouch(scrollPane); //can use mouse wheel if available to scroll
        super.enter();
    }

    public String getSaveFileSuffix() {
        String difficulty;
        switch (AdventurePlayer.current().getDifficulty().name) {
            case "easy":
            case "Easy":
                difficulty = "[%99][CYAN]• [WHITE]";
                break;
            case "normal":
            case "Normal":
                difficulty = "[%99][GREEN]• [WHITE]";
                break;
            case "hard":
            case "Hard":
                difficulty = "[%99][GOLD]• [WHITE]";
                break;
            case "insane":
            case "Insane":
                difficulty = "[%99][RED]• [WHITE]";
                break;
            default:
                difficulty = "[%99][WHITE]";
                break;
        }
        return ASCII_179 + difficulty + GameScene.instance().getAdventurePlayerLocation(true, true);
    }
}
