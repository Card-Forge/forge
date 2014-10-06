package forge.match.input;

import forge.control.FControlGamePlayback;
import forge.game.Game;
import forge.game.phase.PhaseHandler;
import forge.match.MatchUtil;
import forge.view.LocalGameView;
import forge.view.PlayerView;

public class InputPlaybackControl extends InputSyncronizedBase implements InputSynchronized {
    private static final long serialVersionUID = 7979208993306642072L;

    final FControlGamePlayback control;

    private boolean isPaused = false;
    private boolean isFast = false;

    private final Game game;
    public InputPlaybackControl(final Game game0, final FControlGamePlayback fControlGamePlayback) {
        super(null);
        game = game0;
        control = fControlGamePlayback;
        setPause(false);
    }

    @Override
    public LocalGameView getGameView() {
        return MatchUtil.getGameView();
    }
    @Override
    public PlayerView getOwner() {
        return getGameView().getLocalPlayerView();
    }

    /* (non-Javadoc)
     * @see forge.gui.input.InputBase#showMessage()
     */
    @Override
    protected void showMessage() {
        setPause(false);
    }

    //update message based on current turn and paused state
    private int currentTurn;
    public void updateTurnMessage() {
        if (isPaused) {
            showMessage(getTurnPhasePriorityMessage(game));
            currentTurn = 0;
        }
        else {
            final PhaseHandler ph = game.getPhaseHandler();
            if (currentTurn == ph.getTurn()) { return; }

            currentTurn = ph.getTurn();
            showMessage("Turn " + currentTurn + " (" + ph.getPlayerTurn() + ")");
        }
    }

    private void setPause(boolean pause) {
        isPaused = pause; 
        if (isPaused) {
            ButtonUtil.update(getOwner(), "Resume", "Step", true, true, true);
        }
        else {
            ButtonUtil.update(getOwner(), "Pause", isFast ? "1x Speed" : "10x Faster", true, true, true);
        }
    }

    public void pause() {
        if (isPaused) { return; }
        control.pause();
        setPause(true);
    }

    @Override
    protected void onOk() {
        if (isPaused) {
            control.resume();
            setPause(false);
        }
        else {
            control.pause();
            setPause(true);
        }
    }

    @Override
    protected void onCancel() {
        if (isPaused) {
            control.singleStep();
        }
        else {
            isFast = !isFast;
            control.setSpeed(isFast);
            setPause(isPaused); // update message
        }
    }
}
