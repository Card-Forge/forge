package forge.screens.match;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

import forge.Singletons;
import forge.game.GameView;
import forge.gui.SOverlayUtils;
import forge.gui.framework.FScreen;
import forge.interfaces.IGameController;
import forge.match.NextGameDecision;

/** 
 * Default controller for a ViewWinLose object. This class can
 * be extended for various game modes to populate the custom
 * panel in the win/lose screen.
 *
 */
public class ControlWinLose {
    private final ViewWinLose view;
    protected final GameView lastGame;
    protected final CMatchUI matchUI;

    /** @param v &emsp; ViewWinLose
     * @param match */
    public ControlWinLose(final ViewWinLose v, final GameView game0, final CMatchUI matchUI) {
        this.view = v;
        this.lastGame = game0;
        this.matchUI = matchUI;
        addListeners();
    }

    /** */
    public void addListeners() {
        view.getBtnContinue().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                actionOnContinue();
            }
        });

        view.getBtnRestart().addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                actionOnRestart();
            }
        });

        view.getBtnQuit().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                actionOnQuit();
                ((JButton) e.getSource()).setEnabled(false);
            }
        });
    }

    /** Action performed when "continue" button is pressed in default win/lose UI. */
    public void actionOnContinue() {
        nextGameAction(NextGameDecision.CONTINUE);
    }

    /** Action performed when "restart" button is pressed in default win/lose UI. */
    public void actionOnRestart() {
        nextGameAction(NextGameDecision.NEW);
    }

    /** Action performed when "quit" button is pressed in default win/lose UI. */
    public void actionOnQuit() {
        nextGameAction(NextGameDecision.QUIT);
        Singletons.getControl().setCurrentScreen(FScreen.HOME_SCREEN);
    }

    private void nextGameAction(final NextGameDecision decision) {
        SOverlayUtils.hideOverlay();
        saveOptions();
        for (final IGameController controller : matchUI.getOriginalGameControllers()) {
            controller.nextGameDecision(decision);
        }
    }

    /**
     * Either continues or restarts a current game. May be overridden for use
     * with other game modes.
     */
    public void saveOptions() {
        matchUI.writeMatchPreferences();
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
    public boolean populateCustomPanel() {
        return false;
    }

    /** @return ViewWinLose object this controller is in charge of */
    public ViewWinLose getView() {
        return this.view;
    }
}
