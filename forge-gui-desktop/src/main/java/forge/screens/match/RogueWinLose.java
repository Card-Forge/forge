package forge.screens.match;

import forge.game.GameView;
import forge.gamemodes.rogue.RogueWinLoseController;
import forge.gui.framework.EDocID;
import forge.screens.home.CHomeUI;
import forge.screens.home.rogue.CSubmenuRogueMap;

/**
 * Win/Lose handler for Rogue Commander mode.
 * Shows rewards after victory and handles navigation back to the Rogue Map.
 */
public class RogueWinLose extends ControlWinLose {
    private final RogueWinLoseController controller;

    /**
     * Instantiates a new rogue commander win/lose handler.
     *
     * @param view0 ViewWinLose object
     * @param game0 GameView object
     * @param matchUI CMatchUI object
     */
    public RogueWinLose(final ViewWinLose view0, final GameView game0, final CMatchUI matchUI) {
        super(view0, game0, matchUI);
        // Get current run from the map controller and pass it to the controller
        var currentRun = CSubmenuRogueMap.SINGLETON_INSTANCE.getCurrentRun();
        controller = new RogueWinLoseController(game0, view0, currentRun);
    }

    /**
     * Populates the custom panel with rewards if the player won.
     *
     * @return true if rewards are shown
     */
    @Override
    public final boolean populateCustomPanel() {
        if (controller == null) {
            System.err.println("ERROR: Controller is null in populateCustomPanel!");
            return false;
        }
        controller.showRewards();
        return true;
    }

    /**
     * When "quit" button is pressed, navigate back to the Rogue Map
     * and update the display.
     */
    @Override
    public final void actionOnQuit() {
        controller.actionOnQuit();
        CSubmenuRogueMap.SINGLETON_INSTANCE.update();
        CHomeUI.SINGLETON_INSTANCE.itemClick(EDocID.HOME_ROGUEMAP);
        super.actionOnQuit();
    }
}
