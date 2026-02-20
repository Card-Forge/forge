package forge.gui.control;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.Subscribe;
import forge.game.card.CardView;
import forge.game.event.*;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiGame;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.player.PlayerControllerHuman;
import forge.player.PlayerZoneUpdate;
import forge.player.PlayerZoneUpdates;
import forge.sound.SoundSystem;
import forge.util.Lang;

import java.util.*;
import java.util.Map.Entry;

public class FControlGameEventHandler extends IGameEventVisitor.Base<Void> {
    private final PlayerControllerHuman humanController;
    private final IGuiGame matchController;
    private final Set<CardView> cardsUpdate = new HashSet<>();
    private final Set<CardView> cardsRefreshDetails = new HashSet<>();
    private final Set<PlayerView> livesUpdate = new HashSet<>();
    private final Set<PlayerView> shardsUpdate = new HashSet<>();
    private final Set<PlayerView> manaPoolUpdate = new HashSet<>();
    private final PlayerZoneUpdates zonesUpdate = new PlayerZoneUpdates();
    private final Map<PlayerView, Object> playersWithValidTargets = Maps.newHashMap();

    private boolean processEventsQueued, needPhaseUpdate, needCombatUpdate, needStackUpdate, needPlayerControlUpdate, refreshFieldUpdate, showExileUpdate;
    private boolean gameOver, gameFinished;
    private boolean needSaveState = false;
    private PlayerView turnUpdate, activatingPlayer;

    public FControlGameEventHandler(final PlayerControllerHuman humanController0) {
        humanController = humanController0;
        matchController = humanController.getGui();
    }

    public FControlGameEventHandler(final IGuiGame gui) {
        humanController = null;
        matchController = gui;
    }

    private final Runnable processEvents = new Runnable() {
        @Override
        public void run() {
            processEventsQueued = false;

            synchronized (cardsUpdate) {
                if (!cardsUpdate.isEmpty()) {
                    matchController.updateCards(cardsUpdate);
                    cardsUpdate.clear();
                }
            }
            synchronized (cardsRefreshDetails) {
                if (!cardsRefreshDetails.isEmpty()) {
                    matchController.refreshCardDetails(cardsRefreshDetails);
                    cardsRefreshDetails.clear();
                }
            }
            synchronized (livesUpdate) {
                if (!livesUpdate.isEmpty()) {
                    matchController.updateLives(livesUpdate);
                    livesUpdate.clear();
                }
            }
            synchronized (shardsUpdate) {
                if (!shardsUpdate.isEmpty()) {
                    matchController.updateShards(shardsUpdate);
                    shardsUpdate.clear();
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
                if (needSaveState) {
                    needSaveState = false;
                    matchController.updatePhase(true);
                } else {
                    matchController.updatePhase(false);
                }
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
                    // Copy to prevent concurrency issues
                    matchController.updateZones(new PlayerZoneUpdates(zonesUpdate));
                    zonesUpdate.clear();
                }
            }
            if (refreshFieldUpdate) {
                refreshFieldUpdate = false;
                matchController.refreshField();
            }
            if (showExileUpdate) {
                showExileUpdate = false;
                matchController.openZones(activatingPlayer, Collections.singleton(ZoneType.Exile), playersWithValidTargets, false);
                activatingPlayer = null;
                playersWithValidTargets.clear();
            }
            if (gameOver) {
                gameOver = false;
                if (humanController != null) {
                    humanController.getInputQueue().onGameOver(true); // this will unlock any game threads waiting for inputs to complete
                }
            }
            if (gameFinished) {
                gameFinished = false;
                if (humanController != null) {
                    final PlayerView localPlayer = humanController.getLocalPlayerView();
                    humanController.cancelAwaitNextInput(); //ensure "Waiting for opponent..." doesn't appear behind WinLo
                    matchController.showPromptMessage(localPlayer, ""); //clear prompt behind WinLose overlay
                    matchController.updateButtons(localPlayer, "", "", false, false, false);
                    humanController.updateAchievements();
                }
                matchController.finishGame();
            }
        }
    };

    @Subscribe
    public void receiveGameEvent(final GameEvent ev) {
        ev.visit(this);
        SoundSystem.instance.receiveEvent(ev);
    }

    private Void processEvent() {
        if (processEventsQueued) { return null; } //avoid queuing event processing multiple times
        processEventsQueued = true;
        GuiBase.getInterface().invokeInEdtLater(processEvents);
        return null;
    }

