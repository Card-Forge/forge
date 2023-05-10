package forge.adventure.scene;

import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.github.tommyettinger.textra.TextraButton;
import forge.Forge;
import forge.adventure.stage.GameHUD;
import forge.adventure.stage.GameStage;
import forge.adventure.stage.MapStage;
import forge.adventure.util.Config;
import forge.adventure.world.WorldSave;
import forge.screens.TransitionScreen;
import forge.sound.SoundSystem;

/**
 * First scene after the splash screen
 */
public class StartScene extends UIScene {

    private static StartScene object;
    Dialog exitDialog;
    TextraButton saveButton, resumeButton, continueButton;


    public StartScene() {
        super(Forge.isLandscapeMode() ? "ui/start_menu.json" : "ui/start_menu_portrait.json");
        ui.onButtonPress("Start", StartScene.this::NewGame);
        ui.onButtonPress("Start+", this::NewGamePlus);
        ui.onButtonPress("Load", StartScene.this::Load);
        ui.onButtonPress("Save", StartScene.this::Save);
        ui.onButtonPress("Resume", StartScene.this::Resume);
        ui.onButtonPress("Continue", StartScene.this::Continue);
        ui.onButtonPress("Settings", StartScene.this::settings);
        ui.onButtonPress("Exit", StartScene.this::Exit);
        ui.onButtonPress("Switch", StartScene.this::switchToClassic);


        saveButton = ui.findActor("Save");
        resumeButton = ui.findActor("Resume");
        continueButton = ui.findActor("Continue");

        saveButton.setVisible(false);
        resumeButton.setVisible(false);
    }

    public static StartScene instance() {
        if (object == null)
            object = new StartScene();
        return object;
    }

    public boolean NewGame() {
        Forge.switchScene(NewGameScene.instance());
        return true;
    }

    public boolean Save() {
        SaveLoadScene.instance().setMode(SaveLoadScene.Modes.Save);
        Forge.switchScene(SaveLoadScene.instance());
        return true;
    }

    public boolean Load() {
        SaveLoadScene.instance().setMode(SaveLoadScene.Modes.Load);
        Forge.switchScene(SaveLoadScene.instance());
        return true;
    }

    public boolean Resume() {
        if (MapStage.getInstance().isInMap())
            Forge.switchScene(TileMapScene.instance());
        else
            Forge.switchScene(GameScene.instance());
        GameHUD.getInstance().getTouchpad().setVisible(false);
        return true;
    }

    public boolean Continue() {
        final String lastActiveSave = Config.instance().getSettingData().lastActiveSave;

        if (WorldSave.isSafeFile(lastActiveSave)) {
            try {
                Forge.setTransitionScreen(new TransitionScreen(() -> {
                    if (WorldSave.load(WorldSave.filenameToSlot(lastActiveSave))) {
                        SoundSystem.instance.changeBackgroundTrack();
                        Forge.switchScene(GameScene.instance());
                    } else {
                        Forge.clearTransitionScreen();
                    }
                }, null, false, true, "Loading World..."));
            } catch (Exception e) {
                Forge.clearTransitionScreen();
            }
        }

        return true;
    }

    public boolean settings() {
        Forge.switchScene(SettingsScene.instance());
        return true;
    }

    public boolean Exit() {
        if (exitDialog == null) {
            exitDialog = createGenericDialog(Forge.getLocalizer().getMessage("lblExitForge"),
                    Forge.getLocalizer().getMessage("lblAreYouSureYouWishExitForge"), Forge.getLocalizer().getMessage("lblOk"),
                    Forge.getLocalizer().getMessage("lblAbort"), () -> {
                        Forge.exit(true);
                        removeDialog();
                    }, this::removeDialog);
        }
        showDialog(exitDialog);
        return true;
    }

    public void switchToClassic() {
        GameHUD.getInstance().stopAudio();
        Forge.switchToClassic();
    }

    @Override
    public void enter() {
        boolean hasSaveButton = WorldSave.getCurrentSave().getWorld().getData() != null;
        if (hasSaveButton) {
            TileMapScene scene = TileMapScene.instance();
            hasSaveButton = !scene.currentMap().isInMap() || scene.isAutoHealLocation();
        }
        saveButton.setVisible(hasSaveButton);

        boolean hasResumeButton = WorldSave.getCurrentSave().getWorld().getData() != null;
        resumeButton.setVisible(hasResumeButton);

        // Continue button mutually exclusive with resume button
        if (Config.instance().getSettingData().lastActiveSave != null && !hasResumeButton) {
            continueButton.setVisible(true);
            if (!Forge.isLandscapeMode()) {
                continueButton.setX(resumeButton.getX());
                continueButton.setY(resumeButton.getY());
            }
        } else {
            continueButton.setVisible(false);
        }


        if (Forge.createNewAdventureMap) {
            this.NewGame();
            GameStage.maximumScrollDistance = 4f;
        }

        super.enter();
    }

    private void NewGamePlus() {
        SaveLoadScene.instance().setMode(SaveLoadScene.Modes.NewGamePlus);
        Forge.switchScene(SaveLoadScene.instance());
    }
}
