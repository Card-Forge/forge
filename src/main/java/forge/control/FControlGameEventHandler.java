package forge.control;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.eventbus.Subscribe;

import forge.Card;
import forge.FThreads;
import forge.game.Game;
import forge.game.event.GameEvent;
import forge.game.event.GameEventAnteCardsSelected;
import forge.game.event.GameEventCardAttachment;
import forge.game.event.GameEventCardCounters;
import forge.game.event.GameEventCardDamaged;
import forge.game.event.GameEventCardStatsChanged;
import forge.game.event.GameEventCardTapped;
import forge.game.event.GameEventGameFinished;
import forge.game.event.GameEventGameOutcome;
import forge.game.event.GameEventManaPool;
import forge.game.event.GameEventPlayerControl;
import forge.game.event.GameEventPlayerLivesChanged;
import forge.game.event.GameEventPlayerPoisoned;
import forge.game.event.GameEventPlayerPriority;
import forge.game.event.GameEventSpellAbilityCast;
import forge.game.event.GameEventSpellRemovedFromStack;
import forge.game.event.GameEventSpellResolved;
import forge.game.event.GameEventTurnBegan;
import forge.game.event.GameEventTurnPhase;
import forge.game.event.GameEventZone;
import forge.game.event.IGameEventVisitor;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.gui.GuiDialog;
import forge.gui.SOverlayUtils;
import forge.gui.match.CMatchUI;
import forge.gui.match.VMatchUI;
import forge.gui.match.ViewWinLose;
import forge.gui.match.controllers.CMessage;
import forge.gui.match.controllers.CStack;
import forge.gui.match.nonsingleton.VHand;
import forge.gui.toolbox.special.PhaseLabel;

public class FControlGameEventHandler extends IGameEventVisitor.Base<Void> {
    private final FControl fc;
    public FControlGameEventHandler(FControl fc ) {
        this.fc = fc;
    }

    private final boolean LOG_EVENTS = false;
    
    @Subscribe
    public void receiveGameEvent(final GameEvent ev) {
        if ( LOG_EVENTS )
            System.out.println("GE: " + ev.toString());
        ev.visit(this);
    }

    private final AtomicBoolean phaseUpdPlanned = new AtomicBoolean(false);
    @Override
    public Void visit(final GameEventTurnPhase ev) {
        if ( phaseUpdPlanned.getAndSet(true) ) return null;
        
        FThreads.invokeInEdtNowOrLater(new Runnable() { @Override public void run() {
            PhaseHandler pH = fc.getObservedGame().getPhaseHandler();
            Player p = pH.getPlayerTurn();
            PhaseType ph = pH.getPhase();

            phaseUpdPlanned.set(false);
            
            final CMatchUI matchUi = CMatchUI.SINGLETON_INSTANCE;
            PhaseLabel lbl = matchUi.getFieldViewFor(p).getPhaseInidicator().getLabelFor(ph);

            matchUi.resetAllPhaseButtons();
            if (lbl != null) lbl.setActive(true);
        } });
        return null;
    }
    
    /* (non-Javadoc)
     * @see forge.game.event.IGameEventVisitor.Base#visit(forge.game.event.GameEventPlayerPriority)
     */
    
    
    private final AtomicBoolean combatUpdPlanned = new AtomicBoolean(false);
    @Override
    public Void visit(GameEventPlayerPriority event) {
        if ( combatUpdPlanned.getAndSet(true) ) return null;
        FThreads.invokeInEdtNowOrLater(new Runnable() { @Override public void run() {
            combatUpdPlanned.set(false);
            CMatchUI.SINGLETON_INSTANCE.showCombat(fc.getObservedGame().getCombat());
        } });
        return null;
    }
    

