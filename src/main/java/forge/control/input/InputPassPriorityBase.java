package forge.control.input;

import forge.FThreads;
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
        

        if( FThreads.isEDT() )
            FThreads.invokeInNewThread(passPriority);
        else 
            passPriority.run();
    }
}