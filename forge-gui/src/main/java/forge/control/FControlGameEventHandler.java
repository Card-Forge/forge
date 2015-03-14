package forge.control;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;
import com.google.common.eventbus.Subscribe;

import forge.GuiBase;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardView;
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
import forge.game.player.PlayerView;
import forge.game.zone.PlayerZone;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.interfaces.IGuiGame;
import forge.model.FModel;
import forge.player.PlayerControllerHuman;
import forge.properties.ForgePreferences.FPref;
import forge.util.Lang;
import forge.util.maps.MapOfLists;

public class FControlGameEventHandler extends IGameEventVisitor.Base<Void> {
    private final PlayerControllerHuman humanController;
    private final IGuiGame matchController;
    private final Set<CardView> cardsUpdate = new HashSet<CardView>();
    private final Set<CardView> cardsRefreshDetails = new HashSet<CardView>();
    private final Set<PlayerView> livesUpdate = new HashSet<PlayerView>();
    private final Set<PlayerView> manaPoolUpdate = new HashSet<PlayerView>();
    private final List<Pair<PlayerView, ZoneType>> zonesUpdate = new ArrayList<Pair<PlayerView, ZoneType>>();

    private boolean processEventsQueued, needPhaseUpdate, needCombatUpdate, needStackUpdate, needPlayerControlUpdate;
    private boolean gameOver, gameFinished;
    private PlayerView turnUpdate;

    public FControlGameEventHandler(final PlayerControllerHuman humanController0) {
        humanController = humanController0;
        matchController = humanController.getGui();
    }

    private final Runnable processEvents = new Runnable() {
        @Override
        public void run() {
            processEventsQueued = false;

            synchronized (cardsUpdate) {
                if (!cardsUpdate.isEmpty()) {
                    for (final CardView c : cardsUpdate) {
                        matchController.updateSingleCard(c);
                    }
                    cardsUpdate.clear();
                }
            }
            synchronized (cardsRefreshDetails) {
                if (!cardsRefreshDetails.isEmpty()) {
                    matchController.updateCards(cardsRefreshDetails);
                    cardsRefreshDetails.clear();
                }
            }
            synchronized (livesUpdate) {
                if (!livesUpdate.isEmpty()) {
                    matchController.updateLives(livesUpdate);
                    livesUpdate.clear();
                }
            }
            synchronized (manaPoolUpdate) {
                if (!manaPoolUpdate.isEmpty()) {
                    matchController.updateManaPool(manaPoolUpdate);
                    manaPoolUpdate.clear();
                }
            }
            if (turnUpdate != null) {
                matchController.updateTurn(turnUpdate);
                turnUpdate = null;
            }
            if (needPhaseUpdate) {
                needPhaseUpdate = false;
                matchController.updatePhase();
            }
            if (needCombatUpdate) {
                needCombatUpdate = false;
                matchController.showCombat();
            }
            if (needStackUpdate) {
                needStackUpdate = false;
                matchController.updateStack();
            }
            if (needPlayerControlUpdate) {
                needPlayerControlUpdate = false;
                matchController.updatePlayerControl();
            }
            synchronized (zonesUpdate) {
                if (!zonesUpdate.isEmpty()) {
                    matchController.updateZones(zonesUpdate);
                    zonesUpdate.clear();
                }
            }
            if (gameOver) {
                gameOver = false;
                humanController.getInputQueue().onGameOver(true); // this will unlock any game threads waiting for inputs to complete
            }
            if (gameFinished) {
                gameFinished = false;
                final PlayerView localPlayer = humanController.getLocalPlayerView();
                humanController.cancelAwaitNextInput(); //ensure "Waiting for opponent..." doesn't appear behind WinLo
                matchController.showPromptMessage(localPlayer, ""); //clear prompt behind WinLose overlay
                matchController.updateButtons(localPlayer, "", "", false, false, false);
                matchController.finishGame();
                humanController.updateAchievements();
            }
        }
    };

    @Subscribe
    public void receiveGameEvent(final GameEvent ev) {
        ev.visit(this);
    }

    private Void processEvent() {
        if (processEventsQueued) { return null; } //avoid queuing event processing multiple times
        processEventsQueued = true;
        GuiBase.getInterface().invokeInEdtLater(processEvents);
        return null;
    }

    private Void processCard(Card card, Set<CardView> list) {
        synchronized (list) {
            list.add(card.getView());
        }
        return processEvent();
    }
    private Void processCards(Collection<Card> cards, Set<CardView> list) {
        if (cards.isEmpty()) { return null; }

        synchronized (list) {
            for (Card c : cards) {
                list.add(c.getView());
            }
        }
        return processEvent();
    }
    private Void processPlayer(Player player, Set<PlayerView> list) {
        synchronized (list) {
            list.add(player.getView());
        }
        return processEvent();
    }
    private Void updateZone(final Zone z) {
        if (z == null) { return null; }

        return updateZone(z.getPlayer(), z.getZoneType());
    }
    private Void updateZone(Player p, ZoneType z) {
        if (p == null || z == null) { return null; }

        synchronized (zonesUpdate) {
            for (Pair<PlayerView, ZoneType> pair : zonesUpdate) {
                if (pair.getLeft() == p.getView() && pair.getRight() == z) {
                    return null; //avoid adding the same pair multiple times
                }
            }
            zonesUpdate.add(Pair.of(p.getView(), z));
        }
        return processEvent();
    }

