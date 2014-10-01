package forge.control;

import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.eventbus.Subscribe;

import forge.FThreads;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.event.GameEvent;
import forge.game.event.GameEventAnteCardsSelected;
import forge.game.event.GameEventAttackersDeclared;
import forge.game.event.GameEventBlockersDeclared;
import forge.game.event.GameEventCardAttachment;
import forge.game.event.GameEventCardChangeZone;
import forge.game.event.GameEventCardCounters;
import forge.game.event.GameEventCardDamaged;
import forge.game.event.GameEventCardPhased;
import forge.game.event.GameEventCardStatsChanged;
import forge.game.event.GameEventCardTapped;
import forge.game.event.GameEventCombatChanged;
import forge.game.event.GameEventCombatEnded;
import forge.game.event.GameEventGameFinished;
import forge.game.event.GameEventGameOutcome;
import forge.game.event.GameEventManaPool;
import forge.game.event.GameEventPlayerControl;
import forge.game.event.GameEventPlayerLivesChanged;
import forge.game.event.GameEventPlayerPoisoned;
import forge.game.event.GameEventPlayerPriority;
import forge.game.event.GameEventPlayerStatsChanged;
import forge.game.event.GameEventShuffle;
import forge.game.event.GameEventSpellAbilityCast;
import forge.game.event.GameEventSpellRemovedFromStack;
import forge.game.event.GameEventSpellResolved;
import forge.game.event.GameEventTurnBegan;
import forge.game.event.GameEventTurnPhase;
import forge.game.event.GameEventZone;
import forge.game.event.IGameEventVisitor;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.match.MatchUtil;
import forge.match.input.ButtonUtil;
import forge.match.input.InputBase;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.properties.ForgePreferences.FPref;
import forge.util.Lang;
import forge.util.ThreadUtil;
import forge.util.gui.SGuiChoose;
import forge.util.maps.MapOfLists;
import forge.view.CardView;
import forge.view.LocalGameView;
import forge.view.PlayerView;

public class FControlGameEventHandler extends IGameEventVisitor.Base<Void> {
    private static FControlGameEventHandler mainHandler;

    public static void processEventsAfterInput() {
        if (mainHandler.processEventsSchedule != null) {
            mainHandler.processEventsSchedule.cancel(false); //cancel any currently scheduled processing
        }
        mainHandler.processEventsDelay = 10; //speed up event processing until an event has been raised
        mainHandler.processEventsSchedule = ThreadUtil.delay(mainHandler.processEventsDelay, mainHandler.processEvents);
    }

    private final LocalGameView gameView;
    private final boolean isMainHandler;
    private final ConcurrentLinkedQueue<GameEvent> eventQueue = new ConcurrentLinkedQueue<GameEvent>();

    public FControlGameEventHandler(final LocalGameView gameView0, final boolean isMainHandler0) {
        gameView = gameView0;
        isMainHandler = isMainHandler0;
        if (isMainHandler) {
            mainHandler = this;
        }
        processEvents.run(); //start event processing loop
    }

    private ScheduledFuture<?> processEventsSchedule;
    private int processEventsDelay = 100;
    private final Runnable processEvents = new Runnable() {
        @Override
        public void run() {
            if (!eventQueue.isEmpty()) {
                processEventsDelay = 100; //reset to 100 once an event has been raised
                gameView.updateViews();
    
                GameEvent ev;
                while ((ev = eventQueue.poll()) != null) {
                    ev.visit(FControlGameEventHandler.this);
                }
            }
            processEventsSchedule = ThreadUtil.delay(processEventsDelay, this); //only process events 10 times per second by default to improve performance
        }
    };

    @Subscribe
    public void receiveGameEvent(final GameEvent ev) {
        eventQueue.add(ev);
    }

    private final AtomicBoolean phaseUpdPlanned = new AtomicBoolean(false);
    @Override
    public Void visit(final GameEventTurnPhase ev) {
        if (!isMainHandler || phaseUpdPlanned.getAndSet(true)) { return null; }

        FThreads.invokeInEdtNowOrLater(gameView.getGui(), new Runnable() {
            @Override
            public void run() {
                phaseUpdPlanned.set(false);
                MatchUtil.getController().updatePhase();
            }
        });
        return null;
    }

