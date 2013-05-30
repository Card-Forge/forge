package forge.control;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.eventbus.Subscribe;

import forge.FThreads;
import forge.game.event.GameEvent;
import forge.game.event.GameEventBlockerAssigned;
import forge.game.event.GameEventGameFinished;
import forge.game.event.GameEventGameStarted;
import forge.game.event.GameEventLandPlayed;
import forge.game.event.GameEventPlayerPriority;
import forge.game.event.GameEventSpellResolved;
import forge.game.event.GameEventTurnPhase;
import forge.game.event.IGameEventVisitor;
import forge.gui.input.InputPlaybackControl;
import forge.gui.match.CMatchUI;

public class FControlGamePlayback extends IGameEventVisitor.Base<Void> {
    private final FControl fc;
    
    private final InputPlaybackControl inputPlayback = new InputPlaybackControl(this);
    private final AtomicBoolean paused = new AtomicBoolean(false);
    
    private final CyclicBarrier gameThreadPauser = new CyclicBarrier(2);
    
    public FControlGamePlayback(FControl fc ) {
        this.fc = fc;
    }
    
    @Subscribe
    public void receiveGameEvent(final GameEvent ev) { ev.visit(this); }

    private int phasesDelay = 200;
    private int combatDelay = 400;
    private int resolveDelay = 600;
    
    private void pauseForEvent(int delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block ignores the exception, but sends it to System.err and probably forge.log.
            e.printStackTrace();
        }
    }


    @Override
    public Void visit(GameEventBlockerAssigned event) {
        pauseForEvent(combatDelay);
        return super.visit(event);
    }
    
    
    /* (non-Javadoc)
     * @see forge.game.event.IGameEventVisitor.Base#visit(forge.game.event.GameEventTurnPhase)
     */
    @Override
    public Void visit(GameEventTurnPhase ev) {
        boolean isUiToStop = CMatchUI.SINGLETON_INSTANCE.stopAtPhase(ev.playerTurn, ev.phase);
        
        switch(ev.phase) {
            case COMBAT_END:
            case COMBAT_DECLARE_ATTACKERS_INSTANT_ABILITY:
            case COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY:
                if( fc.getObservedGame().getPhaseHandler().inCombat() )
                    pauseForEvent(combatDelay);
                break;
            default:
                if( isUiToStop )
                    pauseForEvent(phasesDelay);
                break;
        }
        
        return null;
    }
    
    
    /* (non-Javadoc)
     * @see forge.game.event.IGameEventVisitor.Base#visit(forge.game.event.GameEventDuelFinished)
     */
    @Override
    public Void visit(GameEventGameFinished event) {
        fc.getInputQueue().removeInput(inputPlayback);
        return null;
    }
    

    @Override
    public Void visit(GameEventGameStarted event) {
        fc.getInputQueue().setInput(inputPlayback);
        return null;
    }

    @Override
    public Void visit(GameEventLandPlayed event) {
        pauseForEvent(resolveDelay);
        return super.visit(event);
    }
    
    @Override
    public Void visit(final GameEventSpellResolved event) {
        FThreads.invokeInEdtNowOrLater(new Runnable() {
            @Override
            public void run() {
                CMatchUI.SINGLETON_INSTANCE.setCard(event.spell.getSourceCard());
            }
        });
        
        pauseForEvent(resolveDelay);
        return null;
    }
    
    /* (non-Javadoc)
     * @see forge.game.event.IGameEventVisitor.Base#visit(forge.game.event.GameEventPlayerPriority)
     */
    @Override
    public Void visit(GameEventPlayerPriority event) {
        if ( paused.get() ) {
            try {
                inputPlayback.onGamePaused();
                gameThreadPauser.await();
                gameThreadPauser.reset();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    
    private void releaseGameThread() {
        // just need to run another thread through the barrier... not edt preferrably :)
        fc.getObservedGame().getAction().invoke( new Runnable() {
            @Override
            public void run() {
                try {
                    gameThreadPauser.await();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block ignores the exception, but sends it to System.err and probably forge.log.
                    e.printStackTrace();
                } catch (BrokenBarrierException e) {
                    // TODO Auto-generated catch block ignores the exception, but sends it to System.err and probably forge.log.
                    e.printStackTrace();
                }
            }
        });
    }

    public void resume() {
        paused.set(false);
        releaseGameThread();
        
    }

    public void pause() {
        paused.set(true);
    }

    public void singleStep() {
        releaseGameThread();
    }

    public void endGame() {
        fc.stopGame();
        releaseGameThread();
    }

}