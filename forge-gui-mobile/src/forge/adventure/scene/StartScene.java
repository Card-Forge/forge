package forge.adventure.scene;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Timer;
import com.github.tommyettinger.textra.TextraButton;
import com.github.tommyettinger.textra.TextraLabel;
import com.github.tommyettinger.textra.TypingLabel;
import forge.Forge;
import forge.adventure.stage.GameHUD;
import forge.adventure.stage.GameStage;
import forge.adventure.stage.MapStage;
import forge.adventure.util.AdventureBackgroundDownloader;
import forge.adventure.util.Config;
import forge.adventure.util.Controls;
import forge.adventure.world.WorldSave;
import forge.assets.FSkinTexture;
import forge.gui.GuiBase;
import forge.localinstance.properties.ForgeProfileProperties;
import forge.screens.TransitionScreen;
import forge.sound.SoundSystem;
import forge.util.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.regex.Pattern;

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
        if (Forge.getDeviceAdapter().needFileAccess()) {
            Forge.getDeviceAdapter().requestFileAcces();
            return true;
        }
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
                    }, true, Forge.getLocalizer().getMessage("lblCancel"), false);
        }
        showDialog(backupDialog);
        return true;
    }
    private final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyMMdd_HHmmss");
    private final SelectBox<String> backupSelectBox = Controls.newComboBox();
    private final Array<String> fileNames = new Array<>();
    private final String prefixPattern = ZipUtil.backupAdvFile.replace(".adv", "");
    private final Pattern strictBackupRegex = Pattern.compile("^" + Pattern.quote(prefixPattern) + "_\\d{6}_\\d{6}\\.adv$");
    public boolean generateBackup() {
        try {
            File source = new FileHandle(ForgeProfileProperties.getUserDir() + "/adventure").file();
            File targetDir = new FileHandle(Forge.getDeviceAdapter().getDownloadsDir()).file();

            String baseName = ZipUtil.backupAdvFile.replace(".adv", "");
            String timestampedFileName = baseName + "_" + TIMESTAMP_FORMAT.format(new Date()) + ".adv";
            File targetFile = new File(targetDir, timestampedFileName);

            ZipUtil.zip(source, targetDir, timestampedFileName);

            if (targetFile.exists() && ZipUtil.isValidZip(targetFile)) {
                zipDialog = createGenericDialog("",
                    Forge.getLocalizer().getMessage("lblSaveLocation") + "\n" + targetFile.getAbsolutePath(),
                    Forge.getLocalizer().getMessage("lblOK"), null, this::removeDialog, null);
            } else {
                throw new IOException("Backup verification failed. The generated file is corrupted.");
            }
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
        File downloadDir = new FileHandle(Forge.getDeviceAdapter().getDownloadsDir()).file();

        File[] files = downloadDir.listFiles((dir, name) -> strictBackupRegex.matcher(name).matches());

        if (files == null || files.length == 0) {
            zipDialog = createGenericDialog("",
                Forge.getLocalizer().getMessageorUseDefault("lblNoBackupsFound", "No backups found!"),
                Forge.getLocalizer().getMessage("lblOK"), null, this::removeDialog, null);
            showDialog(zipDialog);
            return false;
        }

        Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

        fileNames.clear();
        for (File file : files) {
            fileNames.add(file.getName());
        }
        backupSelectBox.clearItems();
        backupSelectBox.setItems(fileNames);

        unzipDialog = createGenericDialog("",
            Forge.getLocalizer().getMessage("lblDoYouWantToRestoreBackup"),
            Forge.getLocalizer().getMessage("lblYes"), Forge.getLocalizer().getMessage("lblNo"),
            () -> {
                String selectedName = backupSelectBox.getSelected();
                File source = new File(downloadDir, selectedName);
                File target = new FileHandle(ForgeProfileProperties.getUserDir() + "/adventure").file().getParentFile();
                removeDialog();

                if (!ZipUtil.isValidZip(source)) {
                    zipDialog = createGenericDialog("",
                        Forge.getLocalizer().getMessageorUseDefault("lblCorruptedBackupError", "Corrupted Backup!"),
                        Forge.getLocalizer().getMessage("lblOK"), null, this::removeDialog, null);
                    showDialog(zipDialog);
                    return;
                }

                Timer.schedule(new Timer.Task() {
                    @Override
                    public void run() {
                        try {
                            extract(source, target);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, 0.1f);
            },
            this::removeDialog, false, "", true
        );

        unzipDialog.getContentTable().row();
        unzipDialog.getContentTable().add(backupSelectBox).width(150).pad(5).center();
        unzipDialog.pack();

        showDialog(unzipDialog);
        return true;
    }
    public boolean extract(File source, File target) {
        String title = "", val = "";
        //boolean isError = false;

        try {
            val = Forge.getLocalizer().getMessage("lblFiles") + ":\n" + ZipUtil.unzip(source, target);
        } catch (IOException e) {
            title = Forge.getLocalizer().getMessage("lblError");
            val = e.getMessage();
            //isError = true;
        } finally {
            Config.instance().getSettingData().lastActiveSave = null;
            Config.instance().saveSettings();

            TextraLabel messageLabel = Controls.newTextraLabel(val);
            messageLabel.setWrap(true);

            ScrollPane scrollPane = new ScrollPane(messageLabel);
            scrollPane.setFadeScrollBars(false);

            var resultsDialog = createGenericDialog(title, "",
                Forge.getLocalizer().getMessage("lblOK"), null, () -> {
                    messageLabel.remove();
                    scrollPane.remove();
                    removeDialog();
                }, null);

            //resultsDialog.getContentTable().row().pad(10);

            float dialogWidth = Forge.isLandscapeMode() ? 150f : 100f;
            float dialogHeight = Forge.isLandscapeMode() ? 100f : 150f;

            resultsDialog.getContentTable().add(scrollPane).width(dialogWidth).height(dialogHeight).expand().fill();
            resultsDialog.pack();

            showDialog(resultsDialog);
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
        SoundSystem.instance.stopBackgroundMusic();
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

        FSkinTexture.invalidateAdventureTextures();
        GuiBase.setAdventureDirectory(Config.instance().getPrefix());
        GuiBase.setAdventureCacheDirectory(Config.instance().getCachePrefix());
        AdventureBackgroundDownloader.start();

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
