package forge.control.input;

import java.util.concurrent.atomic.AtomicInteger;

import forge.Card;
import forge.FThreads;
import forge.Singletons;
import forge.game.player.Player;
import forge.gui.match.CMatchUI;
import forge.view.ButtonUtil;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class InputLockUI implements Input  {
    private final AtomicInteger iCall = new AtomicInteger();
    
    public void showMessage() {
        int ixCall = 1 + iCall.getAndIncrement();
        FThreads.delay(500, new InputUpdater(ixCall));
    }
    
    @Override
    public String toString() {
        return "lockUI"; 
    }
    
    private class InputUpdater implements Runnable {
        final int ixCall;
        
        public InputUpdater(final int idxCall) {
            ixCall = idxCall;
        }
        
        @Override
        public void run() {
            if ( ixCall != iCall.get() || !isActive()) // cancel the message if it's not from latest call or input is gone already 
                return;
            FThreads.invokeInEDT(showMessageFromEdt);
        }
    };
    
    private final Runnable showMessageFromEdt = new Runnable() {
        
        @Override
        public void run() {
            ButtonUtil.disableAll();
            showMessage("Waiting for actions...");
        }
    };
    
    protected final boolean isActive() {
        return Singletons.getModel().getMatch().getInput().getInput() == this;
    }

    protected void showMessage(String message) { 
        CMatchUI.SINGLETON_INSTANCE.showMessage(message);
    }

    @Override public void selectCard(Card c) {}
    @Override public void selectPlayer(Player player) {}
    @Override public void selectButtonOK() {}
    @Override public void selectButtonCancel() {}

}
