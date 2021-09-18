package forge.gamemodes.match.input;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseHandler;
import forge.gui.control.FControlGamePlayback;

public class InputPlaybackControl extends InputSyncronizedBase {
    private static final long serialVersionUID = 7979208993306642072L;

    final FControlGamePlayback control;

    private boolean isPaused = false;
    private boolean isFast = false;

    private final Game game;
    public InputPlaybackControl(final Game game0, final FControlGamePlayback fControlGamePlayback) {
        super(fControlGamePlayback.getController());
        game = game0;
        control = fControlGamePlayback;
        setPause(false);
    }

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
        } else {
            final PhaseHandler ph = game.getPhaseHandler();
            if (currentTurn == ph.getTurn()) { return; }

            currentTurn = ph.getTurn();
            showMessage("Turn " + currentTurn + " (" + ph.getPlayerTurn() + ")");
        }
    }

    private void setPause(final boolean pause) {
        isPaused = pause; 
        if (isPaused) {
            getController().getGui().updateButtons(null, "Resume", "Step", true, true, true);
        } else {
            getController().getGui().updateButtons(null, "Pause", isFast ? "1x Speed" : "10x Faster", true, true, true);
        }
        getController().getGui().setgamePause(isPaused);
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
            getController().getGui().setGameSpeed(isFast);
            setPause(isPaused); // update message
        }
    }

    @Override
    public String getActivateAction(Card card) {
        return null;
    }
}
