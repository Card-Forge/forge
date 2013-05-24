package forge.control.input;

import forge.FThreads;
import forge.game.player.Player;

public abstract class InputNonSyncBase extends InputBase {
    private static final long serialVersionUID = -4038934296796872326L;
    protected final Player player;
    
    public InputNonSyncBase(Player p) {
        this.player = p;
    }
    
    protected void passPriority() {
        final Runnable pass = new Runnable() {
            @Override public void run() {
                player.getController().passPriority();
            }
        };
        if( FThreads.isEDT() )
            player.getGame().getInputQueue().LockAndInvokeGameAction(pass);
        else 
            pass.run();
    }
}