    /* (non-Javadoc)
     * @see forge.game.event.IGameEventVisitor.Base#visit(forge.game.event.GameEventPlayerPriority)
     */
    private final AtomicBoolean combatUpdPlanned = new AtomicBoolean(false);
    @Override
    public Void visit(GameEventPlayerPriority event) {
        updateCombat();
        return null;
    }

    private void updateCombat() {
        if (!isMainHandler || combatUpdPlanned.getAndSet(true)) { return; }

        FThreads.invokeInEdtNowOrLater(gameView.getGui(), new Runnable() {
            @Override
            public void run() {
                combatUpdPlanned.set(false);
                MatchUtil.getController().showCombat(gameView.getCombat());
            }
        });
    }

    private final AtomicBoolean turnUpdPlanned = new AtomicBoolean(false);
    @Override
    public Void visit(final GameEventTurnBegan event) {
        if (!isMainHandler) { return null; }

        if (FModel.getPreferences().getPrefBoolean(FPref.UI_STACK_CREATURES) && event.turnOwner != null) {
            // anything except stack will get here
            updateZone(Pair.of(gameView.getPlayerView(event.turnOwner), ZoneType.Battlefield));
        }
        
        if (turnUpdPlanned.getAndSet(true)) { return null; }

        FThreads.invokeInEdtNowOrLater(gameView.getGui(), new Runnable() {
            @Override
            public void run() {
                turnUpdPlanned.set(false);
                MatchUtil.getController().updateTurn(gameView.getPlayerView(event.turnOwner));
            }
        });
        return null;
    }

    @Override
    public Void visit(GameEventAnteCardsSelected ev) {
        if (!isMainHandler) { return null; }

        final List<CardView> options = Lists.newArrayList();
        for (final Entry<Player, Card> kv : ev.cards.entries()) {
            final CardView fakeCard = new CardView(-1); //use fake card so real cards appear with proper formatting
            fakeCard.getOriginal().setName("  -- From " + Lang.getPossesive(kv.getKey().getName()) + " deck --");
            options.add(fakeCard);
            options.add(gameView.getCardView(kv.getValue()));
        }
        SGuiChoose.reveal(gameView.getGui(), "These cards were chosen to ante", options);
        return null;
    }

    @Override
    public Void visit(GameEventPlayerControl ev) {
        if (ev.player.getGame().isGameOver()) {
            return null;
        }

        FThreads.invokeInEdtNowOrLater(gameView.getGui(), new Runnable() {
            @Override
            public void run() {
                MatchUtil.getController().updatePlayerControl();
            }
        });
        return null;
    }

    private final Runnable unlockGameThreadOnGameOver = new Runnable() {
        @Override
        public void run() {
            gameView.getInputQueue().onGameOver(true); // this will unlock any game threads waiting for inputs to complete
        }
    };

    @Override
    public Void visit(GameEventGameOutcome ev) {
        FThreads.invokeInEdtNowOrLater(gameView.getGui(), unlockGameThreadOnGameOver);
        return null;
    }

    @Override
    public Void visit(GameEventGameFinished ev) {
        FThreads.invokeInEdtNowOrLater(gameView.getGui(), new Runnable() {
            @Override
            public void run() {
                PlayerView localPlayer = gameView.getLocalPlayerView();
                InputBase.cancelAwaitNextInput(); //ensure "Waiting for opponent..." doesn't appear behind WinLo
                MatchUtil.getController().showPromptMessage(localPlayer, ""); //clear prompt behind WinLose overlay
                ButtonUtil.update(localPlayer, "", "", false, false, false);
                if (isMainHandler) { 
                    MatchUtil.getController().finishGame();
                    gameView.updateAchievements();
                    mainHandler = null; //this can be reset after game is finished
                }
            }
        });
        return null;
    }

    private final AtomicBoolean stackUpdPlanned = new AtomicBoolean(false);
    private final Runnable updStack = new Runnable() {
        @Override
        public void run() {
            stackUpdPlanned.set(false);
            MatchUtil.getController().updateStack();
        }
    };

    @Override
    public Void visit(GameEventSpellAbilityCast event) {
        if (!isMainHandler) { return null; }

        if (!stackUpdPlanned.getAndSet(true)) {
            FThreads.invokeInEdtNowOrLater(gameView.getGui(), updStack);
        }
        return null;
    }
    @Override
    public Void visit(GameEventSpellResolved event) {
        if (!isMainHandler) { return null; }

        if (!stackUpdPlanned.getAndSet(true)) {
            FThreads.invokeInEdtNowOrLater(gameView.getGui(), updStack);
        }
        return null;
    }
    @Override
    public Void visit(GameEventSpellRemovedFromStack event) {
        if (!isMainHandler) { return null; }

        if (!stackUpdPlanned.getAndSet(true)) {
            FThreads.invokeInEdtNowOrLater(gameView.getGui(), updStack);
        }
        return null;
    }

