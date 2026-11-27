package forge.screens.match;

import forge.game.GameView;
import forge.gamemodes.rogue.RogueMetaProgress;
import forge.gamemodes.rogue.RogueWinLoseController;
import forge.gamemodes.rogue.flow.RogueFlow;
import forge.gui.framework.EDocID;
import forge.localinstance.skin.FSkinProp;
import forge.screens.home.CHomeUI;
import forge.screens.home.rogue.CSubmenuRogueMap;
import forge.screens.home.rogue.RogueUIHelper;
import forge.util.Localizer;

/**
 * Win/Lose handler for Rogue Commander mode.
 * Shows rewards after victory and handles navigation back to the Rogue Map.
 */
public class RogueWinLose extends ControlWinLose {
    private final RogueFlow flow = CSubmenuRogueMap.SINGLETON_INSTANCE.getFlow();
    private RogueWinLoseController controller;
    private final Localizer localizer = Localizer.getInstance();

    /**
     * Instantiates a new rogue commander win/lose handler.
     *
     * @param view0 ViewWinLose object
     * @param game0 GameView object
     * @param matchUI CMatchUI object
     */
    public RogueWinLose(final ViewWinLose view0, final GameView game0, final CMatchUI matchUI) {
        super(view0, game0, matchUI);
    }

    /**
     * Populates the custom panel with rewards if the player won.
     *
     * @return true if rewards are shown
     */
    @Override
    public final boolean populateCustomPanel() {
        controller = flow.matchOver(lastGame, getView());
        if (flow.offersEnding(controller)) {
            boolean unlocked = RogueMetaProgress.getInstance().isEndlessModeUnlocked();
            getView().getBtnContinue().setText(localizer.getMessage("lblRogueCompleteRun"));
            getView().getBtnContinue().setVisible(true);
            getView().getBtnContinue().setEnabled(true);
            getView().getBtnQuit().setText(localizer.getMessage("lblRogueGoEndless"));
            getView().getBtnQuit().setEnabled(unlocked);
            if (!unlocked) {
                getView().showQuitButtonHint(RogueUIHelper.html(localizer.getMessage("lblRogueUnlockEndlessHint"), true),
                    FSkinProp.ICO_LOCK);
            }
        }
        return true;
    }

    @Override
    public String getOutcomeTitleOverride() {
        return flow.resultTitle(controller);
    }

    @Override
    public boolean shouldShowPlayerScores() {
        return false;
    }

    @Override
    public final void actionOnQuit() {
        if (flow.offersEnding(controller)) {
            if (flow.run().isRunWon() || !RogueMetaProgress.getInstance().isEndlessModeUnlocked()) {
                return;
            }
            getView().getBtnQuit().setEnabled(false);
            leaveWinLose(true);
            return;
        }
        leaveWinLose(false);
    }

    @Override
    public final void actionOnContinue() {
        if (!flow.offersEnding(controller) || flow.run().isRunWon()) {
            return;
        }
        getView().getBtnContinue().setEnabled(false);
        leaveWinLose(false);
    }

    private void leaveWinLose(boolean goEndless) {
        flow.resultLeft(controller, goEndless);
        if (flow.run() != null) {
            CHomeUI.SINGLETON_INSTANCE.itemClick(EDocID.HOME_ROGUEMAP);
        }

        super.actionOnQuit();
    }
}
