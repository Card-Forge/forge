package forge.control;

import com.google.common.eventbus.Subscribe;

import forge.game.event.GameEvent;
import forge.game.event.GameEventBlockerAssigned;
import forge.game.event.GameEventGameStarted;
import forge.game.event.GameEventLandPlayed;
import forge.game.event.GameEventSpellResolved;
import forge.game.event.GameEventTurnPhase;
import forge.game.event.IGameEventVisitor;
import forge.game.player.Player;
import forge.gui.match.CMatchUI;

public class FControlGamePlayback extends IGameEventVisitor.Base<Void> {
    private final FControl fc;
    public FControlGamePlayback(FControl fc ) {
        this.fc = fc;
    }
    
    @Subscribe
    public void receiveGameEvent(final GameEvent ev) { ev.visit(this); }

    private int phasesDelay = 400;
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
     * @see forge.game.event.IGameEventVisitor.Base#visit(forge.game.event.GameEventGameStarted)
     */
    @Override
    public Void visit(GameEventGameStarted event) {
        boolean hasHuman = false;
        for(Player p :  event.players) {
            if ( p.getController().getLobbyPlayer() == fc.getLobby().getGuiPlayer() )
                hasHuman = true;
        }
        
    
        // show input here to adjust speed if no human playing
        
        return null;
    }


    @Override
    public Void visit(GameEventLandPlayed event) {
        pauseForEvent(resolveDelay);
        return super.visit(event);
    }
    
    @Override
    public Void visit(GameEventSpellResolved event) {
        pauseForEvent(resolveDelay);
        return null;
    }

}