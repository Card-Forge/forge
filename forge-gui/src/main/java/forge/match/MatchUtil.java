package forge.match;

import forge.GuiBase;
import forge.game.Game;
import forge.game.player.Player;
import forge.match.input.Input;
import forge.match.input.InputPassPriority;

public class MatchUtil {
    public static boolean undoLastAction() {
        if (canUndoLastAction() && GuiBase.getInterface().getGame().stack.undo()) {
            Input currentInput = GuiBase.getInterface().getInputQueue().getInput();
            if (currentInput instanceof InputPassPriority) {
                currentInput.showMessageInitial(); //ensure prompt updated if needed
            }
            return true;
        }
        return false;
    }

    public static boolean canUndoLastAction() {
        Game game = GuiBase.getInterface().getGame();
        if (game.stack.canUndo()) {
            Player player = game.getPhaseHandler().getPriorityPlayer();
            if (player != null && player.getLobbyPlayer() == GuiBase.getInterface().getGuiPlayer()) {
                return true;
            }
        }
        return false;
    }
}
