package forge.player;

import com.google.common.collect.Lists;
import forge.game.GameEntityView;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.player.actions.FinishTargetingAction;
import forge.game.player.actions.PassPriorityAction;
import forge.game.player.actions.PlayerAction;
import forge.gamemodes.match.input.Input;
import forge.gamemodes.match.input.InputPassPriority;
import forge.gamemodes.match.input.InputSelectTargets;
import forge.interfaces.IMacroSystem;
import forge.util.ITriggerEvent;
import forge.util.Localizer;

import java.util.List;

// Iteration on the current limited macro system. Instead of asking for IDs to click on
// Instead we wrap the input queue in a way that we can record what the player is doing and
// try to play it back as much as possible

public class RecordActionsMacroSystem implements IMacroSystem {
    private final PlayerControllerHuman playerControllerHuman;
    private final Localizer localizer = Localizer.getInstance();

    private final List<PlayerAction> actions = Lists.newArrayList();
    private final List<PlayerAction> playbackActions = Lists.newArrayList();


    private boolean recording = false;

    public RecordActionsMacroSystem(PlayerControllerHuman playerControllerHuman) {
        this.playerControllerHuman = playerControllerHuman;
    }

    public boolean isRecording() { return recording; }

    public boolean startRecording() {
        if (recording) {
            return false;
        }

        recording = true;
        actions.clear();
        playbackActions.clear();

        return true;
    }

    public boolean finishRecording() {
        if (!recording) {
            return false;
        }

        recording = false;
        playbackActions.addAll(actions);

        return true;
    }

    @Override
    public void addRememberedAction(PlayerAction action) {
        if (!recording) {
            return;
        }

        actions.add(action);
    }

    @Override
    public void setRememberedActions() {
        if (recording) {
            finishRecording();
        } else {
            startRecording();
        }
    }

    public void runFullMacro() {
        if (actions.isEmpty() || recording) {
            return;
        }

        if (playbackActions.isEmpty()) {
            playbackActions.addAll(actions);
        } else if (actions.size() != playbackActions.size()) {
            // Not at the beginning of the loop
            return;
        }

        while(!playbackActions.isEmpty()) {
            runFirstAction();
        }
    }

    @Override
    public void nextRememberedAction() {
        if (actions.isEmpty()) {
            // Didn't record anything. We should warn the user.
            // playerControllerHuman.getGui().message(localizer.getMessage("lblPleaseDefineanActionSequenceFirst"), dialogTitle);
            return;
        }

        if (recording) {
            // In the middle of a recording can't run macros
            System.out.println("Make sure macros are finished recording before running them...");
            return;
        }

        if (playbackActions.isEmpty()) {
            playbackActions.addAll(actions);
            // Finished a loop. Reset loop
            System.out.println("Finished macro loop. Restarting at the beginning...");
            // playerControllerHuman.getGui().message(localizer.getMessage("lblPleaseDefineanActionSequenceFirst"), dialogTitle);
            // return;
        }

        runFirstAction();
    }

    private void runFirstAction() {
        PlayerAction action = playbackActions.remove(0);
        processAction(action);
    }

    public void processAction(PlayerAction action) {
        // TODO Add Actions that haven't been covered yet
        if (action instanceof PassPriorityAction) {
            final Input inp = playerControllerHuman.getInputProxy().getInput();
            if (inp instanceof InputPassPriority) {
                inp.selectButtonOK();
            }
        } else if (action instanceof FinishTargetingAction) {
            final Input inp = playerControllerHuman.getInputProxy().getInput();
            if (inp instanceof InputSelectTargets) {
                inp.selectButtonOK();
            }
        } else {
            GameEntityView gev = action.getGameEntityView();
            if (gev instanceof CardView) {
                playerControllerHuman.selectCard((CardView)gev, null, new DummyTriggerEvent());
            } else if (gev instanceof PlayerView) {
                playerControllerHuman.selectPlayer((PlayerView)gev, null);
            }
        }
    }

    private class DummyTriggerEvent implements ITriggerEvent {
        @Override
        public int getButton() {
            return 1; // Emulate left mouse button
        }

        @Override
        public int getX() {
            return 0; // Hopefully this doesn't do anything wonky!
        }

        @Override
        public int getY() {
            return 0;
        }
    }
}
