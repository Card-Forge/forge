package forge.screens.match.winlose;

import forge.Forge;
import forge.adventure.scene.DuelScene;
import forge.game.GameView;

public class AdventureWinLose extends ControlWinLose {
    /**
     * @param v    &emsp; ViewWinLose
     * @param game
     */
    public AdventureWinLose(ViewWinLose v, GameView game) {
        super(v, game);
        v.getBtnContinue().setVisible(false);
        v.getBtnRestart().setVisible(false);
        v.getLabelShowBattlefield().setVisible(false);
        v.getBtnQuit().setText(Forge.getLocalizer().getMessage("lblBackToAdventure"));
        Forge.setCursor(null, "0");
    }

    @Override
    public void actionOnContinue() {
        //Do Nothing
    }

    @Override
    public void actionOnRestart() {
        //Do Nothing
    }

    @Override
    public void actionOnQuit() {
        getView().hide();
        DuelScene.instance().exitDuelScene();
    }

    @Override
    public void saveOptions() {
        //Do Nothing
    }

    @Override
    public void showRewards() {
        //Do Nothing
    }
}
