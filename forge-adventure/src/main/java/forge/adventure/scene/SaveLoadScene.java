package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import forge.adventure.AdventureApplicationAdapter;
import forge.adventure.util.Controls;
import forge.adventure.world.WorldSave;
import forge.adventure.world.WorldSaveHeader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

public class SaveLoadScene extends Scene {
    private final IntMap<TextButton> buttons = new IntMap<>();
    Texture Background;
    IntMap<WorldSaveHeader> previews = new IntMap<>();
    Stage stage;
    Color defColor;
    Table layout;
    boolean save = true;
    Dialog dialog;
    TextField textInput;
    Label header;
    int currentSlot = -3;
    Image previewImage;
    TextButton saveLoadButton;

    public SaveLoadScene() {

    }

    @Override
    public void dispose() {
        stage.dispose();
        Background.dispose();
    }

    @Override
    public void render() {

        //Batch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(1, 0, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.getBatch().begin();
        stage.getBatch().disableBlending();
        stage.getBatch().draw(Background, 0, 0, GetIntendedWidth(), GetIntendedHeight());
        stage.getBatch().enableBlending();
        stage.getBatch().end();
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
        //Batch.end();
    }

    private void AddSaveSlot(String name, int i) {
        layout.add(Controls.newLabel(name));
        TextButton button = Controls.newTextButton("...");
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                try {
                    Select(i);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        layout.add(button).expandX();
        buttons.put(i, button);
        layout.row();

    }

    public boolean Back() {
        AdventureApplicationAdapter.CurrentAdapter.SwitchToLast();
        return true;
    }

    public boolean Select(int slot) {
        currentSlot = slot;

        if (previews.containsKey(slot)) {
            WorldSaveHeader header = previews.get(slot);
            if (header.preview != null) {
                previewImage.setDrawable(new TextureRegionDrawable(new Texture(header.preview)));
                previewImage.layout();
            }
        }
        for (IntMap.Entry<TextButton> butt : buttons.entries()) {
            butt.value.setColor(defColor);
        }
        if (buttons.containsKey(slot)) {
            TextButton button = buttons.get(slot);
            button.setColor(Color.RED);
        }

        return true;
    }

    public boolean LoadSave() {
        if (save) {
            textInput.setText("savegame " + currentSlot);
            dialog.show(stage);
            stage.setKeyboardFocus(textInput);
        } else {
            WorldSave.Load(currentSlot);
        }
        return true;
    }

    public boolean SaveAbort() {

        dialog.hide();
        return true;
    }

    public boolean Save() {
        dialog.hide();
        WorldSave.getCurrentSave().Save(textInput.getText(), currentSlot);
        UpdateFiles();
        return true;
    }

    private void UpdateFiles() {

        File f = new File(WorldSave.GetSaveDir());
        f.mkdirs();
        File[] names = f.listFiles();
        previews.clear();
        for (File name : names) {
            int slot = WorldSave.FilenameToSlot(name.getName());
            if (slot >= -2) {
                FileInputStream fos = null;
                try {

                    fos = new FileInputStream(name.getAbsolutePath());
                    ObjectInputStream oos = new ObjectInputStream(fos);
                    WorldSaveHeader header = (WorldSaveHeader) oos.readObject();
                    buttons.get(slot).setText(header.name);
                    previews.put(slot, header);
                } catch (ClassNotFoundException | IOException | GdxRuntimeException e) {
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
        }

    }

    public boolean Resume() {
        return true;
    }

    public boolean settings() {
        AdventureApplicationAdapter.CurrentAdapter.SwitchScene(forge.adventure.scene.SceneType.SettingsScene.instance);
        return true;
    }

    public boolean Exit() {
        Gdx.app.exit();
        return true;
    }

    public void SetSaveGame(boolean save) {
        if (save) {
            header.setText("Save game");
            saveLoadButton.setText("Save");
        } else {
            header.setText("Load game");
            saveLoadButton.setText("Load");
        }
        this.save = save;
    }

    @Override
    public void Enter() {
        Select(-3);
        UpdateFiles();
        Gdx.input.setInputProcessor(stage); //Start taking input from the ui
    }

    @Override
    public void ResLoaded() {
        stage = new Stage(new StretchViewport(GetIntendedWidth(), GetIntendedHeight()));
        Background = new Texture(AdventureApplicationAdapter.CurrentAdapter.GetRes().GetFile("ui/title_bg.png"));
        layout = new Table();
        layout.setFillParent(true);
        stage.addActor(layout);
        dialog = Controls.newDialog("Save");
        textInput = Controls.newTextField("");
        dialog.getButtonTable().add(Controls.newLabel("Name your new savegame.")).colspan(2);
        dialog.getButtonTable().row();
        dialog.getButtonTable().add(Controls.newLabel("Name:")).align(Align.left);
        dialog.getButtonTable().add(textInput).fillX().expandX();
        dialog.getButtonTable().row();
        dialog.getButtonTable().add(Controls.newTextButton("Save", () -> Save())).align(Align.left);
        dialog.getButtonTable().add(Controls.newTextButton("Abort", () -> SaveAbort())).align(Align.left);

        previewImage = new Image();
        previewImage.setSize(WorldSaveHeader.previewImageWidth, WorldSaveHeader.previewImageWidth / (GetIntendedWidth() / (float) GetIntendedHeight()));

        previewImage.setPosition(GetIntendedWidth() - previewImage.getWidth(), GetIntendedHeight() - previewImage.getHeight());
        stage.addActor(previewImage);
        header = Controls.newLabel("Save");
        header.setHeight(header.getHeight() * 2);
        layout.add(header).colspan(2).align(Align.center);
        layout.row();
        AddSaveSlot("Auto save", -2);
        AddSaveSlot("Quick save", -1);
        for (int i = 1; i < 11; i++)
            AddSaveSlot("Slot:" + i, i);


        TextButton backButton = Controls.newTextButton("Back");
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                try {
                    Back();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        layout.add(backButton);
        saveLoadButton = Controls.newTextButton("SaveLoad");
        saveLoadButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                try {
                    LoadSave();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        layout.add(saveLoadButton);
        defColor = saveLoadButton.getColor();
    }
}