    private final List<Pair<PlayerView, ZoneType>> zonesToUpdate = new Vector<Pair<PlayerView,ZoneType>>();
    private final Runnable updZones = new Runnable() {
        @Override
        public void run() {
            synchronized (zonesToUpdate) {
                MatchUtil.getController().updateZones(zonesToUpdate);
                zonesToUpdate.clear();
            }
        }
    };

    @Override
    public Void visit(GameEventZone event) {
        if (!isMainHandler) { return null; }

        if (event.player != null) {
            // anything except stack will get here
            updateZone(Pair.of(gameView.getPlayerView(event.player), event.zoneType));
        }
        return null;
    }

    @Override
    public Void visit(GameEventCardAttachment event) {
        if (!isMainHandler) { return null; }

        final Game game = event.equipment.getGame();
        final PlayerZone zEq = (PlayerZone)game.getZoneOf(event.equipment);
        if (event.oldEntiy instanceof Card) {
            updateZone(game.getZoneOf((Card)event.oldEntiy));
        }
        if (event.newTarget instanceof Card) {
            updateZone(game.getZoneOf((Card)event.newTarget));
        }
        return updateZone(zEq);
    }

    private Void updateZone(final Zone z) {
        return updateZone(Pair.of(gameView.getPlayerView(z.getPlayer()), z.getZoneType()));
    }

    private Void updateZone(final Pair<PlayerView, ZoneType> kv) {
        boolean needUpdate = false;
        synchronized (zonesToUpdate) {
            needUpdate = zonesToUpdate.isEmpty();
            if (!zonesToUpdate.contains(kv)) {
                zonesToUpdate.add(kv);
            }
        }
        if (needUpdate) {
            FThreads.invokeInEdtNowOrLater(gameView.getGui(), updZones);
        }
        return null;
    }

    private final Set<CardView> cardsToUpdate = Sets.newHashSet();
    private final Runnable updCards = new Runnable() {
        @Override
        public void run() {
            synchronized (cardsToUpdate) {
                MatchUtil.updateCards(cardsToUpdate);
                cardsToUpdate.clear();
            }
        }
    };

    @Override
    public Void visit(final GameEventCardTapped event) {
        if (!isMainHandler) { return null; }

        return updateSingleCard(gameView.getCardView(event.card));
    }
    
    @Override
    public Void visit(final GameEventCardPhased event) {
        if (!isMainHandler) { return null; }

        return updateSingleCard(gameView.getCardView(event.card));
    }

    @Override
    public Void visit(final GameEventCardDamaged event) {
        if (!isMainHandler) { return null; }

        return updateSingleCard(gameView.getCardView(event.card));
    }

    @Override
    public Void visit(final GameEventCardCounters event) {
        if (!isMainHandler) { return null; }

        return updateSingleCard(gameView.getCardView(event.card));
    }

    @Override
    public Void visit(final GameEventBlockersDeclared event) { // This is to draw icons on blockers declared by AI
        if (!isMainHandler) { return null; }

        for (MapOfLists<Card, Card> kv : event.blockers.values()) {
            for (Collection<Card> blockers : kv.values()) {
                updateManyCards(gameView.getCardViews(blockers));
            }
        }
        return super.visit(event);
    }

    @Override
    public Void visit(GameEventAttackersDeclared event) {
        if (!isMainHandler) { return null; }

        // Skip redraw for GUI player?
        if (event.player.getLobbyPlayer() == GamePlayerUtil.getGuiPlayer()) {
            return null;
        }

        // Update all attackers.
        // Although they might have been updated when they were tapped, there could be someone with vigilance, not redrawn yet.
        updateManyCards(gameView.getCardViews(event.attackersMap.values()));

        return super.visit(event);
    }

    @Override
    public Void visit(GameEventCombatChanged event) {
        gameView.refreshCombat();
        updateCombat();
        return null;
    }

    @Override
    public Void visit(GameEventCombatEnded event) {
        if (!isMainHandler) { return null; }

        gameView.refreshCombat();
        updateCombat();

        // This should remove sword/shield icons from combatants by the time game moves to M2
        updateManyCards(gameView.getCardViews(event.attackers));
        updateManyCards(gameView.getCardViews(event.blockers));
        return null;
    }

