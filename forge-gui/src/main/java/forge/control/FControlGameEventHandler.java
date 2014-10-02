package forge.control;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;
import com.google.common.eventbus.Subscribe;

import forge.GuiBase;
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
import forge.interfaces.IGuiTimer;
import forge.match.IMatchController;
import forge.match.MatchUtil;
import forge.match.input.ButtonUtil;
import forge.match.input.InputBase;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.properties.ForgePreferences.FPref;
import forge.util.Lang;
import forge.util.gui.SGuiChoose;
import forge.util.maps.MapOfLists;
import forge.view.CardView;
import forge.view.LocalGameView;
import forge.view.PlayerView;

public class FControlGameEventHandler extends IGameEventVisitor.Base<Void> {
    private static final String PLAYER_ZONE_DELIM = "|";
    private static final int BASE_TIMER_INTERVAL = 100; //process events 10 times per second by default

    private final LocalGameView gameView;
    private final IGuiTimer processEventsTimer;
    private final HashSet<Card> cardsUpdate = new HashSet<Card>();
    private final HashSet<Card> cardsRefreshDetails = new HashSet<Card>();
    private final HashSet<Player> livesUpdate = new HashSet<Player>();
    private final HashSet<Player> manaPoolUpdate = new HashSet<Player>();
    private final HashSet<String> zonesUpdate = new HashSet<String>();

    private boolean eventReceived, needPhaseUpdate, needCombatUpdate, needStackUpdate, needPlayerControlUpdate;
    private boolean gameOver, gameFinished;
    private Player turnUpdate;

    public FControlGameEventHandler(final LocalGameView gameView0) {
        gameView = gameView0;
        processEventsTimer = GuiBase.getInterface().createGuiTimer(processEvents, BASE_TIMER_INTERVAL);
        processEventsTimer.start(); //start event processing loop
    }

    private final Runnable processEvents = new Runnable() {
        @Override
        public void run() {
            synchronized (processEventsTimer) {
                if (eventReceived) {
                    eventReceived = false;

                    gameView.startUpdate();
                    IMatchController controller = MatchUtil.getController();
                    if (!cardsUpdate.isEmpty()) {
                        MatchUtil.updateCards(gameView.getCardViews(cardsUpdate));
                        cardsUpdate.clear();
                    }
                    if (!cardsRefreshDetails.isEmpty()) {
                        controller.refreshCardDetails(gameView.getCardViews(cardsRefreshDetails));
                        cardsRefreshDetails.clear();
                    }
                    if (!livesUpdate.isEmpty()) {
                        controller.updateLives(gameView.getPlayerViews(livesUpdate));
                        livesUpdate.clear();
                    }
                    if (!manaPoolUpdate.isEmpty()) {
                        controller.updateManaPool(gameView.getPlayerViews(manaPoolUpdate));
                        manaPoolUpdate.clear();
                    }
                    if (!zonesUpdate.isEmpty()) {
                        List<PlayerView> players = gameView.getPlayers();
                        ArrayList<Pair<PlayerView, ZoneType>> zones = new ArrayList<Pair<PlayerView, ZoneType>>();
                        for (String z : zonesUpdate) {
                            int idx = z.indexOf(PLAYER_ZONE_DELIM);
                            zones.add(Pair.of(players.get(Integer.parseInt(z.substring(0, idx))), ZoneType.valueOf(z.substring(idx + 1))));
                        }
                        controller.updateZones(zones);
                        zonesUpdate.clear();
                    }
                    if (turnUpdate != null) {
                        controller.updateTurn(gameView.getPlayerView(turnUpdate));
                        turnUpdate = null;
                    }
                    if (needPhaseUpdate) {
                        needPhaseUpdate = false;
                        controller.updatePhase();
                    }
                    if (needCombatUpdate) {
                        needCombatUpdate = false;
                        gameView.refreshCombat();
                        controller.showCombat(gameView.getCombat());
                    }
                    if (needStackUpdate) {
                        needStackUpdate = false;
                        controller.updateStack();
                    }
                    if (needPlayerControlUpdate) {
                        needPlayerControlUpdate = false;
                        controller.updatePlayerControl();
                    }
                    if (gameOver) {
                        gameOver = false;
                        gameView.getInputQueue().onGameOver(true); // this will unlock any game threads waiting for inputs to complete
                    }
                    if (gameFinished) {
                        gameFinished = false;
                        PlayerView localPlayer = gameView.getLocalPlayerView();
                        InputBase.cancelAwaitNextInput(); //ensure "Waiting for opponent..." doesn't appear behind WinLo
                        controller.showPromptMessage(localPlayer, ""); //clear prompt behind WinLose overlay
                        ButtonUtil.update(localPlayer, "", "", false, false, false);
                        controller.finishGame();
                        gameView.updateAchievements();
                        processEventsTimer.stop();
                    }
                    gameView.endUpdate();
                }
            }
        }
    };

