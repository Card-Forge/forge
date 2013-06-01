package forge.control;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.eventbus.Subscribe;

import forge.Card;
import forge.FThreads;
import forge.game.event.GameEvent;
import forge.game.event.GameEventAnteCardsSelected;
import forge.game.event.GameEventGameFinished;
import forge.game.event.GameEventGameOutcome;
import forge.game.event.GameEventPlayerControl;
import forge.game.event.GameEventTurnBegan;
import forge.game.event.GameEventTurnPhase;
import forge.game.event.IGameEventVisitor;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.gui.GuiDialog;
import forge.gui.SOverlayUtils;
import forge.gui.match.CMatchUI;
import forge.gui.match.VMatchUI;
import forge.gui.match.ViewWinLose;
import forge.gui.match.controllers.CMessage;
import forge.gui.match.nonsingleton.VHand;
import forge.gui.match.nonsingleton.VField.PhaseLabel;

public class FControlGameEventHandler extends IGameEventVisitor.Base<Void> {
    private final FControl fc;
    public FControlGameEventHandler(FControl fc ) {
        this.fc = fc;
    }

    @Subscribe
    public void receiveGameEvent(final GameEvent ev) { ev.visit(this); }

    private final AtomicBoolean phaseUpdPlanned = new AtomicBoolean(false);
    @Override
    public Void visit(final GameEventTurnPhase ev) {
        if ( phaseUpdPlanned.getAndSet(true) ) return null;
        
        FThreads.invokeInEdtNowOrLater(new Runnable() { @Override public void run() {
            Player p = fc.getObservedGame().getPhaseHandler().getPlayerTurn();
            PhaseType ph = fc.getObservedGame().getPhaseHandler().getPhase();

            phaseUpdPlanned.set(false);
            
            final CMatchUI matchUi = CMatchUI.SINGLETON_INSTANCE;
            PhaseLabel lbl = matchUi.getFieldViewFor(p).getLabelFor(ph);

            matchUi.resetAllPhaseButtons();
            if (lbl != null) lbl.setActive(true);
        } });
        

        return null;
    }

    private final AtomicBoolean turnUpdPlanned = new AtomicBoolean(false);
    @Override
    public Void visit(GameEventTurnBegan event) {
        if ( turnUpdPlanned.getAndSet(true) ) return null;

        FThreads.invokeInEdtNowOrLater(new Runnable() {
            @Override
            public void run() {
                turnUpdPlanned.set(false);
                CMessage.SINGLETON_INSTANCE.updateText();
            }
        });
        return null;
    }
    
    @Override
    public Void visit(GameEventAnteCardsSelected ev) {
        // Require EDT here?
        final String nl = System.getProperty("line.separator");
        final StringBuilder msg = new StringBuilder();
        for (final Pair<Player, Card> kv : ((GameEventAnteCardsSelected) ev).cards) {
            msg.append(kv.getKey().getName()).append(" ante: ").append(kv.getValue()).append(nl);
        }
        GuiDialog.message(msg.toString(), "Ante");
        return null;
    }
    
    @Override
    public Void visit(GameEventPlayerControl ev) {
        FThreads.invokeInEdtNowOrLater(new Runnable() { @Override public void run() {
            CMatchUI.SINGLETON_INSTANCE.initHandViews(fc.getLobby().getGuiPlayer());
            VMatchUI.SINGLETON_INSTANCE.populate();
            for(VHand h : VMatchUI.SINGLETON_INSTANCE.getHands()) {
                h.getLayoutControl().updateHand();
            }
        } });
        return null;
    }
    
    @Override
    public Void visit(GameEventGameOutcome ev) {
        FThreads.invokeInEdtNowOrLater(new Runnable() { @Override public void run() {
            fc.getInputQueue().onGameOver(); // this will unlock any game threads waiting for inputs to complete
        } });
        return null;
    }
    
    @Override
    public Void visit(GameEventGameFinished ev) {
        FThreads.invokeInEdtNowOrLater(new Runnable() { @Override public void run() {
            new ViewWinLose(fc.getObservedGame().getMatch());
            SOverlayUtils.showOverlay();
        } });
        return null;
    }
}