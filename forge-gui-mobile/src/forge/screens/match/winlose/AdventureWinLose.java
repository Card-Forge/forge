package forge.screens.match.winlose;

import forge.Forge;
import forge.adventure.scene.DuelScene;
import forge.game.GameView;
import forge.screens.match.MatchController;

public class AdventureWinLose extends ControlWinLose {
    /**
     * @param v    &emsp; ViewWinLose
     * @param game
     */
    public AdventureWinLose(ViewWinLose v, GameView game) {
        super(v, game);

        if (lastGame.isMatchOver()) {
            v.getBtnQuit().setText(Forge.getLocalizer().getMessage("lblBackToAdventure"));
            //v.getBtnContinue().setVisible(false);
        }
        else{
            v.getBtnContinue().setVisible(true);
            v.getBtnContinue().setEnabled(true);
            v.getBtnContinue().setText(Forge.getLocalizer().getMessage("btnNextGame"));
            v.getBtnQuit().setText(Forge.getLocalizer().getMessageorUseDefault("lblQuitAdventureEventMatch", "Quit Match (will count as a loss)"));
        }
        v.getBtnRestart().setVisible(false);
        v.getBtnRestart().setEnabled(false);
        v.getLabelShowBattlefield().setVisible(false);
        //v.getBtnQuit().setText(Forge.getLocalizer().getMessage("lblBackToAdventure"));
        Forge.setCursor(null, "0");
    }

    @Override
    public void actionOnContinue() {
        super.actionOnContinue();
    }

    @Override
    public void actionOnRestart() {
        saveOptions();
    }

    @Override
    public void actionOnQuit() {
        saveOptions();
        getView().hide();
        DuelScene.instance().GameEnd();
        DuelScene.instance().exitDuelScene();
    }

    @Override
    public void saveOptions() {
        MatchController.writeMatchPreferences();
    }

    @Override
    public void showRewards() {
        //Do Nothing
    }
}
