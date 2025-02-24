package forge.adventure.scene;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.utils.Timer;
import com.github.tommyettinger.textra.TextraButton;
import com.github.tommyettinger.textra.TypingLabel;
import forge.Forge;
import forge.adventure.stage.GameHUD;
import forge.adventure.stage.GameStage;
import forge.adventure.stage.MapStage;
import forge.adventure.util.Config;
import forge.adventure.util.Controls;
import forge.adventure.world.WorldSave;
import forge.localinstance.properties.ForgeProfileProperties;
import forge.screens.TransitionScreen;
import forge.sound.SoundSystem;
import forge.util.ZipUtil;

import java.io.File;
import java.io.IOException;

/**
 * First scene after the splash screen
 */
public class StartScene extends UIScene {
    private static StartScene object;
    Dialog exitDialog, backupDialog, zipDialog, unzipDialog;
    TextraButton saveButton, resumeButton, continueButton;
    TypingLabel version = Controls.newTypingLabel("{GRADIENT}[%80]v." + Forge.getDeviceAdapter().getVersionString() + "{ENDGRADIENT}");


    public StartScene() {
        super(Forge.isLandscapeMode() ? "ui/start_menu.json" : "ui/start_menu_portrait.json");
        ui.onButtonPress("Start", StartScene.this::NewGame);
        ui.onButtonPress("Start+", this::NewGamePlus);
        ui.onButtonPress("Load", StartScene.this::Load);
        ui.onButtonPress("Save", StartScene.this::Save);
        ui.onButtonPress("Resume", StartScene.this::Resume);
        ui.onButtonPress("Continue", StartScene.this::Continue);
        ui.onButtonPress("Settings", StartScene.this::settings);
        ui.onButtonPress("Backup", StartScene.this::backup);
        ui.onButtonPress("Exit", StartScene.this::Exit);
        ui.onButtonPress("Switch", StartScene.this::switchToClassic);


        saveButton = ui.findActor("Save");
        resumeButton = ui.findActor("Resume");
        continueButton = ui.findActor("Continue");

        saveButton.setVisible(false);
        resumeButton.setVisible(false);
        version.setHeight(5);
        version.skipToTheEnd();
        ui.addActor(version);
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
        if (TileMapScene.instance().currentMap().isInMap()) {
            Dialog noSave = createGenericDialog("", Forge.getLocalizer().getMessage("lblGameNotSaved"), Forge.getLocalizer().getMessage("lblOK"),null, null, null);
            showDialog(noSave);
        } else {
            SaveLoadScene.instance().setMode(SaveLoadScene.Modes.Save);
            Forge.switchScene(SaveLoadScene.instance());
        }
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

    boolean loaded = false;

    public boolean Continue() {
        final String lastActiveSave = Config.instance().getSettingData().lastActiveSave;

        if (WorldSave.isSafeFile(lastActiveSave)) {
            if (loaded)
                return true;
            loaded = true;
            try {
                Forge.setTransitionScreen(new TransitionScreen(() -> {
                    loaded = false;
                    if (WorldSave.load(WorldSave.filenameToSlot(lastActiveSave))) {
                        SoundSystem.instance.changeBackgroundTrack();
                        Forge.switchScene(GameScene.instance());
                    } else {
                        Forge.clearTransitionScreen();
                    }
                }, null, false, true, Forge.getLocalizer().getMessage("lblLoadingWorld")));
            } catch (Exception e) {
                loaded = false;
                Forge.clearTransitionScreen();
            }
        }

        return true;
    }

    public boolean settings() {
        Forge.switchScene(SettingsScene.instance());
        return true;
    }

    public boolean backup() {
        if (backupDialog == null) {
            backupDialog = createGenericDialog(Forge.getLocalizer().getMessage("lblData"),
                    null, Forge.getLocalizer().getMessage("lblBackup"),
                    Forge.getLocalizer().getMessage("lblRestore"),
                    () -> {
                        removeDialog();
                        Timer.schedule(new Timer.Task() {
                            @Override
                            public void run() {
                                generateBackup();
                            }
                        }, 0.2f);
                    },
                    () -> {
                        removeDialog();
                        Timer.schedule(new Timer.Task() {
                            @Override
                            public void run() {
                                restoreBackup();
                            }
                        }, 0.2f);
                    }, true, Forge.getLocalizer().getMessage("lblCancel"));
        }
        showDialog(backupDialog);
        return true;
    }
    public boolean generateBackup() {
        try {
            File source = new FileHandle(ForgeProfileProperties.getUserDir() + "/adventure/Shandalar").file();
            File target = new FileHandle(Forge.getDeviceAdapter().getDownloadsDir()).file();
            ZipUtil.zip(source, target, ZipUtil.backupAdvFile);
            zipDialog = createGenericDialog("",
                    Forge.getLocalizer().getMessage("lblSaveLocation") + "\n" + target.getAbsolutePath() + File.separator + ZipUtil.backupAdvFile,
                    Forge.getLocalizer().getMessage("lblOK"), null, this::removeDialog, null);
        } catch (IOException e) {
            zipDialog = createGenericDialog("",
                    Forge.getLocalizer().getMessage("lblErrorSavingFile") + "\n\n" + e.getMessage(),
                    Forge.getLocalizer().getMessage("lblOK"), null, this::removeDialog, null);
        } finally {
            showDialog(zipDialog);
        }
        return true;
    }
    public boolean restoreBackup() {
        File source = new FileHandle(Forge.getDeviceAdapter().getDownloadsDir() + ZipUtil.backupAdvFile).file();
        File target = new FileHandle(ForgeProfileProperties.getUserDir() + "/adventure/Shandalar").file().getParentFile();
        if (unzipDialog == null) {
            unzipDialog = createGenericDialog("",
                    Forge.getLocalizer().getMessage("lblDoYouWantToRestoreBackup"),
                    Forge.getLocalizer().getMessage("lblYes"), Forge.getLocalizer().getMessage("lblNo"),
                    () -> {
                        removeDialog();
                        Timer.schedule(new Timer.Task() {
                            @Override
                            public void run() {
                                extract(source, target);
                            }
                        }, 0.2f);
                    }, this::removeDialog);
        }
        showDialog(unzipDialog);
        return true;
    }
    public boolean extract(File source, File target) {
        String title = "", val = "";
        try {
            val = Forge.getLocalizer().getMessage("lblFiles") + ":\n" + ZipUtil.unzip(source, target);
        } catch (IOException e) {
            title = Forge.getLocalizer().getMessage("lblError");
            val = e.getMessage();
        } finally {
            Config.instance().getSettingData().lastActiveSave = null;
            Config.instance().saveSettings();
            showDialog(createGenericDialog(title, val,
                    Forge.getLocalizer().getMessage("lblOK"), null, this::removeDialog, null));
        }
        return true;
    }

    public boolean Exit() {
        if (exitDialog == null) {
            exitDialog = createGenericDialog(Forge.getLocalizer().getMessage("lblExitForge"),
                    Forge.getLocalizer().getMessage("lblAreYouSureYouWishExitForge"), Forge.getLocalizer().getMessage("lblOK"),
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

    public void updateResumeContinue() {
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
    }

    @Override
    public void enter() {
        boolean hasSaveButton = WorldSave.getCurrentSave().getWorld().getData() != null;
        if (hasSaveButton) {
            TileMapScene scene = TileMapScene.instance();
            hasSaveButton = !scene.currentMap().isInMap() || scene.isAutoHealLocation();
        }
        saveButton.setVisible(hasSaveButton);
        saveButton.setDisabled(TileMapScene.instance().currentMap().isInMap());
        updateResumeContinue();

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
