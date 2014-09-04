package forge.match.input;

import java.util.concurrent.atomic.AtomicInteger;

import forge.FThreads;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.interfaces.IGuiBase;
import forge.util.ITriggerEvent;
import forge.util.ThreadUtil;

public class InputLockUI implements Input  {
    private final AtomicInteger iCall = new AtomicInteger();

    private final IGuiBase gui;
    private final Game game;
    public InputLockUI(final IGuiBase gui, final Game game, final InputQueue inputQueue) {
        this.gui = gui;
        this.game = game;
    }

    private IGuiBase getGui() {
        return gui;
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
            FThreads.invokeInEdtLater(getGui(), showMessageFromEdt);
        }
    };
    
    private final Runnable showMessageFromEdt = new Runnable() {
        @Override
        public void run() {
            ButtonUtil.update(getGui(), "", "", false, false, false);
            showMessage("Waiting for actions...");
        }
    };

    protected final boolean isActive() {
        return getGui().getInputQueue().getInput() == this;
    }

    protected void showMessage(String message) { 
        getGui().showPromptMessage(message);
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
        for (Player player : game.getPlayers()) {
            player.getController().autoPassCancel();
        }
    }
}
