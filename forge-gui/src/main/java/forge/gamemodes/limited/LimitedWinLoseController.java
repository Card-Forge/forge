package forge.gamemodes.limited;

import forge.game.GameView;
import forge.gui.interfaces.IButton;
import forge.gui.interfaces.IWinLoseView;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.util.Localizer;

public abstract class LimitedWinLoseController {
    private final Localizer localizer = Localizer.getInstance();
    private final IWinLoseView<? extends IButton> view;
    private final GameView lastGame;
    private final boolean wonMatch;
    private LimitedMatch limitedMatch;
    private boolean nextRound = false;

    public LimitedWinLoseController(IWinLoseView<? extends IButton> view0, final GameView game0) {
        view = view0;
        lastGame = game0;
        limitedMatch = FModel.getLimitedMatch();
        wonMatch = lastGame.isMatchWonBy(GamePlayerUtil.getGuiPlayer());
    }

    public void showOutcome() {
        // view.getBtnRestart().setVisible(false);
        // Deliberate; allow replaying bad tournaments

        //TODO: do per-game actions like ante here...

        resetView();
        nextRound = false;

        if (lastGame.isWinner(GamePlayerUtil.getGuiPlayer())) {
            limitedMatch.addWin();
        } else {
            limitedMatch.addLoss();
        }

        view.getBtnRestart().setText(localizer.getMessage("btnRestartRound"));

        showOutcome(() -> {
            if (!lastGame.isMatchOver()) {
                showTournamentInfo(localizer.getMessage("btnTournamentInfo"));
                return;
            }

            if (wonMatch) {
                if (limitedMatch.getCurrentRound() < limitedMatch.getRounds()) {
                    view.getBtnContinue().setText(localizer.getMessage("btnNextRound") + " (" + (limitedMatch.getCurrentRound() + 1)
                            + "/" + limitedMatch.getRounds() + ")");
                    nextRound = true;
                    view.getBtnContinue().setEnabled(true);
                    showTournamentInfo(localizer.getMessage("btnWonRound") + limitedMatch.getCurrentRound() + "/"
                            + limitedMatch.getRounds());
                } else {
                    showTournamentInfo(localizer.getMessage("btnWonTournament"));
                }
            } else {
                view.getBtnContinue().setVisible(false);
                showTournamentInfo(localizer.getMessage("btnLoseRound") + limitedMatch.getCurrentRound() + "/"
                        + limitedMatch.getRounds());
            }
        });
    }

    public final boolean actionOnContinue() {
        resetView();
        if (nextRound) {
            view.hide();
            saveOptions();
            limitedMatch.nextRound();
            return true;
        }
        return false;
    }

    public final void actionOnRestart() {
        resetView();
        // limitedMatch.resetCurrentRound();
    }

    public final void actionOnQuit() {
        resetView();
        limitedMatch.resetCurrentRound();
    }

    /**
     * <p>
     * Shows some tournament info in the custom panel.
     * </p>
     * @param String - the title to be displayed
     */
    private void showTournamentInfo(final String newTitle) {
        showMessage("Round: " + limitedMatch.getCurrentRound() + "/" + limitedMatch.getRounds(), newTitle);
        // + "      Total Wins: " + limitedMatch.getWins()
        // + "      Total Losses: " + limitedMatch.getLosses()
    }

    /**
     * <p>
     * ResetView
     * </p>
     * Restore the default texts to the win/lose panel buttons.
     * 
     */
    private void resetView() {
        view.getBtnQuit().setText(localizer.getMessage("btnQuit"));
        view.getBtnContinue().setText(localizer.getMessage("btnContinue"));
        view.getBtnRestart().setText(localizer.getMessage("btnRestart"));
    }

    protected abstract void showOutcome(Runnable runnable);
    protected abstract void showMessage(String message, String title);
    protected abstract void saveOptions();
}