    private Void processCard(final CardView cardView, final Set<CardView> list) {
        synchronized (list) {
            list.add(cardView);
        }
        return processEvent();
    }
    private Void processCards(final Collection<CardView> cards, final Set<CardView> list) {
        if (cards.isEmpty()) { return null; }

        synchronized (list) {
            list.addAll(cards);
        }
        return processEvent();
    }
    private Void processPlayer(final PlayerView playerView, final Set<PlayerView> list) {
        synchronized (list) {
            list.add(playerView);
        }
        return processEvent();
    }
    private Void updateZone(final PlayerView p, final ZoneType z) {
        if (p == null || z == null) { return null; }

        synchronized (zonesUpdate) {
            zonesUpdate.add(new PlayerZoneUpdate(p, z));
        }
        return processEvent();
    }

    @Override
    public Void visit(final GameEventTurnPhase ev) {
        needPhaseUpdate = true;
        needSaveState = !"dev".equals(ev.phaseDesc());

        PlayerView ap = ev.playerTurn();
        boolean refreshField = ap.getCards(ZoneType.Battlefield) != null &&
                (ap.getCards(ZoneType.Battlefield).anyMatch(CardView::isToken)
                || (FModel.getPreferences().getPrefBoolean(FPref.UI_STACK_CREATURES) && ap.getCards(ZoneType.Battlefield).anyMatch(c -> c.getCurrentState().isCreature())));
        if (refreshField) {
            updateZone(ap, ZoneType.Battlefield);
        }
        return processEvent();
    }

    @Override
    public Void visit(final GameEventPlayerPriority event) {
        needCombatUpdate = true;
        matchController.updateDependencies();
        return processEvent();
    }

    @Override
    public Void visit(final GameEventTurnBegan event) {
        turnUpdate = event.turnOwner();
        processPlayer(event.turnOwner(), livesUpdate);
        return processEvent();
    }

    @Override
    public Void visit(final GameEventAnteCardsSelected ev) {
        if (humanController == null) { return null; }
        final List<CardView> options = Lists.newArrayList();
        for (final Entry<PlayerView, CardView> kv : ev.cards().entries()) {
            //use fake card so real cards appear with proper formatting
            final CardView fakeCard = new CardView(-1, null, "  -- From " + Lang.getInstance().getPossesive(kv.getKey().getName()) + " deck --");
            options.add(fakeCard);
            options.add(kv.getValue());
        }
        humanController.getGui().reveal("These cards were chosen to ante", options);
        return null;
    }

    @Override
    public Void visit(final GameEventPlayerControl ev) {
        if (humanController != null) {
            final PlayerControllerHuman newController = ev.newControllerIsHuman() ? humanController : null;
            matchController.setGameController(ev.player(), newController);
        }

        needPlayerControlUpdate = true;
        return processEvent();
    }

    @Override
    public Void visit(final GameEventGameOutcome ev) {
        gameOver = true;
        return processEvent();
    }

    @Override
    public Void visit(final GameEventGameFinished ev) {
        gameFinished = true;
        return processEvent();
    }

    @Override
    public Void visit(final GameEventSpellAbilityCast event) {
        needStackUpdate = true;
        if(GuiBase.getInterface().isLibgdxPort()) {
            return processEvent(); //mobile port don't have notify stack addition like the desktop
        } else {
            processEvent();

            final Runnable notifyStackAddition = matchController.notifyStackAddition(event);
            GuiBase.getInterface().invokeInEdtLater(notifyStackAddition);
        }
        return null;
    }

    @Override
    public Void visit(final GameEventSpellResolved event) {
        needStackUpdate = true;
        return processEvent();
    }

    @Override
    public Void visit(final GameEventSpellRemovedFromStack event) {
        needStackUpdate = true;
        if(GuiBase.getInterface().isLibgdxPort()) {
            return processEvent(); //mobile port don't have notify stack addition like the desktop
        } else {
            processEvent();

            final Runnable notifyStackAddition = matchController.notifyStackRemoval(event);
            GuiBase.getInterface().invokeInEdtLater(notifyStackAddition);
        }
        return null;
    }

    @Override
    public Void visit(final GameEventZone event) {
        if (event.player() != null) {
            // anything except stack will get here
            updateZone(event.player(), event.zoneType());
            return processEvent();
        }
        return null;
    }

    @Override
    public Void visit(final GameEventCardAttachment event) {
        updateZone(event.equipment().getController(), event.equipment().getZone());
        if (event.oldEntity() instanceof CardView oldCard) {
            updateZone(oldCard.getController(), oldCard.getZone());
        }
        if (event.newTarget() instanceof CardView newCard) {
            updateZone(newCard.getController(), newCard.getZone());
        }
        return processEvent();
    }

    @Override
    public Void visit(final GameEventCardTapped event) {
        refreshFieldUpdate = true; //update all players field when event un/tapped
        processCard(event.card(), cardsUpdate);
        return processEvent();
    }

    @Override
    public Void visit(final GameEventCardPhased event) {
        processCard(event.card(), cardsUpdate);
        return processEvent();
    }

    @Override
    public Void visit(final GameEventCardDamaged event) {
        processCard(event.card(), cardsUpdate);
        return processEvent();
    }

