package forge.control;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.eventbus.Subscribe;

import forge.FThreads;
import forge.GuiBase;
import forge.game.Game;
import forge.game.card.CardView;
import forge.game.event.GameEvent;
import forge.game.event.GameEventBlockersDeclared;
import forge.game.event.GameEventGameFinished;
import forge.game.event.GameEventGameStarted;
import forge.game.event.GameEventLandPlayed;
import forge.game.event.GameEventPlayerPriority;
import forge.game.event.GameEventSpellAbilityCast;
import forge.game.event.GameEventSpellResolved;
import forge.game.event.GameEventTurnPhase;
import forge.game.event.IGameEventVisitor;
import forge.game.player.PlayerView;
import forge.match.MatchUtil;
import forge.match.input.InputPlaybackControl;
import forge.player.PlayerControllerHuman;

public class FControlGamePlayback extends IGameEventVisitor.Base<Void> {
    private InputPlaybackControl inputPlayback;
    private final AtomicBoolean paused = new AtomicBoolean(false);

    private final CyclicBarrier gameThreadPauser = new CyclicBarrier(2);

    private final PlayerControllerHuman humanController;
    public FControlGamePlayback(final PlayerControllerHuman humanController0) {
        humanController = humanController0;
    }

    private Game game;

    public Game getGame() {
        return game;
    }

    public void setGame(Game game0) {
        game = game0;
        inputPlayback = new InputPlaybackControl(game, this);
    }

    @Subscribe
    public void receiveGameEvent(final GameEvent ev) { ev.visit(this); }

    public static final int phasesDelay = 200;
    public static final int combatDelay = 400;
    public static final int castDelay = 400;
    public static final int resolveDelay = 400;

    private boolean fasterPlayback = false;

    private void pauseForEvent(int delay) {
        try {
            Thread.sleep(fasterPlayback ? delay / 10 : delay);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block ignores the exception, but sends it to System.err and probably forge.log.
            e.printStackTrace();
        }
    }

    @Override
    public Void visit(GameEventBlockersDeclared event) {
        pauseForEvent(combatDelay);
        return super.visit(event);
    }

    /* (non-Javadoc)
     * @see forge.game.event.IGameEventVisitor.Base#visit(forge.game.event.GameEventTurnPhase)
     */
    @Override
    public Void visit(GameEventTurnPhase ev) {
        boolean isUiToStop = MatchUtil.getController().stopAtPhase(PlayerView.get(ev.playerTurn), ev.phase);

        switch(ev.phase) {
            case COMBAT_END:
            case COMBAT_DECLARE_ATTACKERS:
            case COMBAT_DECLARE_BLOCKERS:
                if (getGame().getPhaseHandler().inCombat()) {
                    pauseForEvent(combatDelay);
                }
                break;
            default:
                if (isUiToStop) {
                    pauseForEvent(phasesDelay);
                }
                break;
        }

        return null;
    }

    /* (non-Javadoc)
     * @see forge.game.event.IGameEventVisitor.Base#visit(forge.game.event.GameEventDuelFinished)
     */
    @Override
    public Void visit(GameEventGameFinished event) {
        humanController.getInputQueue().removeInput(inputPlayback);
        return null;
    }

    @Override
    public Void visit(GameEventGameStarted event) {
        humanController.getInputQueue().setInput(inputPlayback);
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
                GuiBase.getInterface().setCard(CardView.get(event.spell.getHostCard()));
            }
        });
        pauseForEvent(resolveDelay);
        return null;
    }

    /* (non-Javadoc)
     * @see forge.game.event.IGameEventVisitor.Base#visit(forge.game.event.GameEventSpellAbilityCast)
     */
    @Override
    public Void visit(final GameEventSpellAbilityCast event) {
        FThreads.invokeInEdtNowOrLater(new Runnable() {
            @Override
            public void run() {
                GuiBase.getInterface().setCard(CardView.get(event.sa.getHostCard()));
            }
        });
        pauseForEvent(castDelay);
        return null;
    }

    /* (non-Javadoc)
     * @see forge.game.event.IGameEventVisitor.Base#visit(forge.game.event.GameEventPlayerPriority)
     */
    @Override
    public Void visit(GameEventPlayerPriority event) {
        inputPlayback.updateTurnMessage();
        if (paused.get()) {
            try {
                gameThreadPauser.await();
                gameThreadPauser.reset();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            catch (BrokenBarrierException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public void onGameStopRequested() {
        paused.set(false);
        if (gameThreadPauser.getNumberWaiting() != 0) {
            releaseGameThread();
        }
    }

    private void releaseGameThread() {
        // just need to run another thread through the barrier... not edt preferrably :)

        getGame().getAction().invoke(new Runnable() {
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

    /**
     * TODO: Write javadoc for this method.
     * @param isFast
     */
    public void setSpeed(boolean isFast) {
        fasterPlayback  = isFast;
    }
}