package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.utils.Align;
import com.github.tommyettinger.textra.TextraButton;
import forge.Forge;
import forge.adventure.stage.GameHUD;
import forge.adventure.stage.GameStage;
import forge.adventure.stage.MapStage;
import forge.adventure.util.Config;
import forge.adventure.util.Controls;
import forge.adventure.util.Current;
import forge.adventure.world.WorldSave;
import forge.screens.TransitionScreen;

/**
 * First scene after the splash screen
 */
public class StartScene extends UIScene {

    private static StartScene object;
    TextraButton saveButton, resumeButton, continueButton, newGameButton, newGameButtonPlus, loadButton, settingsButton, exitButton, switchButton, dialogOk, dialogCancel, dialogButtonSelected;
    Dialog dialog;
    private int selected = -1;

    public StartScene() {
        super(Forge.isLandscapeMode() ? "ui/start_menu.json" : "ui/start_menu_portrait.json");
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
        loadButton = ui.findActor("Load");
        saveButton = ui.findActor("Save");
        resumeButton = ui.findActor("Resume");
        continueButton = ui.findActor("Continue");
        settingsButton = ui.findActor("Settings");
        exitButton = ui.findActor("Exit");
        switchButton = ui.findActor("Switch");

        saveButton.setVisible(false);
        resumeButton.setVisible(false);
        dialog = Controls.newDialog(Forge.getLocalizer().getMessage("lblExitForge"));
        dialog.getButtonTable().add(Controls.newLabel(Forge.getLocalizer().getMessage("lblAreYouSureYouWishExitForge"))).colspan(2).pad(2, 15, 2, 15);
        dialog.getButtonTable().row();
        dialogOk = Controls.newTextButton(Forge.getLocalizer().getMessage("lblExit"), () -> Forge.exit(true));
        dialogButtonSelected = dialogOk;
        dialog.getButtonTable().add(dialogOk).width(60).align(Align.left).padLeft(15);
        dialogCancel = Controls.newTextButton(Forge.getLocalizer().getMessage("lblCancel"), () -> dialog.hide());
        dialog.getButtonTable().add(dialogCancel).width(60).align(Align.right).padRight(15);
        dialog.getColor().a = 0;
    }

    public static StartScene instance() {
        if(object==null)
            object=new StartScene();
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
        if (dialog != null)
            dialog.show(stage);
        return true;
    }