    private Void updateSingleCard(final CardView c) {
        boolean needUpdate = false;
        synchronized (cardsToUpdate) {
            needUpdate = cardsToUpdate.isEmpty();
            if (!cardsToUpdate.contains(c)) {
                cardsToUpdate.add(c);
            }
        }
        if (needUpdate) {
            FThreads.invokeInEdtNowOrLater(gameView.getGui(), updCards);
        }
        return null;
    }

    private Void updateManyCards(final Iterable<CardView> cc) {
        boolean needUpdate = false;
        synchronized (cardsToUpdate) {
            needUpdate = cardsToUpdate.isEmpty();
            Iterables.addAll(cardsToUpdate, cc);
        }
        if (needUpdate) {
            FThreads.invokeInEdtNowOrLater(gameView.getGui(), updCards);
        }
        return null;
    }

    @Override
    public Void visit(GameEventCardChangeZone event) {
        if (!isMainHandler) { return null; }

        if (event.from != null) {
            updateZone(event.from);
        }
        if (event.to != null) {
            updateZone(event.to);
        }
        return null;
    }

    /* (non-Javadoc)
     * @see forge.game.event.IGameEventVisitor.Base#visit(forge.game.event.GameEventCardStatsChanged)
     */
    @Override
    public Void visit(GameEventCardStatsChanged event) {
        if (!isMainHandler) { return null; }

        final Iterable<CardView> cardViews = gameView.getCardViews(event.cards);
        MatchUtil.getController().refreshCardDetails(cardViews);
        return updateManyCards(cardViews);
    }

    @Override
    public Void visit(GameEventPlayerStatsChanged event) {
        if (!isMainHandler) { return null; }

        for (final Player p : event.players) {
            MatchUtil.getController().refreshCardDetails(gameView.getCardViews(p.getAllCards()));
        }
        return null;
    }

    @Override
    public Void visit(GameEventShuffle event) {
        if (!isMainHandler) { return null; }

        updateZone(event.player.getZone(ZoneType.Library));
        return null;
    }

    // Update manapool
    private final List<Player> manaPoolUpdate = new Vector<Player>();
    private final Runnable updManaPool = new Runnable() {
        @Override public void run() {
            synchronized (manaPoolUpdate) {
                MatchUtil.getController().updateManaPool(gameView.getPlayerViews(manaPoolUpdate));
                manaPoolUpdate.clear();
            }
        }
    };

    @Override
    public Void visit(GameEventManaPool event) {
        if (!isMainHandler) { return null; }

        boolean invokeUpdate = false;
        synchronized (manaPoolUpdate) {
            if (!manaPoolUpdate.contains(event.player)) {
                invokeUpdate = manaPoolUpdate.isEmpty();
                manaPoolUpdate.add(event.player);
            }
        }
        if (invokeUpdate) {
            FThreads.invokeInEdtNowOrLater(gameView.getGui(), updManaPool);
        }
        return null;
    }

    // Update lives counters
    private final List<Player> livesUpdate = new Vector<Player>();
    private final Runnable updLives = new Runnable() {
        @Override public void run() {
            synchronized (livesUpdate) {
                MatchUtil.getController().updateLives(gameView.getPlayerViews(livesUpdate));
                livesUpdate.clear();
            }
        }
    };
    @Override
    public Void visit(GameEventPlayerLivesChanged event) {
        if (!isMainHandler) { return null; }

        boolean invokeUpdate = false;
        synchronized (livesUpdate) {
            if (!livesUpdate.contains(event.player)) {
                invokeUpdate = livesUpdate.isEmpty();
                livesUpdate.add(event.player);
            }
        }
        if (invokeUpdate) {
            FThreads.invokeInEdtNowOrLater(gameView.getGui(), updLives);
        }
        return null;
    }

    @Override
    public Void visit(GameEventPlayerPoisoned event) {
        if (!isMainHandler) { return null; }

        boolean invokeUpdate = false;
        synchronized (livesUpdate) {
            if (!livesUpdate.contains(event.receiver)) {
                invokeUpdate = livesUpdate.isEmpty();
                livesUpdate.add(event.receiver);
            }
        }
        if (invokeUpdate) {
            FThreads.invokeInEdtNowOrLater(gameView.getGui(), updLives);
        }
        return null;
    }
}