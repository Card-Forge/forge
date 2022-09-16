package forge.adventure.scene;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
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
    TextButton saveLoadButton, back, quickSave, autoSave, dialogSaveBtn, dialogAbortBtn;
    Actor lastHighlightedSave;
    SelectBox difficulty;
    ScrollPane scrollPane;

    private SaveLoadScene() {
        super(Forge.isLandscapeMode() ? "ui/save_load.json" : "ui/save_load_portrait.json");

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


    private static SaveLoadScene object;

    public static SaveLoadScene instance() {
        if(object==null)
            object=new SaveLoadScene();
        return object;
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
                    previewDate.setText(DateFormat.getDateInstance().format(header.saveDate) + "\n" + DateFormat.getTimeInstance(DateFormat.SHORT).format(header.saveDate));
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
            selectActor(button, false);
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
                    selectActor(textInput, false);
                    stage.setKeyboardFocus(textInput);
                }
                break;
            case Load:
                try {
                    Forge.setTransitionScreen(new TransitionScreen(() -> {
                        if (WorldSave.load(currentSlot)) {
                            Forge.switchScene(GameScene.instance());
                        } else {
                            Forge.clearTransitionScreen();
                        }
                    }, null, false, true, "Loading World..."));
                } catch (Exception e) {
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
                            Forge.switchScene(GameScene.instance());
                        } else {
                            Forge.clearTransitionScreen();
                        }
                    }, null, false, true, "Generating World..."));
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
        if (kbVisible) {
            if (keycode == Input.Keys.BUTTON_START)
                keyOK();
            else if (keycode == Input.Keys.BUTTON_L1)
                toggleShiftOrBackspace(true);
            else if (keycode == Input.Keys.BUTTON_R1)
                toggleShiftOrBackspace(false);
            else if (keycode == Input.Keys.BUTTON_B)
                hideOnScreenKeyboard();
            else if (keycode == Input.Keys.BUTTON_A) {
                if (selectedKey != null)
                    performTouch(selectedKey);
            } else if (keycode == Input.Keys.DPAD_UP || keycode == Input.Keys.DPAD_DOWN || keycode == Input.Keys.DPAD_LEFT || keycode == Input.Keys.DPAD_RIGHT)
                setSelectedKey(keycode);
        } else if (dialog.getColor().a != 0f) {
            if (keycode == Input.Keys.BUTTON_A) {
                if (selectedActor == textInput) {
                    lastInputField = textInput;
                    showOnScreenKeyboard(textInput.getText());
                } else if (selectedActor == dialogAbortBtn || selectedActor == dialogSaveBtn) {
                    performTouch(selectedActor);
                    if (lastSelectedSlot > 0)
                        select(lastSelectedSlot);
                    else
                        select(-3);
                }
            } else if (keycode == Input.Keys.BUTTON_B) {
                performTouch(dialogAbortBtn);
                if (lastSelectedSlot > 0)
                    select(lastSelectedSlot);
                else
                    select(-3);
            }
            else if (keycode == Input.Keys.DPAD_DOWN) {
                if (selectedActor == null) {
                    selectActor(textInput, false);
                } else if (selectedActor == textInput)
                    selectActor(dialogSaveBtn, false);
            } else if (keycode == Input.Keys.DPAD_UP) {
                if (selectedActor == null)
                    selectActor(dialogSaveBtn, false);
                else if (selectedActor == dialogSaveBtn || selectedActor == dialogAbortBtn) {
                    selectActor(textInput, false);
                }
            } else if (keycode == Input.Keys.DPAD_LEFT) {
                if (selectedActor == dialogAbortBtn)
                    selectActor(dialogSaveBtn, false);
            } else if (keycode == Input.Keys.DPAD_RIGHT) {
                if (selectedActor == dialogSaveBtn)
                    selectActor(dialogAbortBtn, false);
            }
        } else {
            if (keycode == Input.Keys.BUTTON_B)
                performTouch(back);
            else if (keycode == Input.Keys.BUTTON_Y) {
                if (difficulty != null && difficulty.isVisible()) {
                    int index = difficulty.getSelectedIndex()-1;
                    if (index < 0)
                        index = 0;
                    difficulty.setSelectedIndex(index);
                }
            } else if (keycode == Input.Keys.BUTTON_X) {
                if (difficulty != null && difficulty.isVisible()) {
                    int index = difficulty.getSelectedIndex()+1;
                    if (index >= 2)
                        index = 2;
                    difficulty.setSelectedIndex(index);
                }
            } else if (keycode == Input.Keys.BUTTON_L1) {
                scrollPane.fling(1f, 0, -300);
            } else if (keycode == Input.Keys.BUTTON_R1) {
                scrollPane.fling(1f, 0, +300);
            } else if (keycode == Input.Keys.BUTTON_A) {
                performTouch(selectedActor);
            } else if (keycode == Input.Keys.DPAD_LEFT) {
                if (selectedActor == back || selectedActor == saveLoadButton) {
                    if (lastHighlightedSave != null)
                        selectActor(lastHighlightedSave, false);
                    else
                        selectActor(actorObjectMap.get(0), false);
                    lastHighlightedSave = selectedActor;
                }
            } else if (keycode == Input.Keys.DPAD_RIGHT) {
                if (!(selectedActor == back || selectedActor == saveLoadButton)) {
                    lastHighlightedSave = selectedActor;
                    selectActor(saveLoadButton, false);
                }
            } else if (keycode == Input.Keys.DPAD_DOWN) {
                int index = mode == Modes.Save ? 9 : 11;
                if (selectedActor == back)
                    selectActor(saveLoadButton, false);
                else if (selectedActorIndex == index) {
                    selectActor(actorObjectMap.get(0), false);
                    scrollPane.fling(1f, 0, +300);
                } else {
                    selectNextActor(false);
                }
                if (selectedActorIndex == 6)
                    scrollPane.fling(1f, 0, -300);
                if (!(selectedActor == back || selectedActor == saveLoadButton))
                    lastHighlightedSave = selectedActor;
            } else if (keycode == Input.Keys.DPAD_UP) {
                if (selectedActor == saveLoadButton)
                    selectActor(back, false);
                else if (selectedActorIndex == 0) {
                    selectActor(buttons.get(10), false);
                    scrollPane.fling(1f, 0, -300);
                } else {
                    selectPreviousActor(false);
                }
                if (selectedActorIndex == 5)
                    scrollPane.fling(1f, 0, +300);
                if (!(selectedActor == back || selectedActor == saveLoadButton))
                    lastHighlightedSave = selectedActor;
            } else if (keycode == Input.Keys.BUTTON_START) {
                performTouch(saveLoadButton);
            }
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
                restoreScene = GameScene.instance();
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
                header.setText(Forge.getLocalizer().getMessage("lblNewGame") + "+");
                saveLoadButton.setText(Forge.getLocalizer().getMessage("lblStart"));
                break;
        }
        autoSave.setDisabled(mode == Modes.Save);
        quickSave.setDisabled(mode == Modes.Save);
        this.mode = mode;
    }

    @Override
    public void enter() {
        unselectActors();
        clearActorObjects();
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
        if (!autoSave.isDisabled())
            addActorObject(autoSave);
        if (!quickSave.isDisabled())
            addActorObject(quickSave);
        for (int i=0; i <= 10; i++) {
            if (buttons.containsKey(i))
                addActorObject(buttons.get(i));
        }
        addActorObject(textInput);
        addActorObject(dialogSaveBtn);
        addActorObject(dialogAbortBtn);
        addActorObject(back);
        addActorObject(saveLoadButton);
        if (scrollPane != null) {
            if (lastSelectedSlot >= 6) {
                scrollPane.fling(1f, 0, -300);
                selectActor(buttons.get(lastSelectedSlot), false);
            } else if (lastSelectedSlot > 0 && lastSelectedSlot < 6) {
                scrollPane.fling(1f, 0, +300);
                selectActor(buttons.get(lastSelectedSlot), false);
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
        dialogSaveBtn = Controls.newTextButton(Forge.getLocalizer().getMessage("lblSave"), () -> SaveLoadScene.this.save());
        dialog.getButtonTable().add(dialogSaveBtn).align(Align.left).padLeft(15);
        dialogAbortBtn = Controls.newTextButton(Forge.getLocalizer().getMessage("lblAbort"), () -> SaveLoadScene.this.saveAbort());
        dialog.getButtonTable().add(dialogAbortBtn).align(Align.right).padRight(15);

        //makes dialog hidden immediately when you open saveload scene..
        dialog.getColor().a = 0;
        dialog.hide();
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

        scrollPane = ui.findActor("saveSlots");
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