    @Override
    public void enter() {
        boolean hasSaveButton = WorldSave.getCurrentSave().getWorld().getData() != null;
        if (hasSaveButton) {
            TileMapScene scene =  TileMapScene.instance();
            hasSaveButton = !scene.currentMap().isInMap() || scene.inTown();
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

        Gdx.input.setInputProcessor(stage); //Start taking input from the ui

        if(Forge.createNewAdventureMap)
        {
            this.NewGame();
            Current.setDebug(true);
            GameStage.maximumScrollDistance=4f;
        }
    }

    @Override
    public void create() {

    }

    @Override
    public boolean keyPressed(int keycode) {
        if (Forge.hasGamepad())
            showGamepadSelector = true;
        if (dialog.getColor().a != 1) {
            if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK) {
                if (WorldSave.getCurrentSave().getWorld().getData() != null) {
                    if (showGamepadSelector)
                        performTouch(resumeButton);
                    else
                        Resume();
                }
            }
            if (keycode == Input.Keys.DPAD_DOWN) {
                selected++;
                if (selected == 1 && Forge.isLandscapeMode())
                    selected++;
                if (!saveButton.isVisible() && selected == 3)
                    selected++;
                if (!resumeButton.isVisible() && selected == 4)
                    selected++;
                if (!continueButton.isVisible() && selected == 5)
                    selected++;
                if (selected > 7 && Forge.isLandscapeMode())
                    selected = 0;
                if (selected > 8 && !Forge.isLandscapeMode())
                    selected = 8;
                setSelected(selected, false);
            } else if (keycode == Input.Keys.DPAD_UP) {
                selected--;
                if (selected == 7 && Forge.isLandscapeMode())
                    selected--;
                if (!continueButton.isVisible() && selected == 5)
                    selected--;
                if (!resumeButton.isVisible() && selected == 4)
                    selected--;
                if (!saveButton.isVisible() && selected == 3)
                    selected--;
                if (selected == 1 && Forge.isLandscapeMode())
                    selected--;
                if (selected < 0)
                    selected = Forge.isLandscapeMode() ? 7 : 0;
                setSelected(selected, false);
            } else if (keycode == Input.Keys.DPAD_RIGHT && Forge.isLandscapeMode()) {
                if (selected == 0 || selected == 7)
                    selected++;
                if (selected > 8)
                    selected = 8;
                setSelected(selected, false);
            } else if (keycode == Input.Keys.DPAD_LEFT && Forge.isLandscapeMode()) {
                if (selected == 1 || selected == 8)
                    selected--;
                if (selected < 0)
                    selected = 0;
                setSelected(selected, false);
            } else if (keycode == Input.Keys.BUTTON_A)
                setSelected(selected, true);
        } else {
            if (keycode == Input.Keys.DPAD_RIGHT) {
                dialogOk.fire(eventExit);
                dialogCancel.fire(eventEnter);
                dialogButtonSelected  = dialogCancel;
            } else if (keycode == Input.Keys.DPAD_LEFT) {
                dialogOk.fire(eventEnter);
                dialogCancel.fire(eventExit);
                dialogButtonSelected = dialogOk;
            } else if (keycode == Input.Keys.BUTTON_A) {
                dialogOk.fire(eventExit);
                dialogCancel.fire(eventExit);
                performTouch(dialogButtonSelected);
            }
        }
        return true;
    }
    private void setSelected(int select, boolean press) {
        if (!showGamepadSelector)
            return;
        unSelectAll();
        switch (select) {
            case 0:
                newGameButton.fire(eventEnter);
                if (press)
                    performTouch(newGameButton);
                break;
            case 1:
                newGameButtonPlus.fire(eventEnter);
                if (press)
                    performTouch(newGameButtonPlus);
                break;
            case 2:
                loadButton.fire(eventEnter);
                if (press)
                    performTouch(loadButton);
                break;
            case 3:
                saveButton.fire(eventEnter);
                if (press)
                    performTouch(saveButton);
                break;
            case 4:
                resumeButton.fire(eventEnter);
                if (press)
                    performTouch(resumeButton);
                break;
            case 5:
                continueButton.fire(eventEnter);
                if (press) {
                    performTouch(continueButton);
                    setSelected(4, false);
                    selected = 4;
                }
                break;
            case 6:
                settingsButton.fire(eventEnter);
                if (press)
                    performTouch(settingsButton);
                break;
            case 7:
                if (Forge.isLandscapeMode()) {
                    exitButton.fire(eventEnter);
                    if (press)
                        performTouch(exitButton);
                } else {
                    switchButton.fire(eventEnter);
                    if (press)
                        performTouch(switchButton);
                }
                break;
            case 8:
                if (Forge.isLandscapeMode()) {
                    switchButton.fire(eventEnter);
                    if (press)
                        performTouch(switchButton);
                } else {
                    exitButton.fire(eventEnter);
                    if (press)
                        performTouch(exitButton);
                }
                break;
            default:
                break;
        }
    }
    private void unSelectAll() {
        if (!showGamepadSelector)
            return;
        newGameButton.fire(eventExit);
        newGameButtonPlus.fire(eventExit);
        loadButton.fire(eventExit);
        saveButton.fire(eventExit);
        resumeButton.fire(eventExit);
        continueButton.fire(eventExit);
        settingsButton.fire(eventExit);
        exitButton.fire(eventExit);
        switchButton.fire(eventExit);
        dialogOk.fire(eventExit);
        dialogCancel.fire(eventExit);
    }
    private void updateSelected() {
        if (dialog.getColor().a == 1) {
            if (Controls.actorContainsVector(dialogOk, pointer)) {
                dialogCancel.fire(eventExit);
                dialogOk.fire(eventEnter);
                dialogButtonSelected = dialogOk;
            }
            if (Controls.actorContainsVector(dialogCancel, pointer)) {
                dialogOk.fire(eventExit);
                dialogCancel.fire(eventEnter);
                dialogButtonSelected = dialogCancel;
            }
            return;
        }
        if (Controls.actorContainsVector(newGameButton, pointer)) {
            newGameButton.fire(eventEnter);
            selected = 0;
        }
        if (Controls.actorContainsVector(newGameButtonPlus, pointer)) {
            newGameButtonPlus.fire(eventEnter);
            selected = 1;
        }
        if (Controls.actorContainsVector(loadButton, pointer)) {
            loadButton.fire(eventEnter);
            selected = 2;
        }
        if (Controls.actorContainsVector(saveButton, pointer)) {
            saveButton.fire(eventEnter);
            selected = 3;
        }
        if (Controls.actorContainsVector(resumeButton, pointer)) {
            resumeButton.fire(eventEnter);
            selected = 4;
        }
        if (Controls.actorContainsVector(continueButton, pointer)) {
            continueButton.fire(eventEnter);
            selected = 5;
        }
        if (Controls.actorContainsVector(settingsButton, pointer)) {
            settingsButton.fire(eventEnter);
            selected = 6;
        }
        if (Controls.actorContainsVector(exitButton, pointer)) {
            exitButton.fire(eventEnter);
            selected = Forge.isLandscapeMode() ? 7 : 8;
        }
        if (Controls.actorContainsVector(switchButton, pointer)) {
            switchButton.fire(eventEnter);
            selected = Forge.isLandscapeMode() ? 8 : 7;
        }
    }

    private void NewGamePlus() {
        SaveLoadScene.instance().setMode(SaveLoadScene.Modes.NewGamePlus);
        Forge.switchScene(SaveLoadScene.instance());
    }
}
