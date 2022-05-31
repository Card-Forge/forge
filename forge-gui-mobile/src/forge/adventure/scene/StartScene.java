package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import forge.Forge;
import forge.adventure.stage.GameHUD;
import forge.adventure.stage.MapStage;
import forge.adventure.util.Config;
import forge.adventure.world.WorldSave;
import forge.screens.TransitionScreen;

/**
 * First scene after the splash screen
 */
public class StartScene extends UIScene {

    TextButton saveButton, resumeButton, continueButton, newGameButton, newGameButtonPlus, loadButton, settingsButton, exitButton, switchButton;

    public StartScene() {
        super(Forge.isLandscapeMode()?"ui/start_menu.json":"ui/start_menu_portrait.json");

    }

    public boolean NewGame() {
        Forge.switchScene(SceneType.NewGameScene.instance);
        return true;
    }

    public boolean Save() {
        ((SaveLoadScene) SceneType.SaveLoadScene.instance).setMode(SaveLoadScene.Modes.Save);
        Forge.switchScene(SceneType.SaveLoadScene.instance);
        return true;
    }

    public boolean Load() {
        ((SaveLoadScene) SceneType.SaveLoadScene.instance).setMode(SaveLoadScene.Modes.Load);
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

    public boolean Continue() {
        final String lastActiveSave = Config.instance().getSettingData().lastActiveSave;

        if (WorldSave.isSafeFile(lastActiveSave) && WorldSave.load(WorldSave.filenameToSlot(lastActiveSave))) {
            Forge.setTransitionScreen(new TransitionScreen(new Runnable() {
                @Override
                public void run() {
                    Forge.switchScene(SceneType.GameScene.instance);
                }
            }, null, false, true));
        } else {
            Forge.clearTransitionScreen();
        }

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

        boolean hasResumeButton = WorldSave.getCurrentSave().getWorld().getData() != null;
        resumeButton.setVisible(hasResumeButton);

        // Continue button mutually exclusive with resume button
        continueButton.setVisible(Config.instance().getSettingData().lastActiveSave != null && !hasResumeButton);

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
            ui.onButtonPress("Start", () -> StartScene.this.NewGame());
            ui.onButtonPress("Start+", () -> NewGamePlus());
            ui.onButtonPress("Load", () -> StartScene.this.Load());
            ui.onButtonPress("Save", () -> StartScene.this.Save());
            ui.onButtonPress("Resume", () -> StartScene.this.Resume());
            ui.onButtonPress("Continue", () -> StartScene.this.Continue());
            ui.onButtonPress("Settings", () -> StartScene.this.settings());
            ui.onButtonPress("Exit", () -> StartScene.this.Exit());
            ui.onButtonPress("Switch", () -> Forge.switchToClassic());

            newGameButton = ui.findActor("Start");
            newGameButton.getLabel().setText(Forge.getLocalizer().getMessage("lblNewGame"));
            newGameButtonPlus = ui.findActor("Start+");
            newGameButtonPlus.getLabel().setText(Forge.getLocalizer().getMessage("lblNewGame")+"+");
            loadButton = ui.findActor("Load");
            loadButton.getLabel().setText(Forge.getLocalizer().getMessage("lblLoad"));
            saveButton = ui.findActor("Save");
            saveButton.getLabel().setText(Forge.getLocalizer().getMessage("lblSave"));
            resumeButton = ui.findActor("Resume");
            resumeButton.getLabel().setText(Forge.getLocalizer().getMessage("lblResume"));
            continueButton = ui.findActor("Continue");
            continueButton.getLabel().setText(Forge.getLocalizer().getMessage("lblContinue"));
            settingsButton = ui.findActor("Settings");
            settingsButton.getLabel().setText(Forge.getLocalizer().getMessage("lblSettings"));
            exitButton = ui.findActor("Exit");
            exitButton.getLabel().setText(Forge.getLocalizer().getMessage("lblExit"));
            switchButton = ui.findActor("Switch");
            switchButton.getLabel().setText(Forge.getLocalizer().getMessage("lblClassic"));

            saveButton.setVisible(false);
            resumeButton.setVisible(false);
    }

    private void NewGamePlus() {
        ((SaveLoadScene) SceneType.SaveLoadScene.instance).setMode(SaveLoadScene.Modes.NewGamePlus);
        Forge.switchScene(SceneType.SaveLoadScene.instance);
    }
}
