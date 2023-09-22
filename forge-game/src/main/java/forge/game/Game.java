/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.game;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.*;
import com.google.common.eventbus.EventBus;
import forge.GameCommand;
import forge.card.CardRarity;
import forge.card.CardStateName;
import forge.card.CardType.Supertype;
import forge.game.ability.AbilityKey;
import forge.game.card.*;
import forge.game.combat.Combat;
import forge.game.event.Event;
import forge.game.event.GameEventDayTimeChanged;
import forge.game.event.GameEventGameOutcome;
import forge.game.phase.Phase;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.phase.Untap;
import forge.game.player.*;
import forge.game.replacement.ReplacementHandler;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.trigger.TriggerHandler;
import forge.game.trigger.TriggerType;
import forge.game.zone.CostPaymentStack;
import forge.game.zone.MagicStack;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.trackable.Tracker;
import forge.util.Aggregates;
import forge.util.MyRandom;
import forge.util.Visitor;
import forge.util.collect.FCollection;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

/**
 * Represents the state of a <i>single game</i>, a new instance is created for each game.
 */
public class Game {

    private static int maxId = 0;
    private static int nextId() { return ++maxId; }

    /** The ID. */
    private int id;
    private final GameRules rules;
    private final PlayerCollection allPlayers = new PlayerCollection();
    private final PlayerCollection ingamePlayers = new PlayerCollection();
    private final PlayerCollection lostPlayers = new PlayerCollection();

    private List<Card> activePlanes = null;

    public final Phase cleanup;
    public final Phase endOfCombat;
    public final Phase endOfTurn;
    public final Untap untap;
    public final Phase upkeep;
    // to execute commands for "current" phase each time state based action is checked
    public final List<GameCommand> sbaCheckedCommandList;
    public final MagicStack stack;
    public final CostPaymentStack costPaymentStack = new CostPaymentStack();
    private final PhaseHandler phaseHandler;
    private final StaticEffects staticEffects = new StaticEffects();
    private final TriggerHandler triggerHandler = new TriggerHandler(this);
    private final ReplacementHandler replacementHandler = new ReplacementHandler(this);
    private final EventBus events = new EventBus("game events");
    private final GameLog gameLog = new GameLog();

    private final Zone stackZone = new Zone(ZoneType.Stack, this);

    private CardCollection lastStateBattlefield = new CardCollection();
    private CardCollection lastStateGraveyard = new CardCollection();

    private CardZoneTable untilHostLeavesPlayTriggerList = new CardZoneTable();

    private Table<CounterType, Player, List<Pair<Card, Integer>>> countersAddedThisTurn = HashBasedTable.create();
    private Multimap<CounterType, Pair<Card, Integer>> countersRemovedThisTurn = ArrayListMultimap.create();

    private FCollection<CardDamageHistory> globalDamageHistory = new FCollection<>();
    private IdentityHashMap<Pair<Integer, Boolean>, Pair<Card, GameEntity>> damageThisTurnLKI = new IdentityHashMap<>();

    private Map<Player, Card> topLibsCast = Maps.newHashMap();
    private Map<Card, Integer> facedownWhileCasting = Maps.newHashMap();

    private Player monarch;
    private Player initiative;
    private Player monarchBeginTurn;
    private Player startingPlayer;

    private Direction turnOrder = Direction.getDefaultDirection();

    private Boolean daytime = null;

    private long timestamp = 0;
    public final GameAction action;
    private final Match match;
    private GameStage age = GameStage.BeforeMulligan;
    private GameOutcome outcome;
    private final Game maingame;

    private final GameView view;
    private final Tracker tracker = new Tracker();

    /**
     * Gets the id.
     *
     * @return the id
     */
    public int getId() {
        return this.id;
    }

    public Player getStartingPlayer() {
        return startingPlayer;
    }
    public void setStartingPlayer(final Player p) {
        startingPlayer = p;
    }

    public Player getMonarch() {
        return monarch;
    }
    public void setMonarch(final Player p) {
        monarch = p;
    }

    public Player getMonarchBeginTurn() {
        return monarchBeginTurn;
    }
    public void setMonarchBeginTurn(Player monarchBeginTurn) {
        this.monarchBeginTurn = monarchBeginTurn;
    }

    public Player getHasInitiative() {
        return initiative;
    }
    public void setHasInitiative(final Player p) {
        initiative = p;
    }

    public CardZoneTable getUntilHostLeavesPlayTriggerList() {
        return untilHostLeavesPlayTriggerList;
    }

    public CardCollectionView getLastStateBattlefield() {
        return lastStateBattlefield;
    }
    public CardCollectionView getLastStateGraveyard() {
        return lastStateGraveyard;
    }

    public void copyLastState() {
        lastStateBattlefield.clear();
        lastStateGraveyard.clear();
        Map<Integer, Card> cachedMap = Maps.newHashMap();
        for (final Player p : getPlayers()) {
            lastStateBattlefield.addAll(p.getZone(ZoneType.Battlefield).getLKICopy(cachedMap));
            lastStateGraveyard.addAll(p.getZone(ZoneType.Graveyard).getLKICopy(cachedMap));
        }
    }

