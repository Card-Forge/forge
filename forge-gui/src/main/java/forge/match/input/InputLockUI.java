package forge.match.input;

import forge.FThreads;
import forge.GuiBase;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.ITriggerEvent;
import forge.util.ThreadUtil;

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
            ButtonUtil.update("", "", false, false, false);
            showMessage("Waiting for actions...");
        }
    };

    protected final boolean isActive() {
        return GuiBase.getInterface().getInputQueue().getInput() == this;
    }

    protected void showMessage(String message) { 
        GuiBase.getInterface().showPromptMessage(message);
    }

    @Override
    public boolean selectCard(Card c, ITriggerEvent triggerEvent) {
        return false;
    }
    @Override
    public void selectAbility(SpellAbility ab) {
    }
    @Override
    public void selectPlayer(Player player, ITriggerEvent triggerEvent) {
    }
    @Override
    public void selectButtonOK() {
    }
    @Override
    public void selectButtonCancel() {
        //cancel auto pass for all players
        Game game = GuiBase.getInterface().getGame();
        for (Player player : game.getPlayers()) {
            player.getController().autoPassCancel();
        }
    }
}
