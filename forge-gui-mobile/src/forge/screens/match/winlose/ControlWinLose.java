package forge.screens.match.winlose;

import forge.game.GameView;
import forge.screens.match.MatchController;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;

/** 
 * Default controller for a ViewWinLose object. This class can
 * be extended for various game modes to populate the custom
 * panel in the win/lose screen.
 *
 */
public class ControlWinLose {
    private final ViewWinLose view;
    protected final GameView lastGame;

    /** @param v &emsp; ViewWinLose
     * @param match */
    public ControlWinLose(final ViewWinLose v, GameView game) {
        view = v;
        lastGame = game;
        addListeners();
    }

    /** */
    public void addListeners() {
        view.getBtnContinue().setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                actionOnContinue();
            }
        });

        view.getBtnRestart().setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                actionOnRestart();
            }
        });

        view.getBtnQuit().setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                actionOnQuit();
                view.getBtnQuit().setEnabled(false);
            }
        });
    }

    /** Action performed when "continue" button is pressed in default win/lose UI. */
    public void actionOnContinue() {
        view.hide();
        saveOptions();

        MatchController.getHostedMatch().continueMatch();
    }

    /** Action performed when "restart" button is pressed in default win/lose UI. */
    public void actionOnRestart() {
        view.hide();
        saveOptions();
        MatchController.getHostedMatch().restartMatch();
    }

    /** Action performed when "quit" button is pressed in default win/lose UI. */
    public void actionOnQuit() {
        // Reset other stuff
        saveOptions();
        MatchController.getHostedMatch().endCurrentGame();
        view.hide();
    }

    /**
     * Either continues or restarts a current game. May be overridden for use
     * with other game modes.
     */
    public void saveOptions() {
        MatchController.writeMatchPreferences();
    }

    /**
     * <p>
     * populateCustomPanel.
     * </p>
     * May be overridden as required by controllers for various game modes
     * to show custom information in center panel. Default configuration is empty.
     * 
     * @return boolean, panel has contents or not.
     */
    public void showRewards() {
    }

    /** @return ViewWinLose object this controller is in charge of */
    public ViewWinLose getView() {
        return view;
    }
}