    public CardCollectionView copyLastState(ZoneType type) {
        CardCollection result = new CardCollection();
        Map<Integer, Card> cachedMap = Maps.newHashMap();
        for (final Player p : getPlayers()) {
            result.addAll(p.getZone(type).getLKICopy(cachedMap));
        }
        return result;
    }

    public CardCollectionView copyLastStateBattlefield() {
        return copyLastState(ZoneType.Battlefield);
    }

    public CardCollectionView copyLastStateGraveyard() {
        return copyLastState(ZoneType.Graveyard);
    }

    public void updateLastStateForCard(Card c) {
        if (c == null || c.getZone() == null) {
            return;
        }

        ZoneType zone = c.getZone().getZoneType();
        CardCollection lookup = zone.equals(ZoneType.Battlefield) ? lastStateBattlefield
                : zone.equals(ZoneType.Graveyard) ? lastStateGraveyard
                : null;

        if (lookup != null) {
            lookup.remove(c);
            lookup.add(CardUtil.getLKICopy(c));
        }
    }

    private final GameEntityCache<Player, PlayerView> playerCache = new GameEntityCache<>();
    public Player getPlayer(PlayerView playerView) {
        return playerCache.get(playerView);
    }
    public void addPlayer(int id, Player player) {
        playerCache.put(Integer.valueOf(id), player);
    }

    // methods that deal with saving, retrieving and clearing LKI information about cards on zone change
    private final HashMap<Integer, Card> changeZoneLKIInfo = new HashMap<>();
    public final void addChangeZoneLKIInfo(Card lki) {
        if (lki == null) {
            return;
        }
        changeZoneLKIInfo.put(lki.getId(), lki);
    }
    public final Card getChangeZoneLKIInfo(Card c) {
        if (c == null) {
            return null;
        }
        return changeZoneLKIInfo.getOrDefault(c.getId(), c);
    }
    public final void clearChangeZoneLKIInfo() {
        changeZoneLKIInfo.clear();
    }

    public Game(Iterable<RegisteredPlayer> players0, GameRules rules0, Match match0) {
        this(players0, rules0, match0, null, -1);
    }

    public Game(Iterable<RegisteredPlayer> players0, GameRules rules0, Match match0, Game maingame0, int startingLife) { /* no more zones to map here */
        rules = rules0;
        match = match0;
        maingame = maingame0;
        this.id = nextId();

        int highestTeam = -1;
        for (RegisteredPlayer psc : players0) {
            // Track highest team number for auto assigning unassigned teams
            int teamNum = psc.getTeamNumber();
            if (teamNum > highestTeam) {
                highestTeam = teamNum;
            }
        }

        // View needs to be done before PlayerController
        view = new GameView(this);

        int plId = 0;
        for (RegisteredPlayer psc : players0) {
            IGameEntitiesFactory factory = (IGameEntitiesFactory)psc.getPlayer();
            Player pl = factory.createIngamePlayer(this, plId++);
            allPlayers.add(pl);
            ingamePlayers.add(pl);

            if (startingLife != -1) {
                pl.setStartingLife(startingLife);
            } else {
                pl.setStartingLife(psc.getStartingLife());
            }
            pl.setMaxHandSize(psc.getStartingHand());
            pl.setStartingHandSize(psc.getStartingHand());

            if (psc.getManaShards() > 0) {
                pl.setNumManaShards(psc.getManaShards());
            }
            int teamNum = psc.getTeamNumber();
            if (teamNum == -1) {
                // RegisteredPlayer doesn't have an assigned team, set it to 1 higher than the highest found team number
                teamNum = ++highestTeam;
                psc.setTeamNumber(teamNum);
            }

            pl.setTeam(teamNum);
        }

        action = new GameAction(this);
        stack = new MagicStack(this);
        phaseHandler = new PhaseHandler(this);

        untap = new Untap(this);
        upkeep = new Phase(PhaseType.UPKEEP);
        cleanup = new Phase(PhaseType.CLEANUP);
        endOfCombat = new Phase(PhaseType.COMBAT_END);
        endOfTurn = new Phase(PhaseType.END_OF_TURN);

        sbaCheckedCommandList = new ArrayList<>();

        // update players
        view.updatePlayers(this);

        subscribeToEvents(gameLog.getEventVisitor());
    }

    public GameView getView() {
        return view;
    }

    public Tracker getTracker() {
        return tracker;
    }

    /**
     * Gets the players who are still fighting to win.
     */
    public final PlayerCollection getPlayers() {
        return ingamePlayers;
    }

    public final PlayerCollection getLostPlayers() {
        return lostPlayers;
    }

    /**
     * Gets the players who are still fighting to win, in turn order.
     */
    public final PlayerCollection getPlayersInTurnOrder() {
        if (getTurnOrder().isDefaultDirection()) {
            return ingamePlayers;
        }
        final PlayerCollection players = new PlayerCollection(ingamePlayers);
        Collections.reverse(players);
        return players;
    }