    @Override
    public Void visit(final GameEventTurnPhase ev) {
        needPhaseUpdate = true;
        return processEvent();
    }

    @Override
    public Void visit(GameEventPlayerPriority event) {
        needCombatUpdate = true;
        return processEvent();
    }

    @Override
    public Void visit(final GameEventTurnBegan event) {
        turnUpdate = event.turnOwner.getView();
        if (FModel.getPreferences().getPrefBoolean(FPref.UI_STACK_CREATURES) && event.turnOwner != null) {
            // anything except stack will get here
            updateZone(event.turnOwner, ZoneType.Battlefield);
        }
        return processEvent();
    }

    @Override
    public Void visit(GameEventAnteCardsSelected ev) {
        final List<CardView> options = Lists.newArrayList();
        for (final Entry<Player, Card> kv : ev.cards.entries()) {
            //use fake card so real cards appear with proper formatting
            final CardView fakeCard = new CardView(-1, null, "  -- From " + Lang.getPossesive(kv.getKey().getName()) + " deck --");
            options.add(fakeCard);
            options.add(kv.getValue().getView());
        }
        humanController.getGui().reveal("These cards were chosen to ante", options);
        return null;
    }

    @Override
    public Void visit(GameEventPlayerControl ev) {
        if (ev.player.getGame().isGameOver()) {
            return null;
        }
        if (ev.newController instanceof PlayerControllerHuman) {
            matchController.setGameController(PlayerView.get(ev.player), (PlayerControllerHuman) ev.newController);
        }
        needPlayerControlUpdate = true;
        return processEvent();
    }

    @Override
    public Void visit(GameEventGameOutcome ev) {
        gameOver = true;
        return processEvent();
    }

    @Override
    public Void visit(GameEventGameFinished ev) {
        gameFinished = true;
        return processEvent();
    }

    @Override
    public Void visit(GameEventSpellAbilityCast event) {
        needStackUpdate = true;
        return processEvent();
    }

    @Override
    public Void visit(GameEventSpellResolved event) {
        needStackUpdate = true;
        return processEvent();
    }

    @Override
    public Void visit(GameEventSpellRemovedFromStack event) {
        needStackUpdate = true;
        return processEvent();
    }

    @Override
    public Void visit(GameEventZone event) {
        if (event.player != null) {
            // anything except stack will get here
            updateZone(event.player, event.zoneType);
            return processEvent();
        }
        return null;
    }

    @Override
    public Void visit(GameEventCardAttachment event) {
        final Game game = event.equipment.getGame();
        final PlayerZone zEq = (PlayerZone)game.getZoneOf(event.equipment);
        if (event.oldEntiy instanceof Card) {
            updateZone(game.getZoneOf((Card)event.oldEntiy));
        }
        if (event.newTarget instanceof Card) {
            updateZone(game.getZoneOf((Card)event.newTarget));
        }
        updateZone(zEq);
        return processEvent();
    }

    @Override
    public Void visit(final GameEventCardTapped event) {
        processCard(event.card, cardsUpdate);
        return processEvent();
    }
    
    @Override
    public Void visit(final GameEventCardPhased event) {
        processCard(event.card, cardsUpdate);
        return processEvent();
    }

    @Override
    public Void visit(final GameEventCardDamaged event) {
        processCard(event.card, cardsUpdate);
        return processEvent();
    }

    @Override
    public Void visit(final GameEventCardCounters event) {
        processCard(event.card, cardsUpdate);
        return processEvent();
    }

    @Override
    public Void visit(final GameEventBlockersDeclared event) {
        HashSet<Card> cards = new HashSet<Card>();
        for (MapOfLists<Card, Card> kv : event.blockers.values()) {
            for (Collection<Card> blockers : kv.values()) {
                cards.addAll(blockers);
            }
        }
        return processCards(cards, cardsUpdate);
    }

    @Override
    public Void visit(GameEventAttackersDeclared event) {
        return processCards(event.attackersMap.values(), cardsUpdate);
    }

    @Override
    public Void visit(GameEventCombatChanged event) {
        needCombatUpdate = true;
        return processEvent();
    }

    @Override
    public Void visit(GameEventCombatEnded event) {
        needCombatUpdate = true;

        // This should remove sword/shield icons from combatants by the time game moves to M2
        processCards(event.attackers, cardsUpdate);
        return processCards(event.blockers, cardsUpdate);
    }

    @Override
    public Void visit(GameEventCardChangeZone event) {
        updateZone(event.from);
        return updateZone(event.to);
    }

    @Override
    public Void visit(GameEventCardStatsChanged event) {
        processCards(event.cards, cardsRefreshDetails);
        return processCards(event.cards, cardsUpdate);
    }

    @Override
    public Void visit(GameEventPlayerStatsChanged event) {
        CardCollection cards = new CardCollection();
        for (final Player p : event.players) {
            cards.addAll(p.getAllCards());
        }
        return processCards(cards, cardsRefreshDetails);
    }

    @Override
    public Void visit(GameEventShuffle event) {
        return updateZone(event.player.getZone(ZoneType.Library));
    }

    @Override
    public Void visit(GameEventManaPool event) {
        return processPlayer(event.player, manaPoolUpdate);
    }

    @Override
    public Void visit(GameEventPlayerLivesChanged event) {
        return processPlayer(event.player, livesUpdate);
    }

    @Override
    public Void visit(GameEventPlayerPoisoned event) {
        return processPlayer(event.receiver, livesUpdate);
    }
}