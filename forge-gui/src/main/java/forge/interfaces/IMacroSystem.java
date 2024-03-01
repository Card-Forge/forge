package forge.interfaces;

import forge.game.player.actions.PlayerAction;

public interface IMacroSystem {
    void addRememberedAction(PlayerAction action);
    void setRememberedActions();
    void nextRememberedAction();
    boolean isRecording();
    String playbackText();
}