    /**
     * Gets the nonactive players who are still fighting to win, in turn order.
     */
    public final PlayerCollection getNonactivePlayers() {
        // Don't use getPlayersInTurnOrder to prevent copying the player collection twice
        final PlayerCollection players = new PlayerCollection(ingamePlayers);
        players.remove(phaseHandler.getPlayerTurn());
        if (!getTurnOrder().isDefaultDirection()) {
            Collections.reverse(players);
        }
        return players;
    }

    /**
     * Gets the players who participated in match (regardless of outcome).
     * <i>Use this in UI and after match calculations</i>
     */
    public final PlayerCollection getRegisteredPlayers() {
        return allPlayers;
    }

    public final Untap getUntap() {
        return untap;
    }
    public final Phase getUpkeep() {
        return upkeep;
    }
    public final Phase getEndOfCombat() {
        return endOfCombat;
    }
    public final Phase getEndOfTurn() {
        return endOfTurn;
    }
    public final Phase getCleanup() {
        return cleanup;
    }

    public void addSBACheckedCommand(final GameCommand c) {
        sbaCheckedCommandList.add(c);
    }
    public final void runSBACheckedCommands() {
        for (final GameCommand c : sbaCheckedCommandList) {
            c.run();
        }
        sbaCheckedCommandList.clear();
    }

    public final PhaseHandler getPhaseHandler() {
        return phaseHandler;
    }
    public final void updateTurnForView() {
        view.updateTurn(phaseHandler);
    }
    public final void updatePhaseForView() {
        view.updatePhase(phaseHandler);
    }
    public final void updatePlayerTurnForView() {
        view.updatePlayerTurn(phaseHandler);
    }

    public final MagicStack getStack() {
        return stack;
    }
    public final void updateStackForView() {
        view.updateStack(stack);
    }

    public final StaticEffects getStaticEffects() {
        return staticEffects;
    }

    public final TriggerHandler getTriggerHandler() {
        return triggerHandler;
    }

    public final Combat getCombat() {
        return getPhaseHandler().getCombat();
    }
    public final void updateCombatForView() {
        view.updateCombat(getCombat());
    }

    public final GameLog getGameLog() {
        return gameLog;
    }
    public final void updateGameLogForView() {
        view.updateGameLog(gameLog);
    }

    public final Zone getStackZone() {
        return stackZone;
    }