    private final AtomicBoolean turnUpdPlanned = new AtomicBoolean(false);
    @Override
    public Void visit(GameEventTurnBegan event) {
        if ( turnUpdPlanned.getAndSet(true) ) return null;

        final Game game = fc.getObservedGame(); // to make sure control gets a correct game instance
        FThreads.invokeInEdtNowOrLater(new Runnable() {
            @Override
            public void run() {
                turnUpdPlanned.set(false);
                CMessage.SINGLETON_INSTANCE.updateText(game);
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
        if ( fc.getObservedGame().isGameOver() )
            return null;

        FThreads.invokeInEdtNowOrLater(new Runnable() { @Override public void run() {
            CMatchUI.SINGLETON_INSTANCE.initHandViews(fc.getLobby().getGuiPlayer());
            VMatchUI.SINGLETON_INSTANCE.populate();
            for(VHand h : VMatchUI.SINGLETON_INSTANCE.getHands()) {
                h.getLayoutControl().updateHand();
            }
        } });
        return null;
    }
    
    private final Runnable unlockGameThreadOnGameOver = new Runnable() { @Override public void run() {
        fc.getInputQueue().onGameOver(); // this will unlock any game threads waiting for inputs to complete
    } };
    
    @Override
    public Void visit(GameEventGameOutcome ev) {
        FThreads.invokeInEdtNowOrLater(unlockGameThreadOnGameOver);
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
    
    private final AtomicBoolean stackUpdPlanned = new AtomicBoolean(false);
    private final Runnable updStack = new Runnable() { @Override public void run() { 
            stackUpdPlanned.set(false);
            CStack.SINGLETON_INSTANCE.update();
        }
    };

    @Override
    public Void visit(GameEventSpellAbilityCast event) {
        if ( !stackUpdPlanned.getAndSet(true) )
            FThreads.invokeInEdtNowOrLater(updStack);
        return null;
    }
    @Override
    public Void visit(GameEventSpellResolved event) {
        if ( !stackUpdPlanned.getAndSet(true) )
            FThreads.invokeInEdtNowOrLater(updStack);
        return null;
    }
    @Override
    public Void visit(GameEventSpellRemovedFromStack event) {
        if ( !stackUpdPlanned.getAndSet(true) )
            FThreads.invokeInEdtNowOrLater(updStack);
        return null;
    }
    


    private final List<Pair<Player, ZoneType>> zonesToUpdate = new Vector<Pair<Player, ZoneType>>();
    private final Runnable updZones = new Runnable() { 
        @Override public void run() { 
            synchronized (zonesToUpdate) {
                CMatchUI.SINGLETON_INSTANCE.updateZones(zonesToUpdate);
                zonesToUpdate.clear();
            }
        }
    };
    
    @Override
    public Void visit(GameEventZone event) {
        if ( event.player != null ) {
            // anything except stack will get here
            updateZone(Pair.of(event.player, event.zoneType));
        } 
        return null;
    }
    
    @Override
    public Void visit(GameEventCardAttachment event) {
        // TODO Auto-generated method stub
        Game game = event.equipment.getGame();
        PlayerZone zEq = (PlayerZone)game.getZoneOf(event.equipment);
        if( event.oldEntiy instanceof Card )
            updateZone(game.getZoneOf((Card)event.oldEntiy));
        if( event.newTarget instanceof Card )
            updateZone(game.getZoneOf((Card)event.newTarget));
        return updateZone(zEq);
    }

    private Void updateZone(Zone z) {
        return updateZone(Pair.of(z.getPlayer(), z.getZoneType()));
    }

    private Void updateZone(Pair<Player, ZoneType> kv) {
        boolean needUpdate = false;
        synchronized (zonesToUpdate) {
            needUpdate = zonesToUpdate.isEmpty();
            if ( !zonesToUpdate.contains(kv) ) {
                zonesToUpdate.add(kv);
            }
        }
        if( needUpdate )
            FThreads.invokeInEdtNowOrLater(updZones);
        return null;
    }
    
    
    private final List<Card> cardsToUpdate = new Vector<Card>();
    private final Runnable updCards = new Runnable() { 
        @Override public void run() { 
            synchronized (cardsToUpdate) {
                CMatchUI.SINGLETON_INSTANCE.updateCards(cardsToUpdate);
                cardsToUpdate.clear();
            }
        }
    };
    
    
    @Override
    public Void visit(GameEventCardTapped event) {
        return updateSingleCard(event.card);
    }

    @Override
    public Void visit(GameEventCardDamaged event) {
        return updateSingleCard(event.damaged);
    }

    @Override
    public Void visit(GameEventCardCounters event) {
        return updateSingleCard(event.card);
    }

    private Void updateSingleCard(Card c) {
        boolean needUpdate = false;
        synchronized (cardsToUpdate) {
            needUpdate = cardsToUpdate.isEmpty();
            if ( !cardsToUpdate.contains(c) ) {
                cardsToUpdate.add(c);
            }
        }
        if( needUpdate )
            FThreads.invokeInEdtNowOrLater(updCards);
        return null;
    }
    
    /* (non-Javadoc)
     * @see forge.game.event.IGameEventVisitor.Base#visit(forge.game.event.GameEventCardStatsChanged)
     */
    @Override
    public Void visit(GameEventCardStatsChanged event) {
        // TODO Smart partial updates
        PlayerZone z = (PlayerZone) event.card.getGame().getZoneOf(event.card);
        return updateZone(z);
    }
    
    // Update manapool
    private final List<Player> manaPoolUpdate = new Vector<Player>();
    private final Runnable updManaPool = new Runnable() { 
        @Override public void run() { 
            synchronized (manaPoolUpdate) {
                CMatchUI.SINGLETON_INSTANCE.updateManaPool(manaPoolUpdate);
                manaPoolUpdate.clear();
            }
        }
    };
    
    @Override
    public Void visit(GameEventManaPool event) {
        boolean invokeUpdate = false; 
        synchronized (manaPoolUpdate) {
            if( !manaPoolUpdate.contains(event.player) ) {
                invokeUpdate = manaPoolUpdate.isEmpty();
                manaPoolUpdate.add(event.player);
            }
        }
        if (invokeUpdate)
            FThreads.invokeInEdtNowOrLater(updManaPool);
        return null;
    }
    
    // Update lives counters 
    private final List<Player> livesUpdate = new Vector<Player>();
    private final Runnable updLives = new Runnable() { 
        @Override public void run() { 
            synchronized (livesUpdate) {
                CMatchUI.SINGLETON_INSTANCE.updateLives(livesUpdate);
                livesUpdate.clear();
            }
        }
    };
    @Override
    public Void visit(GameEventPlayerLivesChanged event) {
        boolean invokeUpdate = false; 
        synchronized (livesUpdate) {
            if( !livesUpdate.contains(event.player) ) {
                invokeUpdate = livesUpdate.isEmpty();
                livesUpdate.add(event.player);
            }
        }
        if (invokeUpdate)
            FThreads.invokeInEdtNowOrLater(updLives);
        return null;
    }

    @Override
    public Void visit(GameEventPlayerPoisoned event) {
        boolean invokeUpdate = false; 
        synchronized (livesUpdate) {
            if( !livesUpdate.contains(event.receiver) ) {
                invokeUpdate = livesUpdate.isEmpty();
                livesUpdate.add(event.receiver);
            }
        }
        if (invokeUpdate)
            FThreads.invokeInEdtNowOrLater(updLives);
        return null;
    }
}