    @Override
    public Void visit(final GameEventCardCounters event) {
        processCard(event.card(), cardsUpdate);
        return processEvent();
    }

    @Override
    public Void visit(final GameEventBlockersDeclared event) {
        final Set<CardView> cards = new HashSet<>();

        for (final Multimap<CardView, CardView> kv : event.blockers().values()) {
            cards.addAll(kv.values());
        }
        return processCards(cards, cardsUpdate);
    }

    @Override
    public Void visit(final GameEventAttackersDeclared event) {
        return processCards(event.attackersMap().values(), cardsUpdate);
    }

    @Override
    public Void visit(final GameEventCombatChanged event) {
        needCombatUpdate = true;
        return processEvent();
    }

    @Override
    public Void visit(final GameEventCombatEnded event) {
        needCombatUpdate = true;

        // This should remove sword/shield icons from combatants by the time game moves to M2
        processCards(event.attackers(), cardsUpdate);
        return processCards(event.blockers(), cardsUpdate);
    }

    @Override
    public Void visit(final GameEventCombatUpdate event) {
        if (!GuiBase.isNetworkplay(matchController))
            return null; //not needed if single player only...

        final List<CardView> cards = new ArrayList<>();
        cards.addAll(event.attackers());
        cards.addAll(event.blockers());

        refreshFieldUpdate = true;

        processCards(cards, cardsRefreshDetails);
        return processCards(cards, cardsUpdate);
    }

    @Override
    public Void visit(final GameEventCardChangeZone event) {
        if (GuiBase.getInterface().isLibgdxPort()) {
            if (event.from() != null) {
                updateZone(event.from().player(), event.from().zoneType());
            }
            if (event.to() != null) {
                updateZone(event.to().player(), event.to().zoneType());
            }
        }
        return processEvent();
    }

    @Override
    public Void visit(final GameEventCardStatsChanged event) {
        refreshFieldUpdate = true;
        processCards(event.cards(), cardsRefreshDetails);
        return processCards(event.cards(), cardsUpdate);
    }

    @Override
    public Void visit(final GameEventCardForetold event) {
        showExileUpdate = true;
        activatingPlayer = event.activatingPlayer();
        playersWithValidTargets.put(activatingPlayer, null);
        return processEvent();
    }

    @Override
    public Void visit(final GameEventCardPlotted event) {
        showExileUpdate = true;
        activatingPlayer = event.activatingPlayer();
        playersWithValidTargets.put(activatingPlayer, null);
        return processEvent();
    }

    @Override
    public Void visit(final GameEventPlayerStatsChanged event) {
        for (final PlayerView p : event.players()) {
            processPlayer(p, livesUpdate);
        }

        return processCards(event.allCards(), cardsRefreshDetails);
    }

    public Void visit(final GameEventLandPlayed event) {
        processPlayer(event.player(), livesUpdate);
        matchController.handleLandPlayed(event.land());
        return processCard(event.land(), cardsRefreshDetails);
    }

    @Override
    public Void visit(final GameEventCardRegenerated event) {
        refreshFieldUpdate = true;
        processCards(event.cards(), cardsRefreshDetails);
        return processCards(event.cards(), cardsUpdate);
    }

    @Override
    public Void visit(final GameEventShuffle event) {
        if (GuiBase.getInterface().isLibgdxPort()) {
            return updateZone(event.player(), ZoneType.Library);
        } else {
            return processEvent();
        }
    }

    @Override
    public Void visit(final GameEventDayTimeChanged event) {
        matchController.updateDayTime(event.daytime() ? "Day" : "Night");
        return processEvent();
    }

    @Override
    public Void visit(GameEventSprocketUpdate event) {
        updateZone(event.contraption().getController(), event.contraption().getZone());
        return processEvent();
    }

    @Override
    public Void visit(final GameEventManaPool event) {
        return processPlayer(event.player(), manaPoolUpdate);
    }

    @Override
    public Void visit(final GameEventPlayerLivesChanged event) {
        return processPlayer(event.player(), livesUpdate);
    }

    @Override
    public Void visit(final GameEventPlayerShardsChanged event) {
        return processPlayer(event.player(), shardsUpdate);
    }

    @Override
    public Void visit(GameEventManaBurn event) {
        return processPlayer(event.player(), livesUpdate);
    }

    @Override
    public Void visit(final GameEventPlayerPoisoned event) {
        return processPlayer(event.receiver(), livesUpdate);
    }

    @Override
    public Void visit(final GameEventPlayerRadiation event) {
        return processPlayer(event.receiver(), livesUpdate);
    }

    @Override
    public Void visit(final GameEventPlayerDamaged event) {
        return processEvent();
    }

    @Override
    public Void visit(final GameEventPlayerCounters event) {
        return processPlayer(event.receiver(), livesUpdate);
    }
}