    public CardCollectionView getCardsPlayerCanActivateInStack() {
        return CardLists.filter(stackZone.getCards(), new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                for (final SpellAbility sa : c.getSpellAbilities()) {
                    final ZoneType restrictZone = sa.getRestrictions().getZone();
                    if (ZoneType.Stack == restrictZone) {
                        return true;
                    }
                }
                return false;
            }
        });
    }

    /**
     * The Direction in which the turn order of this Game currently proceeds.
     */
    public final Direction getTurnOrder() {
        if (phaseHandler.getPlayerTurn() != null && phaseHandler.getPlayerTurn().getAmountOfKeyword("The turn order is reversed.") % 2 == 1) {
            return turnOrder.getOtherDirection();
        }
    	return turnOrder;
    }
    public final void reverseTurnOrder() {
    	turnOrder = turnOrder.getOtherDirection();
    }
    public final void resetTurnOrder() {
    	turnOrder = Direction.getDefaultDirection();
    }

    /**
     * Create and return the next timestamp.
     */
    public final long getNextTimestamp() {
        timestamp = getTimestamp() + 1;
        return getTimestamp();
    }
    public final long getTimestamp() {
        return timestamp;
    }

    public final GameOutcome getOutcome() {
        return outcome;
    }

    public final Game getMaingame() {
        return maingame;
    }

    public ReplacementHandler getReplacementHandler() {
        return replacementHandler;
    }

    public synchronized boolean isGameOver() {
        return age == GameStage.GameOver;
    }

    public synchronized void setGameOver(GameEndReason reason) {
        for (Player p : allPlayers) {
            p.clearController();
        }
        age = GameStage.GameOver;

        for (Player p : getPlayers()) {
            p.onGameOver();
        }

        final GameOutcome result = new GameOutcome(reason, getRegisteredPlayers());
        result.setTurnsPlayed(getPhaseHandler().getTurn());

        outcome = result;
        if (maingame == null) {
            match.addGamePlayed(this);
        }

        view.updateGameOver(this);

        // The log shall listen to events and generate text internally
        if (maingame == null) {
            fireEvent(new GameEventGameOutcome(result, match.getOutcomes()));
        }
    }

    public Zone getZoneOf(final Card card) {
        return card.getLastKnownZone();
    }

    public synchronized CardCollectionView getCardsIn(final ZoneType zone) {
        if (zone == ZoneType.Stack) {
            return getStackZone().getCards();
        }
        return getPlayers().getCardsIn(zone);
    }

    public CardCollectionView getCardsIncludePhasingIn(final ZoneType zone) {
        if (zone == ZoneType.Stack) {
            return getStackZone().getCards();
        }

        CardCollection cards = new CardCollection();
        for (final Player p : getPlayers()) {
            cards.addAll(p.getCardsIn(zone, false));
        }
        return cards;
    }

    public CardCollectionView getCardsIn(final Iterable<ZoneType> zones) {
        CardCollection cards = new CardCollection();
        for (final ZoneType z : zones) {
            cards.addAll(getCardsIn(z));
        }
        return cards;
    }

    public CardCollectionView getCardsInOwnedBy(final Iterable<ZoneType> zones, Player p) {
        CardCollection cards = new CardCollection();
        for (final ZoneType z : zones) {
            cards.addAll(getCardsIncludePhasingIn(z));
        }
        return CardLists.filter(cards, CardPredicates.isOwner(p));
    }

    public boolean isCardExiled(final Card c) {
        return getCardsIn(ZoneType.Exile).contains(c);
    }

    public boolean isCardInPlay(final String cardName) {
        return Iterables.any(getCardsIn(ZoneType.Battlefield), CardPredicates.nameEquals(cardName));
    }

    public boolean isCardInCommand(final String cardName) {
        return Iterables.any(getCardsIn(ZoneType.Command), CardPredicates.nameEquals(cardName));
    }

    public CardCollectionView getColoredCardsInPlay(final String color) {
        final CardCollection cards = new CardCollection();
        for (Player p : getPlayers()) {
            cards.addAll(p.getColoredCardsInPlay(color));
        }
        return cards;
    }

    private static class CardStateVisitor extends Visitor<Card> {
        Card found = null;
        Card old = null;

        private CardStateVisitor(final Card card) {
            this.old = card;
        }

        @Override
        public boolean visit(Card object) {
            if (object.equals(old)) {
                found = object;
            }
            return found == null;
        }

        public Card getFound(final Card notFound) {
            return found == null ? notFound : found;
        }
    }

    public Card getCardState(final Card card) {
        return getCardState(card, card);
    }
    public Card getCardState(final Card card, final Card notFound) {
        CardStateVisitor visit = new CardStateVisitor(card);
        this.forEachCardInGame(visit);
        return visit.getFound(notFound);
    }

    private static class CardIdVisitor extends Visitor<Card> {
        Card found = null;
        int id;

        private CardIdVisitor(final int id) {
            this.id = id;
        }

        @Override
        public boolean visit(Card object) {
            if (this.id == object.getId()) {
                found = object;
            }
            return found == null;
        }

        public Card getFound() {
            return found;
        }
    }

    public Card findByView(CardView view) {
        if (view == null) {
            return null;
        }
        CardIdVisitor visit = new CardIdVisitor(view.getId());
        if (ZoneType.Stack.equals(view.getZone())) {
            visit.visitAll(getStackZone());
        } else if (view.getController() != null && view.getZone() != null) {
            visit.visitAll(getPlayer(view.getController()).getZone(view.getZone()));
        } else { // fallback if view doesn't has controller or zone set for some reason
            forEachCardInGame(visit);
        }
        return visit.getFound();
    }

    public Card findById(int id) {
        CardIdVisitor visit = new CardIdVisitor(id);
        this.forEachCardInGame(visit);
        return visit.getFound();
    }

    public void forEachCardInGame(Visitor<Card> visitor) {
        forEachCardInGame(visitor, false);
    }
    // Allows visiting cards in game without allocating a temporary list.
    public void forEachCardInGame(Visitor<Card> visitor, boolean withSideboard) {
        for (final Player player : getPlayers()) {
            if (!visitor.visitAll(player.getZone(ZoneType.Graveyard).getCards())) {
                return;
            }
            if (!visitor.visitAll(player.getZone(ZoneType.Hand).getCards())) {
                return;
            }
            if (!visitor.visitAll(player.getZone(ZoneType.Library).getCards())) {
                return;
            }
            if (!visitor.visitAll(player.getZone(ZoneType.Battlefield).getCards(false))) {
                return;
            }
            if (!visitor.visitAll(player.getZone(ZoneType.Exile).getCards())) {
                return;
            }
            if (!visitor.visitAll(player.getZone(ZoneType.Command).getCards())) {
                return;
            }
            if (withSideboard && !visitor.visitAll(player.getZone(ZoneType.Sideboard).getCards())) {
                return;
            }
            if (!visitor.visitAll(player.getInboundTokens())) {
                return;
            }
        }
        visitor.visitAll(getStackZone().getCards());
    }
    public CardCollectionView getCardsInGame() {
        final CardCollection all = new CardCollection();
        Visitor<Card> visitor = new Visitor<Card>() {
            @Override
            public boolean visit(Card card) {
                all.add(card);
                return true;
            }
        };
        forEachCardInGame(visitor);
        return all;
    }

    public final GameAction getAction() {
        return action;
    }

    public final Match getMatch() {
        return match;
    }

    /**
     * Get the player whose turn it is after a given player's turn, taking turn
     * order into account.
     * @param playerTurn a {@link Player}, or {@code null}.
     * @return A {@link Player}, whose turn comes after the current player, or
     * {@code null} if there are no players in the game.
     */
    public Player getNextPlayerAfter(final Player playerTurn) {
        return getNextPlayerAfter(playerTurn, getTurnOrder());
    }

    /**
     * Get the player whose turn it is after a given player's turn, taking turn
     * order into account.
     * @param playerTurn a {@link Player}, or {@code null}.
     * @param turnOrder a {@link Direction}
     * @return A {@link Player}, whose turn comes after the current player, or
     * {@code null} if there are no players in the game.
     */
    public Player getNextPlayerAfter(final Player playerTurn, final Direction turnOrder) {
        int iPlayer = ingamePlayers.indexOf(playerTurn);

        if (ingamePlayers.isEmpty()) {
            return null;
        }

        final int shift = turnOrder.getShift();
        if (-1 == iPlayer) { // if playerTurn has just lost
        	final int totalNumPlayers = allPlayers.size();
            int iAlive;
            iPlayer = allPlayers.indexOf(playerTurn);
            do {
                iPlayer = (iPlayer + shift) % totalNumPlayers;
                if (iPlayer < 0) {
                	iPlayer += totalNumPlayers;
                }
                iAlive = ingamePlayers.indexOf(allPlayers.get(iPlayer));
            } while (iAlive < 0);
            iPlayer = iAlive;
        } else { // for the case playerTurn hasn't died
        	final int numPlayersInGame = ingamePlayers.size();
        	iPlayer = (iPlayer + shift) % numPlayersInGame;
        	if (iPlayer < 0) {
        		iPlayer += numPlayersInGame;
        	}
        }

        return ingamePlayers.get(iPlayer);
    }

    public int getPosition(Player player, Player startingPlayer) {
        int startPosition = ingamePlayers.indexOf(startingPlayer);
        int myPosition = ingamePlayers.indexOf(player);
        if (startPosition > myPosition) {
            myPosition += ingamePlayers.size();
        }

        return myPosition - startPosition + 1;
    }

    public void onPlayerLost(Player p) {
        //set for Avatar
        p.setHasLost(true);
        // Rule 800.4 Losing a Multiplayer game
        CardCollectionView cards = this.getCardsInGame();
        boolean planarControllerLost = false;
        boolean isMultiplayer = getPlayers().size() > 2;
        CardZoneTable triggerList = new CardZoneTable();

        // 702.142f & 707.9
        // If a player leaves the game, all face-down cards that player owns must be revealed to all players.
        // At the end of each game, all face-down cards must be revealed to all players.
        if (!isMultiplayer) {
            for (Player pl : getPlayers()) {
                pl.revealFaceDownCards();
            }
        } else {
            p.revealFaceDownCards();
        }

        for (Card c : cards) {
            // CR 800.4d if card is controlled by opponent, LTB should trigger
            if (c.getOwner().equals(p) && c.getController().equals(p)) {
                c.getGame().getTriggerHandler().clearActiveTriggers(c, null);
            }
        }

        for (Card c : cards) {
            if (c.getController().equals(p) && (c.isPlane() || c.isPhenomenon())) {
                planarControllerLost = true;
            }

            if (isMultiplayer) {
                // unattach all "Enchant Player"
                c.removeAttachedTo(p);
                if (c.getOwner().equals(p)) {
                    for (Card cc : cards) {
                        cc.removeImprintedCard(c);
                        cc.removeEncodedCard(c);
                        cc.removeRemembered(c);
                        cc.removeAttachedTo(c);
                        cc.removeAttachedCard(c);
                    }
                    triggerList.put(c.getZone().getZoneType(), null, c);
                    getAction().ceaseToExist(c, false);
                    // CR 603.2f owner of trigger source lost game
                    getTriggerHandler().clearDelayedTrigger(c);
                } else {
                    // return stolen permanents
                    if (c.isInPlay() && (c.getController().equals(p) || c.getZone().getPlayer().equals(p))) {
                        c.removeTempController(p);
                        getAction().controllerChangeZoneCorrection(c);
                    }
                    c.removeTempController(p);
                    // return stolen spells
                    if (c.isInZone(ZoneType.Stack)) {
                        SpellAbilityStackInstance si = getStack().getInstanceMatchingSpellAbilityID(c.getCastSA());
                        si.setActivatingPlayer(c.getController());
                    }
                    if (c.getController().equals(p)) {
                        getAction().exile(c, null, null);
                        triggerList.put(ZoneType.Battlefield, c.getZone().getZoneType(), c);
                    }
                }
            } else {
                c.forceTurnFaceUp();
            }
        }

        triggerList.triggerChangesZoneAll(this, null);

        // 901.6: If the current planar controller would leave the game, instead the next player
        // in turn order that wouldn’t leave the game becomes the planar controller, then the old
        // planar controller leaves
        // 901.10: When a player leaves the game, all objects owned by that player except abilities
        // from phenomena leave the game. (See rule 800.4a.) If that includes a face-up plane card
        // or phenomenon card, the planar controller turns the top card of his or her planar deck face up.the game.
        if (planarControllerLost) {
            getNextPlayerAfter(p).initPlane();
        }

        if (p.isMonarch()) {
            // if the player who lost was the Monarch, someone else will be the monarch
            // TODO need to check rules if it should try the next player if able
            if (p.equals(getPhaseHandler().getPlayerTurn())) {
                getAction().becomeMonarch(getNextPlayerAfter(p), null);
            } else {
                getAction().becomeMonarch(getPhaseHandler().getPlayerTurn(), null);
            }
        }

        if (p.hasInitiative()) {
            // The third way to take the initiative is if the player who currently has the initiative leaves the game.
            // When that happens, the player whose turn it is takes the initiative.
            // If the player who has the initiative leaves the game on their own turn,
            // or the active player left the game at the same time, the next player in turn order takes the initiative.
            if (p.equals(getPhaseHandler().getPlayerTurn())) {
                getAction().takeInitiative(getNextPlayerAfter(p), null);
            } else {
                getAction().takeInitiative(getPhaseHandler().getPlayerTurn(), null);
            }
        }

        // Remove leftover items from
        getStack().removeInstancesControlledBy(p);

        getTriggerHandler().onPlayerLost(p);

        ingamePlayers.remove(p);
        lostPlayers.add(p);

        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromPlayer(p);
        getTriggerHandler().runTrigger(TriggerType.LosesGame, runParams, false);
    }

    /**
     * Fire only the events after they became real for gamestate and won't get replaced.<br>
     * The events are sent to UI, log and sound system. Network listeners are under development.
     */
    public void fireEvent(final Event event) {
        events.post(event);
    }
    public void subscribeToEvents(final Object subscriber) {
        events.register(subscriber);
    }

    public GameRules getRules() {
        return rules;
    }

    public List<Card> getActivePlanes() {
        return activePlanes;
    }
    public void setActivePlanes(List<Card> activePlane0) {
        activePlanes = activePlane0;
    }

    public void archenemy904_10() {
        //904.10. If a non-ongoing scheme card is face up in the
        //command zone, and it isn't the source of a triggered ability
        //that has triggered but not yet left the stack, that scheme card
        //is turned face down and put on the bottom of its owner's scheme
        //deck the next time a player would receive priority.
        //(This is a state-based action. See rule 704.)

        for (int i = 0; i < getCardsIn(ZoneType.Command).size(); i++) {
            Card c = getCardsIn(ZoneType.Command).get(i);
            if (c.isScheme() && !c.getType().hasSupertype(Supertype.Ongoing)) {
                boolean foundonstack = false;
                for (SpellAbilityStackInstance si : stack) {
                    if (si.getSourceCard().equals(c)) {
                        foundonstack = true;
                        break;
                    }
                }
                if (!foundonstack) {
                    getTriggerHandler().suppressMode(TriggerType.ChangesZone);
                    c.getController().getZone(ZoneType.Command).remove(c);
                    i--;
                    getTriggerHandler().clearSuppression(TriggerType.ChangesZone);

                    c.getController().getZone(ZoneType.SchemeDeck).add(c);
                }
            }
        }
    }

    public GameStage getAge() {
        return age;
    }
    public void setAge(GameStage value) {
        age = value;
    }

    private int cardIdCounter = 0, hiddenCardIdCounter = 0;
    public int nextCardId() {
        return ++cardIdCounter;
    }
    public int nextHiddenCardId() {
        return ++hiddenCardIdCounter;
    }

    public Multimap<Player, Card> chooseCardsForAnte(final boolean matchRarity) {
        Multimap<Player, Card> anteed = ArrayListMultimap.create();

        if (matchRarity) {
            boolean onePlayerHasTimeShifted = false;

            List<CardRarity> validRarities = new ArrayList<>(Arrays.asList(CardRarity.values()));
            for (final Player player : getPlayers()) {
                final Set<CardRarity> playerRarity = getValidRarities(player.getCardsIn(ZoneType.Library));
                if (!onePlayerHasTimeShifted) {
                    onePlayerHasTimeShifted = playerRarity.contains(CardRarity.Special);
                }
                validRarities.retainAll(playerRarity);
            }

            if (validRarities.size() == 0) { //If no possible rarity matches were found, use the original method to choose antes
                for (Player player : getPlayers()) {
                    chooseRandomCardsForAnte(player, anteed);
                }
                return anteed;
            }

            //If possible, don't ante basic lands
            if (validRarities.size() > 1) {
                validRarities.remove(CardRarity.BasicLand);
            }

            if (validRarities.contains(CardRarity.Special)) {
                onePlayerHasTimeShifted = false;
            }

            CardRarity anteRarity = validRarities.get(MyRandom.getRandom().nextInt(validRarities.size()));

            System.out.println("Rarity chosen for ante: " + anteRarity.name());

            for (final Player player : getPlayers()) {
                CardCollection library = new CardCollection(player.getCardsIn(ZoneType.Library));
                CardCollection toRemove = new CardCollection();

                //Remove all cards that aren't of the chosen rarity
                for (Card card : library) {
                    if (onePlayerHasTimeShifted && card.getRarity() == CardRarity.Special) {
                        //Since Time Shifted cards don't have a traditional rarity, they're wildcards
                        continue;
                    } else if (anteRarity == CardRarity.MythicRare || anteRarity == CardRarity.Rare) {
                        //Rare and Mythic Rare cards are considered the same rarity, just as in booster packs
                        //Otherwise it's possible to never lose Mythic Rare cards if you choose opponents carefully
                        //It also lets you win Mythic Rare cards when you don't have any to ante
                        if (card.getRarity() != CardRarity.MythicRare && card.getRarity() != CardRarity.Rare) {
                            toRemove.add(card);
                        }
                    } else {
                        if (card.getRarity() != anteRarity) {
                            toRemove.add(card);
                        }
                    }
                }

                library.removeAll(toRemove);

                if (library.size() > 0) { //Make sure that matches were found. If not, use the original method to choose antes
                    Card ante = library.get(MyRandom.getRandom().nextInt(library.size()));
                    anteed.put(player, ante);
                } else {
                    chooseRandomCardsForAnte(player, anteed);
                }
            }
        }
        else {
            for (Player player : getPlayers()) {
                chooseRandomCardsForAnte(player, anteed);
            }
        }
        return anteed;
    }

    private void chooseRandomCardsForAnte(final Player player, final Multimap<Player, Card> anteed) {
        final CardCollectionView lib = player.getCardsIn(ZoneType.Library);
        Predicate<Card> goodForAnte = Predicates.not(CardPredicates.Presets.BASIC_LANDS);
        Card ante = Aggregates.random(Iterables.filter(lib, goodForAnte));
        if (ante == null) {
            getGameLog().add(GameLogEntryType.ANTE, "Only basic lands found. Will ante one of them");
            ante = Aggregates.random(lib);
        }
        anteed.put(player, ante);
    }

    private static Set<CardRarity> getValidRarities(final Iterable<Card> cards) {
        final Set<CardRarity> rarities = new HashSet<>();
        for (final Card card : cards) {
            if (card.getRarity() == CardRarity.Rare || card.getRarity() == CardRarity.MythicRare) {
                //Since both rare and mythic rare are considered the same, adding both rarities
                //massively increases the odds chances of the game picking rare cards to ante.
                //This is a little unfair, so we add just one of the two.
                rarities.add(CardRarity.Rare);
            } else {
                rarities.add(card.getRarity());
            }
        }
        return rarities;
    }

    public void clearCaches() {
        lastStateBattlefield.clear();
        lastStateGraveyard.clear();
        //playerCache.clear();
    }

    // Does the player control any cards that care about the order of cards in the graveyard?
    public boolean isGraveyardOrdered(final Player p) {
        for (Card c : p.getAllCards()) {
            if (c.hasSVar("NeedsOrderedGraveyard")) {
                return true;
            } else if (c.getOriginalState(CardStateName.Original).hasSVar("NeedsOrderedGraveyard")) {
                return true;
            }
        }
        for (Card c : p.getOpponents().getCardsIn(ZoneType.Battlefield)) {
            // Bone Dancer is important when an opponent has it active on the battlefield
            if ("opponent".equalsIgnoreCase(c.getSVar("NeedsOrderedGraveyard"))) {
                return true;
            }
        }
        return false;
    }

    public Player getControlVote() {
        Player result = null;
        long maxValue = 0;
        for (Player p : getPlayers()) {
            Long v = p.getHighestControlVote();
            if (v != null && v > maxValue) {
                maxValue = v;
                result = p;
            }
        }
        return result;
    }

    public void onCleanupPhase() {
        clearCounterAddedThisTurn();
        clearCounterRemovedThisTurn();
        clearGlobalDamageHistory();
        // some cards need this info updated even after a player lost, so don't skip them
        for (Player player : getRegisteredPlayers()) {
            player.onCleanupPhase();
        }
    }

    public void addCounterAddedThisTurn(Player putter, CounterType cType, Card card, Integer value) {
        if (putter == null || card == null || value <= 0) {
            return;
        }
        List<Pair<Card, Integer>> result = countersAddedThisTurn.get(cType, putter);
        if (result == null) {
            result = Lists.newArrayList();
            countersAddedThisTurn.put(cType, putter, result);
        }
        result.add(Pair.of(CardUtil.getLKICopy(card), value));
    }

    public int getCounterAddedThisTurn(CounterType cType, String validPlayer, String validCard, Card source, Player sourceController, CardTraitBase ctb) {
        int result = 0;
        if (!countersAddedThisTurn.containsRow(cType)) {
            return result;
        }
        for (Map.Entry<Player, List<Pair<Card, Integer>>> e : countersAddedThisTurn.row(cType).entrySet()) {
           if (e.getKey().isValid(validPlayer.split(","), sourceController, source, ctb)) {
               for (Pair<Card, Integer> p : e.getValue()) {
                   if (p.getKey().isValid(validCard.split(","), sourceController, source, ctb)) {
                       result += p.getValue();
                   }
               }
           }
        }
        return result;
    }
    public int getCounterAddedThisTurn(CounterType cType, Card card) {
        int result = 0;
        if (!countersAddedThisTurn.containsRow(cType)) {
            return result;
        }
        for (List<Pair<Card, Integer>> l : countersAddedThisTurn.row(cType).values()) {
            for (Pair<Card, Integer> p : l) {
                if (p.getKey().equalsWithTimestamp(card)) {
                    result += p.getValue();
                }
            }
        }
        return result;
    }

    public void clearCounterAddedThisTurn() {
        countersAddedThisTurn.clear();
    }

    public void addCounterRemovedThisTurn(CounterType cType, Card card, Integer value) {
        countersRemovedThisTurn.put(cType, Pair.of(CardUtil.getLKICopy(card), value));
    }

    public int getCounterRemovedThisTurn(CounterType cType, String validCard, Card source, Player sourceController, CardTraitBase ctb) {
        int result = 0;
        for (Pair<Card, Integer> p : countersRemovedThisTurn.get(cType)) {
            if (p.getKey().isValid(validCard.split(","), sourceController, source, ctb)) {
                result += p.getValue();
            }
        }
        return result;
    }

    public void clearCounterRemovedThisTurn() {
        countersRemovedThisTurn.clear();
    }

    /**
     * Gets the damage instances done this turn.
     * @param isCombat if true only combat damage matters, pass null for both
     * @param anyIsEnough if true returns early once result has an entry
     * @param validSourceCard
     * @param validTargetEntity
     * @param source
     * @param sourceController
     * @param ctb
     * @return List<Integer> for each source
     */
    public List<Integer> getDamageDoneThisTurn(Boolean isCombat, boolean anyIsEnough, String validSourceCard, String validTargetEntity, Card source, Player sourceController, CardTraitBase ctb) {
        final List<Integer> dmgList = Lists.newArrayList();
        for (CardDamageHistory cdh : globalDamageHistory) {
            int dmg = cdh.getDamageDoneThisTurn(isCombat, anyIsEnough, validSourceCard, validTargetEntity, source, sourceController, ctb);
            if (dmg == 0) {
                continue;
            }

            dmgList.add(dmg);

            if (anyIsEnough) {
                break;
            }
        }

        return dmgList;
    }

    public void addGlobalDamageHistory(CardDamageHistory cdh, Pair<Integer, Boolean> dmg, Card source, GameEntity target) {
        globalDamageHistory.add(cdh);
        damageThisTurnLKI.put(dmg, Pair.of(source, target));
    }
    public void clearGlobalDamageHistory() {
        globalDamageHistory.clear();
        damageThisTurnLKI.clear();
    }

    public Pair<Card, GameEntity> getDamageLKI(Pair<Integer, Boolean> dmg) {
        return damageThisTurnLKI.get(dmg);
    }

    public Card getTopLibForPlayer(Player P) {
        return topLibsCast.get(P);
    }
    public void setTopLibsCast() {
        for (Player p : getPlayers()) {
            topLibsCast.put(p, p.getTopXCardsFromLibrary(1).isEmpty() ? null : p.getTopXCardsFromLibrary(1).get(0));
        }
    }
    public void clearTopLibsCast(SpellAbility sa) {
        // if nothing left to pay
        if (sa.getActivatingPlayer().getPaidForSA() == null) {
            topLibsCast.clear();
            for (Card c : facedownWhileCasting.keySet()) {
                // maybe it was discarded as payment?
                if (c.isInZone(ZoneType.Hand)) {
                    c.forceTurnFaceUp();

                    // If an effect allows or instructs a player to reveal the card as it’s being drawn,
                    // it’s revealed after the spell becomes cast or the ability becomes activated.
                    final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(c);
                    runParams.put(AbilityKey.Number, facedownWhileCasting.get(c));
                    runParams.put(AbilityKey.Player, c.getOwner());
                    runParams.put(AbilityKey.CanReveal, true);
                    // need to hold trigger to clear list first
                    getTriggerHandler().runTrigger(TriggerType.Drawn, runParams, true);
                }
            }
            facedownWhileCasting.clear();
        }
    }
    public void addFacedownWhileCasting(Card c, int numDrawn) {
        facedownWhileCasting.put(c, Integer.valueOf(numDrawn));
    }

    public boolean isDay() {
        return this.daytime != null && this.daytime == false;
    }
    public boolean isNight() {
        return this.daytime != null && this.daytime == true;
    }
    public boolean isNeitherDayNorNight() {
        return this.daytime == null;
    }

    public Boolean getDayTime() {
        return this.daytime;
    }
    public void setDayTime(Boolean value) {
        Boolean previous = this.daytime;
        this.daytime = value;

        if (previous != null && value != null && previous != value) {
            Map<AbilityKey, Object> params = AbilityKey.newMap();
            this.getTriggerHandler().runTrigger(TriggerType.DayTimeChanges, params, false);
        }
        if (!isNeitherDayNorNight())
            fireEvent(new GameEventDayTimeChanged(isDay()));
    }
}
