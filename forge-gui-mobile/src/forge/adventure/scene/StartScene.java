package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Actor;
import forge.Forge;
import forge.adventure.stage.GameHUD;
import forge.adventure.world.WorldSave;
import forge.gui.GuiBase;

/**
 * First scene after the splash screen
 */
public class StartScene extends UIScene {

    Actor saveButton;
    Actor resumeButton;

    public StartScene()
    {
        super(GuiBase.isAndroid() ? "ui/start_menu_mobile.json" : "ui/start_menu.json");

    }
    public boolean NewGame() {
        Forge.switchScene(SceneType.NewGameScene.instance);
        return true;
    }

    public boolean Save() {
        ((SaveLoadScene) SceneType.SaveLoadScene.instance).setSaveGame(true);
        Forge.switchScene(SceneType.SaveLoadScene.instance);
        return true;
    }

    public boolean Load() {
        ((SaveLoadScene) SceneType.SaveLoadScene.instance).setSaveGame(false);
        Forge.switchScene(SceneType.SaveLoadScene.instance);
        return true;
    }

    public boolean Resume() {
        //Forge.switchToLast();
        GameHUD.getInstance().getTouchpad().setVisible(false);
        Forge.switchScene(SceneType.GameScene.instance);
        return true;
    }

    public boolean settings() {
        Forge.switchScene(SceneType.SettingsScene.instance);
        return true;
    }

    public boolean Exit() {
        Forge.exit(true);
        return true;
    }

    @Override
    public void enter() {

        boolean hasSaveButton=WorldSave.getCurrentSave().getWorld().getData() != null;
        if(hasSaveButton)
            hasSaveButton=!((TileMapScene)SceneType.TileMapScene.instance).currentMap().isInMap();
        saveButton.setVisible(hasSaveButton);
        resumeButton.setVisible(WorldSave.getCurrentSave().getWorld().getData() != null);
        Gdx.input.setInputProcessor(stage); //Start taking input from the ui
    }

    @Override
    public void create() {


    }

    @Override
    public boolean keyPressed(int keycode)
    {
        if (keycode == Input.Keys.ESCAPE)
        {
            if(WorldSave.getCurrentSave().getWorld().getData() != null)
                Resume();
        }
        return true;
    }
    @Override
    public void resLoaded() {
        super.resLoaded();

        ui.onButtonPress("Start", new Runnable() {
            @Override
            public void run() {
                StartScene.this.NewGame();
            }
        });
        ui.onButtonPress("Load", new Runnable() {
            @Override
            public void run() {
                StartScene.this.Load();
            }
        });
        ui.onButtonPress("Start", new Runnable() {
            @Override
            public void run() {
                StartScene.this.NewGame();
            }
        });
        ui.onButtonPress("Save", new Runnable() {
            @Override
            public void run() {
                StartScene.this.Save();
            }
        });
        ui.onButtonPress("Resume", new Runnable() {
            @Override
            public void run() {
                StartScene.this.Resume();
            }
        });

        saveButton = ui.findActor("Save");
        resumeButton = ui.findActor("Resume");
        ui.onButtonPress("Settings", new Runnable() {
            @Override
            public void run() {
                StartScene.this.settings();
            }
        });
        ui.onButtonPress("Exit", new Runnable() {
            @Override
            public void run() {
                StartScene.this.Exit();
            }
        });
        saveButton.setVisible(false);
        resumeButton.setVisible(false);
    }
}
