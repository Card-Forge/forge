package forge.screens.match.input;

import forge.FThreads;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.screens.match.FControl;
import forge.util.ThreadUtil;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class InputLockUI implements Input  {
    private final AtomicInteger iCall = new AtomicInteger();

    public InputLockUI(InputQueue inputQueue) {
    }

    public void showMessageInitial() {
        int ixCall = 1 + iCall.getAndIncrement();
        ThreadUtil.delay(500, new InputUpdater(ixCall));
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
            FThreads.invokeInEdtLater(showMessageFromEdt);
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
        return FControl.getInputQueue().getInput() == this;
    }

    protected void showMessage(String message) { 
        FControl.showMessage(message);
    }

    @Override public void selectCard(final Card card, final List<Card> orderedCardOptions) {}
    @Override public void selectAbility(SpellAbility ab) {}
    @Override public void selectPlayer(Player player) {}
    @Override public void selectButtonOK() {}
    @Override public void selectButtonCancel() {}
}
