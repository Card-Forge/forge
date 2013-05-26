package forge.control.input;

import forge.game.phase.PhaseHandler;
import forge.game.player.Player;

public abstract class InputPassPriorityBase extends InputBase {
    private static final long serialVersionUID = -4038934296796872326L;
    protected final Player player;
    
    public InputPassPriorityBase(Player p) {
        this.player = p;
    }
    
    final Runnable passPriority = new Runnable() {
        @Override public void run() {
            player.getController().passPriority();
        }
    };
    
    protected final void pass() { // no futher overloads possible
        setFinished();
        getQueue().invokeGameAction(passPriority);
    }

    protected String getTurnPhasePriorityMessage() {
        final PhaseHandler ph = player.getGame().getPhaseHandler();
        final StringBuilder sb = new StringBuilder();
    
        sb.append("Priority: ").append(player).append("\n").append("\n");
        sb.append("Turn : ").append(ph.getPlayerTurn()).append("\n");
        sb.append("Phase: ").append(ph.getPhase().Name).append("\n");
        sb.append("Stack: ");
        if (!player.getGame().getStack().isEmpty()) {
            sb.append(player.getGame().getStack().size()).append(" to Resolve.");
        } else {
            sb.append("Empty");
        }
        sb.append("\n");
        String message = sb.toString();
        return message;
    }
}