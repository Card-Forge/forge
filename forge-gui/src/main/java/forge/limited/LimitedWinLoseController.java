package forge.limited;

import forge.game.GameView;
import forge.interfaces.IButton;
import forge.interfaces.IWinLoseView;
import forge.model.FModel;
import forge.player.GamePlayerUtil;

public abstract class LimitedWinLoseController {
    private final IWinLoseView<? extends IButton> view;
    private final GameView lastGame;
    private final boolean wonMatch;
    private GauntletMini gauntlet;
    private boolean nextRound = false;

    public LimitedWinLoseController(IWinLoseView<? extends IButton> view0, final GameView game0) {
        view = view0;
        lastGame = game0;
        gauntlet = FModel.getGauntletMini();
        wonMatch = lastGame.isMatchWonBy(GamePlayerUtil.getGuiPlayer());
    }

    public void showOutcome() {
        // view.getBtnRestart().setVisible(false);
        // Deliberate; allow replaying bad tournaments

        //TODO: do per-game actions like ante here...

        resetView();
        nextRound = false;

        if (lastGame.isWinner(GamePlayerUtil.getGuiPlayer())) {
            gauntlet.addWin();
        }
        else {
            gauntlet.addLoss();
        }

        view.getBtnRestart().setText("Restart Round");

        showOutcome(new Runnable() {
            @Override
            public void run() {
                if (!lastGame.isMatchOver()) {
                    showTournamentInfo("Tournament Info");
                    return;
                }

                if (wonMatch) {
                    if (gauntlet.getCurrentRound() < gauntlet.getRounds()) {
                        view.getBtnContinue().setText("Next Round (" + (gauntlet.getCurrentRound() + 1)
                                + "/" + gauntlet.getRounds() + ")");
                        nextRound = true;
                        view.getBtnContinue().setEnabled(true);
                        showTournamentInfo("YOU HAVE WON ROUND " + gauntlet.getCurrentRound() + "/"
                                + gauntlet.getRounds());
                    }
                    else {
                        showTournamentInfo("***CONGRATULATIONS! YOU HAVE WON THE TOURNAMENT!***");
                    }
                }
                else {
                    view.getBtnContinue().setVisible(false);
                    showTournamentInfo("YOU HAVE LOST ON ROUND " + gauntlet.getCurrentRound() + "/"
                            + gauntlet.getRounds());
                }
            }
        });
    }

    public final boolean actionOnContinue() {
        resetView();
        if (nextRound) {
            view.hide();
            saveOptions();
            gauntlet.nextRound();
            return true;
        }
        return false;
    }

    public final void actionOnRestart() {
        resetView();
        // gauntlet.resetCurrentRound();
    }

    public final void actionOnQuit() {
        resetView();
        gauntlet.resetCurrentRound();
    }

    /**
     * <p>
     * Shows some tournament info in the custom panel.
     * </p>
     * @param String - the title to be displayed
     */
    private void showTournamentInfo(final String newTitle) {
        showMessage("Round: " + gauntlet.getCurrentRound() + "/" + gauntlet.getRounds(), newTitle);
        // + "      Total Wins: " + gauntlet.getWins()
        // + "      Total Losses: " + gauntlet.getLosses()
    }

    /**
     * <p>
     * ResetView
     * </p>
     * Restore the default texts to the win/lose panel buttons.
     * 
     */
    private void resetView() {
        view.getBtnQuit().setText("Quit");
        view.getBtnContinue().setText("Continue");
        view.getBtnRestart().setText("Restart");
    }

    protected abstract void showOutcome(Runnable runnable);
    protected abstract void showMessage(String message, String title);
    protected abstract void saveOptions();
}
