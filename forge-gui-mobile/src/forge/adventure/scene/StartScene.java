package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import forge.Forge;
import forge.adventure.stage.GameHUD;
import forge.adventure.stage.MapStage;
import forge.adventure.world.WorldSave;

/**
 * First scene after the splash screen
 */
public class StartScene extends UIScene {

    TextButton saveButton, resumeButton, newGameButton, loadButtton, settingsButton, exitButton, switchButton;

    public StartScene() {
        super("ui/start_menu_mobile.json");

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
        if (MapStage.getInstance().isInMap())
            Forge.switchScene(SceneType.TileMapScene.instance);
        else
            Forge.switchScene(SceneType.GameScene.instance);
        GameHUD.getInstance().getTouchpad().setVisible(false);
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

        boolean hasSaveButton = WorldSave.getCurrentSave().getWorld().getData() != null;
        if (hasSaveButton)
            hasSaveButton = !((TileMapScene) SceneType.TileMapScene.instance).currentMap().isInMap();
        saveButton.setVisible(hasSaveButton);
        resumeButton.setVisible(WorldSave.getCurrentSave().getWorld().getData() != null);
        Gdx.input.setInputProcessor(stage); //Start taking input from the ui
    }

    @Override
    public void create() {


    }

    @Override
    public boolean keyPressed(int keycode) {
        if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK) {
            if (WorldSave.getCurrentSave().getWorld().getData() != null)
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
        newGameButton = ui.findActor("Start");
        newGameButton.getLabel().setText(Forge.getLocalizer().getMessage("lblNewGame"));
        loadButtton = ui.findActor("Load");
        loadButtton.getLabel().setText(Forge.getLocalizer().getMessage("lblLoad"));
        saveButton = ui.findActor("Save");
        saveButton.getLabel().setText(Forge.getLocalizer().getMessage("lblSave"));
        resumeButton = ui.findActor("Resume");
        resumeButton.getLabel().setText(Forge.getLocalizer().getMessage("lblResume"));
        settingsButton = ui.findActor("Settings");
        settingsButton.getLabel().setText(Forge.getLocalizer().getMessage("lblSettings"));
        exitButton = ui.findActor("Exit");
        exitButton.getLabel().setText(Forge.getLocalizer().getMessage("lblExit"));
        switchButton = ui.findActor("Switch");
        switchButton.getLabel().setText(Forge.getLocalizer().getMessage("lblClassic"));
        if (!Forge.isLandscapeMode()) {
            float w = Scene.GetIntendedWidth();
            float bW = w - 165;
            float oX = w/2 - bW/2;
            newGameButton.setWidth(bW);
            newGameButton.setX(oX);
            newGameButton.getLabel().setFontScaleX(2);
            loadButtton.setWidth(bW);
            loadButtton.setX(oX);
            loadButtton.getLabel().setFontScaleX(2);
            saveButton.setWidth(bW);
            saveButton.setX(oX);
            saveButton.getLabel().setFontScaleX(2);
            resumeButton.setWidth(bW);
            resumeButton.setX(oX);
            resumeButton.getLabel().setFontScaleX(2);
            settingsButton.setWidth(bW);
            settingsButton.setX(oX);
            settingsButton.getLabel().setFontScaleX(2);
            exitButton.setWidth(bW/2);
            exitButton.setX(w/2-exitButton.getWidth());
            exitButton.getLabel().setFontScaleX(2);
            switchButton.setWidth(bW/2);
            switchButton.setX(w/2);
            switchButton.getLabel().setFontScaleX(2);
        }
        ui.onButtonPress("Switch", new Runnable() {
            @Override
            public void run() {
                Forge.switchToClassic();
            }
        });
        saveButton.setVisible(false);
        resumeButton.setVisible(false);
    }
}
