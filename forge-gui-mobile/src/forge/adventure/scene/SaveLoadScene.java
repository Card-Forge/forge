package forge.adventure.scene;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.IntMap;
import forge.Forge;
import forge.adventure.util.Controls;
import forge.adventure.world.WorldSave;
import forge.adventure.world.WorldSaveHeader;
import forge.screens.TransitionScreen;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.zip.InflaterInputStream;

/**
 * Scene to load and save the game.
 *
 */
public class SaveLoadScene extends UIScene {
    private final IntMap<TextButton> buttons = new IntMap<>();
    IntMap<WorldSaveHeader> previews = new IntMap<>();
    Color defColor;
    Table layout;
    boolean save = true;
    Dialog dialog;
    TextField textInput;
    Label header;
    int currentSlot = -3;
    Image previewImage;
    TextButton saveLoadButton;
    TextButton quickSave;
    TextButton autoSave;

    public SaveLoadScene() {
        super("ui/save_load.json");
    }




    private TextButton addSaveSlot(String name, int i) {
        layout.add(Controls.newLabel(name)).colspan(1).align(Align.right).expandX();
        layout.add(Controls.newLabel("  "));
        TextButton button = Controls.newTextButton("...");
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                try {
                    if(!button.isDisabled())
                        select(i);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        layout.add(button).colspan(2).align(Align.left).expandX();
        buttons.put(i, button);
        layout.row();
        return button;

    }

    public void back() {
        Forge.switchToLast();
    }

    public boolean select(int slot) {
        currentSlot = slot;

        if (previews.containsKey(slot)) {
            WorldSaveHeader header = previews.get(slot);
            if (header.preview != null) {
                previewImage.setDrawable(new TextureRegionDrawable(new Texture(header.preview)));
                previewImage.layout();
            }
        }
        for (IntMap.Entry<TextButton> butt : new IntMap.Entries<TextButton> (buttons)) {
            butt.value.setColor(defColor);
        }
        if (buttons.containsKey(slot)) {
            TextButton button = buttons.get(slot);
            button.setColor(Color.RED);
        }

        return true;
    }

    public void loadSave() {
        if (save) {
            textInput.setText(buttons.get(currentSlot).getText().toString());
            dialog.show(stage);
            stage.setKeyboardFocus(textInput);
        } else {
            if (WorldSave.load(currentSlot)) {
                Forge.setTransitionScreen(new TransitionScreen(null, null, false, true));
                Forge.switchScene(SceneType.GameScene.instance);
            } else {
                Forge.clearTransitionScreen();
            }
        }
    }

    public boolean saveAbort() {

        dialog.hide();
        return true;
    }

    @Override
    public boolean keyPressed(int keycode)
    {
        if (keycode == Input.Keys.ESCAPE)
        {
                back();
        }
        return true;
    }
    public void save() {
        dialog.hide();
        if( WorldSave.getCurrentSave().save(textInput.getText(), currentSlot))
        {
            updateFiles();
            Forge.switchScene(SceneType.GameScene.instance);
        }


    }

    private void updateFiles() {

        File f = new File(WorldSave.getSaveDir());
        f.mkdirs();
        File[] names = f.listFiles();
        if(names==null)
            throw new RuntimeException("Can not find save directory");
        previews.clear();
        for (File name : names) {
            if (WorldSave.isSafeFile(name.getName())) {
                try {

                    try (FileInputStream fos = new FileInputStream(name.getAbsolutePath());
                         InflaterInputStream inf = new InflaterInputStream(fos);
                         ObjectInputStream oos = new ObjectInputStream(inf)) {


                        int slot=WorldSave.filenameToSlot(name.getName());
                        WorldSaveHeader header = (WorldSaveHeader) oos.readObject();
                        buttons.get(slot).setText(header.name);
                        previews.put(slot, header);
                    }

                } catch (ClassNotFoundException | IOException | GdxRuntimeException e) {


                }
            }
        }

    }


    public void setSaveGame(boolean save) {
        if (save) {
            header.setText("Save game");
            saveLoadButton.setText("Save");
        } else {
            header.setText("Load game");
            saveLoadButton.setText("Load");
        }
        autoSave.setDisabled(save);
        quickSave.setDisabled(save);
        this.save = save;
    }

    @Override
    public void enter() {
        select(-3);
        updateFiles();
        super.enter();
    }

    @Override
    public void resLoaded() {
        super.resLoaded();
        layout = new Table();
        layout.setFillParent(true);
        stage.addActor(layout);
        dialog = Controls.newDialog("Save");
        textInput = Controls.newTextField("");
        dialog.getButtonTable().add(Controls.newLabel("Name your new save file.")).colspan(2);
        dialog.getButtonTable().row();
        dialog.getButtonTable().add(Controls.newLabel("Name:")).align(Align.left);
        dialog.getButtonTable().add(textInput).fillX().expandX();
        dialog.getButtonTable().row();
        dialog.getButtonTable().add(Controls.newTextButton("Save", new Runnable() {
            @Override
            public void run() {
                SaveLoadScene.this.save();
            }
        })).align(Align.left);
        dialog.getButtonTable().add(Controls.newTextButton("Abort", new Runnable() {
            @Override
            public void run() {
                SaveLoadScene.this.saveAbort();
            }
        })).align(Align.left);

        previewImage = ui.findActor("preview");
        header = Controls.newLabel("Save");
        header.setHeight(header.getHeight() * 2);
        layout.add(header).colspan(3).align(Align.center).expand();
        layout.row();
        autoSave=addSaveSlot("Auto save", WorldSave.AUTO_SAVE_SLOT);
        quickSave=addSaveSlot("Quick save", WorldSave.QUICK_SAVE_SLOT);
        for (int i = 1; i < 11; i++)
            addSaveSlot("Slot:" + i, i);

        saveLoadButton = ui.findActor("save");
        ui.onButtonPress("save", new Runnable() {
            @Override
            public void run() {
                SaveLoadScene.this.loadSave();
            }
        });
        ui.onButtonPress("return", new Runnable() {
            @Override
            public void run() {
                SaveLoadScene.this.back();
            }
        });
        defColor = saveLoadButton.getColor();


        ScrollPane scrollPane = ui.findActor("saveSlots");
        scrollPane.setActor(layout);
    }
}
