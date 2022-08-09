package forge.adventure.scene;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.Scaling;
import forge.Forge;
import forge.adventure.data.DifficultyData;
import forge.adventure.util.Config;
import forge.adventure.util.Controls;
import forge.adventure.util.Current;
import forge.adventure.world.WorldSave;
import forge.adventure.world.WorldSaveHeader;
import forge.screens.TransitionScreen;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.DateFormat;
import java.util.function.Function;
import java.util.zip.InflaterInputStream;

/**
 * Scene to load and save the game.
 */
public class SaveLoadScene extends UIScene {
    private final IntMap<TextButton> buttons = new IntMap<>();
    IntMap<WorldSaveHeader> previews = new IntMap<>();
    Color defColor;
    Table layout;
    Modes mode;
    Dialog dialog;
    TextField textInput;
    Label header;
    int currentSlot = -3, lastSelectedSlot = 0;
    Image previewImage;
    Label previewDate;
    Image previewBorder;
    TextButton saveLoadButton, back;
    TextButton quickSave;
    TextButton autoSave;
    SelectBox difficulty;

    public SaveLoadScene() {
        super(Forge.isLandscapeMode() ? "ui/save_load.json" : "ui/save_load_portrait.json");
    }


    private TextButton addSaveSlot(String name, int i) {
        layout.add(Controls.newLabel(name)).align(Align.left).pad(2, 5, 2, 10);
        TextButton button = Controls.newTextButton("...");
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                try {
                    if (!button.isDisabled())
                        select(i);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        layout.add(button).align(Align.left).expandX();
        buttons.put(i, button);
        layout.row();
        return button;

    }

    public void back() {
        Forge.switchToLast();
    }

    public boolean select(int slot) {
        currentSlot = slot;
        if (slot > 0)
            lastSelectedSlot = slot;
        if (previews.containsKey(slot)) {
            WorldSaveHeader header = previews.get(slot);
            if (header.preview != null) {
                previewImage.setDrawable(new TextureRegionDrawable(new Texture(header.preview)));
                previewImage.setScaling(Scaling.fit);
                previewImage.layout();
                previewImage.setVisible(true);
                previewDate.setVisible(true);
                if (header.saveDate != null)
                    previewDate.setText(DateFormat.getDateInstance().format(header.saveDate)+"\n"+DateFormat.getTimeInstance(DateFormat.SHORT).format(header.saveDate));
                else
                    previewDate.setText("");
            }
        } else {
            if (previewImage != null)
                previewImage.setVisible(false);
            if (previewDate != null)
                previewDate.setVisible(false);
        }
        for (IntMap.Entry<TextButton> butt : new IntMap.Entries<TextButton>(buttons)) {
            butt.value.setColor(defColor);
        }
        if (buttons.containsKey(slot)) {
            TextButton button = buttons.get(slot);
            button.setColor(Color.RED);
        }

        return true;
    }

    public void loadSave() {
        switch (mode) {
            case Save:
                if (currentSlot > 0) {
                    //prevent NPE, allowed saveslot is 1 to 10..
                    textInput.setText(buttons.get(currentSlot).getText().toString());
                    dialog.show(stage);
                    stage.setKeyboardFocus(textInput);
                }
                break;
            case Load:
                if (WorldSave.load(currentSlot)) {
                    Forge.setTransitionScreen(new TransitionScreen(() -> Forge.switchScene(SceneType.GameScene.instance), null, false, true));
                } else {
                    Forge.clearTransitionScreen();
                }
                break;
            case NewGamePlus:
                try {
                    Forge.setTransitionScreen(new TransitionScreen(() -> {
                        if (WorldSave.load(currentSlot)) {
                            WorldSave.getCurrentSave().clearChanges();
                            WorldSave.getCurrentSave().getWorld().generateNew(0);
                            if (difficulty != null)
                                Current.player().updateDifficulty(Config.instance().getConfigData().difficulties[difficulty.getSelectedIndex()]);
                            Current.player().setWorldPosY((int) (WorldSave.getCurrentSave().getWorld().getData().playerStartPosY * WorldSave.getCurrentSave().getWorld().getData().height * WorldSave.getCurrentSave().getWorld().getTileSize()));
                            Current.player().setWorldPosX((int) (WorldSave.getCurrentSave().getWorld().getData().playerStartPosX * WorldSave.getCurrentSave().getWorld().getData().width * WorldSave.getCurrentSave().getWorld().getTileSize()));
                            Forge.switchScene(SceneType.GameScene.instance);
                        } else {
                            Forge.clearTransitionScreen();
                        }
                    }, null, false, true));
                } catch (Exception e) {
                    Forge.clearTransitionScreen();
                }
                break;

        }
    }

    public boolean saveAbort() {

        dialog.hide();
        return true;
    }

    @Override
    public boolean keyPressed(int keycode) {
        if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK) {
            back();
        }
        return true;
    }