    @Subscribe
    public void receiveGameEvent(final GameEvent ev) {
        synchronized (processEventsTimer) { //ensure multiple events aren't processed at the same time
            eventReceived = true;
            ev.visit(this);
        }
    }

    @Override
    public Void visit(final GameEventTurnPhase ev) {
        needPhaseUpdate = true;
        return null;
    }

    @Override
    public Void visit(GameEventPlayerPriority event) {
        needCombatUpdate = true;
        return null;
    }

    @Override
    public Void visit(final GameEventTurnBegan event) {
        turnUpdate = event.turnOwner;

        if (FModel.getPreferences().getPrefBoolean(FPref.UI_STACK_CREATURES) && event.turnOwner != null) {
            // anything except stack will get here
            updateZone(event.turnOwner, ZoneType.Battlefield);
        }
        return null;
    }

    @Override
    public Void visit(GameEventAnteCardsSelected ev) {
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
        needPlayerControlUpdate = true;
        return null;
    }

    @Override
    public Void visit(GameEventGameOutcome ev) {
        gameOver = true;
        return null;
    }

    @Override
    public Void visit(GameEventGameFinished ev) {
        gameFinished = true;
        return null;
    }

    @Override
    public Void visit(GameEventSpellAbilityCast event) {
        needStackUpdate = true;
        return null;
    }

    @Override
    public Void visit(GameEventSpellResolved event) {
        needStackUpdate = true;
        return null;
    }

    @Override
    public Void visit(GameEventSpellRemovedFromStack event) {
        needStackUpdate = true;
        return null;
    }

    @Override
    public Void visit(GameEventZone event) {
        if (event.player != null) {
            // anything except stack will get here
            updateZone(event.player, event.zoneType);
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
        return null;
    }

    private void updateZone(final Zone z) {
        if (z == null) { return; }
        updateZone(z.getPlayer(), z.getZoneType());
    }
    private void updateZone(Player p, ZoneType z) {
        if (p == null || z == null) { return; }
        zonesUpdate.add(p.getId() + PLAYER_ZONE_DELIM + z.name());
    }

    @Override
    public Void visit(final GameEventCardTapped event) {
        cardsUpdate.add(event.card);
        return null;
    }
    
    @Override
    public Void visit(final GameEventCardPhased event) {
        cardsUpdate.add(event.card);
        return null;
    }

    @Override
    public Void visit(final GameEventCardDamaged event) {
        cardsUpdate.add(event.card);
        return null;
    }

    @Override
    public Void visit(final GameEventCardCounters event) {
        cardsUpdate.add(event.card);
        return null;
    }

    @Override
    public Void visit(final GameEventBlockersDeclared event) { // This is to draw icons on blockers declared by AI
        for (MapOfLists<Card, Card> kv : event.blockers.values()) {
            for (Collection<Card> blockers : kv.values()) {
                cardsUpdate.addAll(blockers);
            }
        }
        return null;
    }

    @Override
    public Void visit(GameEventAttackersDeclared event) {
        // Skip redraw for GUI player?
        if (event.player.getLobbyPlayer() == GamePlayerUtil.getGuiPlayer()) {
            return null;
        }

        // Update all attackers.
        // Although they might have been updated when they were tapped, there could be someone with vigilance, not redrawn yet.
        cardsUpdate.addAll(event.attackersMap.values());
        return null;
    }

    @Override
    public Void visit(GameEventCombatChanged event) {
        needCombatUpdate = true;
        return null;
    }

    @Override
    public Void visit(GameEventCombatEnded event) {
        needCombatUpdate = true;

        // This should remove sword/shield icons from combatants by the time game moves to M2
        cardsUpdate.addAll(event.attackers);
        cardsUpdate.addAll(event.blockers);
        return null;
    }

    @Override
    public Void visit(GameEventCardChangeZone event) {
        updateZone(event.from);
        updateZone(event.to);
        return null;
    }

    @Override
    public Void visit(GameEventCardStatsChanged event) {
        cardsRefreshDetails.addAll(event.cards);
        cardsUpdate.addAll(event.cards);
        return null;
    }

    @Override
    public Void visit(GameEventPlayerStatsChanged event) {
        for (final Player p : event.players) {
            cardsRefreshDetails.addAll(p.getAllCards());
        }
        return null;
    }

    @Override
    public Void visit(GameEventShuffle event) {
        updateZone(event.player.getZone(ZoneType.Library));
        return null;
    }

    @Override
    public Void visit(GameEventManaPool event) {
        manaPoolUpdate.add(event.player);
        return null;
    }

    @Override
    public Void visit(GameEventPlayerLivesChanged event) {
        livesUpdate.add(event.player);
        return null;
    }

    @Override
    public Void visit(GameEventPlayerPoisoned event) {
        livesUpdate.add(event.receiver);
        return null;
    }
}