package forge.match.input;

import java.util.concurrent.atomic.AtomicInteger;

import forge.FThreads;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.interfaces.IGuiBase;
import forge.match.MatchUtil;
import forge.util.ITriggerEvent;
import forge.util.ThreadUtil;
import forge.view.PlayerView;

public class InputLockUI implements Input  {
    private final AtomicInteger iCall = new AtomicInteger();

    private IGuiBase gui;
    private final InputQueue inputQueue;
    private final Game game;
    public InputLockUI(final Game game0, final InputQueue inputQueue0) {
        game = game0;
        inputQueue = inputQueue0;
    }

    @Override
    public PlayerView getOwner() {
        return null;
    }

    @Override
    public IGuiBase getGui() {
        return gui;
    }

    public void setGui(final IGuiBase gui0) {
        gui = gui0;
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
            ButtonUtil.update(InputLockUI.this.getOwner(), "", "", false, false, false);
            showMessage("Waiting for actions...");
        }
    };

    protected final boolean isActive() {
        return inputQueue.getInput() == this;
    }

    protected void showMessage(String message) { 
        MatchUtil.getController().showPromptMessage(getOwner(), message);
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