    public void save() {
        dialog.hide();
        if (WorldSave.getCurrentSave().save(textInput.getText(), currentSlot)) {
            updateFiles();
            //ensure the dialog is hidden before switching
            dialog.getColor().a = 0f;

            Scene restoreScene = Forge.switchToLast();
            if (restoreScene != null) {
                restoreScene = Forge.switchToLast();
            }

            if (restoreScene == null) {
                restoreScene = SceneType.GameScene.instance;
            }

            Forge.switchScene(restoreScene);
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
                        WorldSaveHeader header = (WorldSaveHeader) oos.readObject();
                        buttons.get(slot).setText(header.name);
                        previews.put(slot, header);
                    }

                } catch (ClassNotFoundException | IOException | GdxRuntimeException e) {


                }
            }
        }

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
                header.setText(Forge.getLocalizer().getMessage("lblNewGame")+"+");
                saveLoadButton.setText(Forge.getLocalizer().getMessage("lblStart"));
                break;
        }
        autoSave.setDisabled(mode == Modes.Save);
        quickSave.setDisabled(mode == Modes.Save);
        this.mode = mode;
    }

    @Override
    public void enter() {
        if (lastSelectedSlot > 0)
            select(lastSelectedSlot);
        else
            select(-3);
        updateFiles();
        autoSave.getLabel().setText(Forge.getLocalizer().getMessage("lblAutoSave"));
        quickSave.getLabel().setText(Forge.getLocalizer().getMessage("lblQuickSave"));
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
        super.enter();
    }

    @Override
    public void resLoaded() {
        super.resLoaded();
        layout = new Table();
        stage.addActor(layout);
        dialog = Controls.newDialog(Forge.getLocalizer().getMessage("lblSave"));
        textInput = Controls.newTextField("");
        int c = 0;
        String[] diffList = new String[Config.instance().getConfigData().difficulties.length];
        for (DifficultyData diff : Config.instance().getConfigData().difficulties) {
            diffList[c] = diff.name;
            c++;
        }
        ;

        difficulty = Controls.newComboBox(diffList, null, o -> {
            //DifficultyData difficulty1 = Config.instance().getConfigData().difficulties[difficulty.getSelectedIndex()];
            return null;
        });
        dialog.getButtonTable().add(Controls.newLabel(Forge.getLocalizer().getMessage("lblNameYourSaveFile"))).colspan(2).pad(2, 15, 2, 15);
        dialog.getButtonTable().row();
        dialog.getButtonTable().add(Controls.newLabel(Forge.getLocalizer().getMessage("lblName") + ": ")).align(Align.left).pad(2, 15, 2, 2);
        dialog.getButtonTable().add(textInput).fillX().expandX().padRight(15);
        dialog.getButtonTable().row();
        dialog.getButtonTable().add(Controls.newTextButton(Forge.getLocalizer().getMessage("lblSave"), () -> SaveLoadScene.this.save())).align(Align.left).padLeft(15);
        dialog.getButtonTable().add(Controls.newTextButton(Forge.getLocalizer().getMessage("lblAbort"), () -> SaveLoadScene.this.saveAbort())).align(Align.right).padRight(15);

        //makes dialog hidden immediately when you open saveload scene..
        dialog.getColor().a = 0;
        previewImage = ui.findActor("preview");
        previewDate = ui.findActor("saveDate");
        header = Controls.newLabel(Forge.getLocalizer().getMessage("lblSave"));
        header.setAlignment(Align.center);
        layout.add(header).pad(2).colspan(4).align(Align.center).expandX();
        layout.row();
        autoSave = addSaveSlot(Forge.getLocalizer().getMessage("lblAutoSave"), WorldSave.AUTO_SAVE_SLOT);
        quickSave = addSaveSlot(Forge.getLocalizer().getMessage("lblQuickSave"), WorldSave.QUICK_SAVE_SLOT);
        for (int i = 1; i < 11; i++)
            addSaveSlot(Forge.getLocalizer().getMessage("lblSlot") + ": " + i, i);

        saveLoadButton = ui.findActor("save");
        saveLoadButton.getLabel().setText(Forge.getLocalizer().getMessage("lblSave"));
        ui.onButtonPress("save", () -> SaveLoadScene.this.loadSave());
        back = ui.findActor("return");
        back.getLabel().setText(Forge.getLocalizer().getMessage("lblBack"));
        ui.onButtonPress("return", () -> SaveLoadScene.this.back());

        defColor = saveLoadButton.getColor();

        ScrollPane scrollPane = ui.findActor("saveSlots");
        scrollPane.setActor(layout);
        ui.addActor(difficulty);
        difficulty.setSelectedIndex(1);
        difficulty.setAlignment(Align.center);
        difficulty.getStyle().fontColor = Color.GOLD;
        if (Forge.isLandscapeMode()) {
            difficulty.setX(280);
            difficulty.setY(220);
        } else {
            difficulty.setX(190);
            difficulty.setY(336);
        }
    }
}
