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

import java.util.*;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.EventBus;

import forge.card.CardRarity;
import forge.card.CardType.Supertype;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardUtil;
import forge.game.card.CardView;
import forge.game.combat.Combat;
import forge.game.cost.Cost;
import forge.game.event.Event;
import forge.game.event.GameEventGameOutcome;
import forge.game.phase.Phase;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.phase.Untap;
import forge.game.phase.Upkeep;
import forge.game.player.IGameEntitiesFactory;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.player.RegisteredPlayer;
import forge.game.replacement.ReplacementHandler;
import forge.game.spellability.Ability;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.spellability.SpellAbilityView;
import forge.game.trigger.TriggerHandler;
import forge.game.trigger.TriggerType;
import forge.game.zone.*;
import forge.trackable.Tracker;
import forge.util.Aggregates;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;
import forge.util.Visitor;

/**
 * Represents the state of a <i>single game</i>, a new instance is created for each game.
 */
public class Game {
    private final GameRules rules;
    private final FCollection<Player> allPlayers = new FCollection<Player>();
    private final FCollection<Player> ingamePlayers = new FCollection<Player>();

    private List<Card> activePlanes = null;

    public final Phase cleanup;
    public final Phase endOfCombat;
    public final Phase endOfTurn;
    public final Untap untap;
    public final Upkeep upkeep;
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

    private Direction turnOrder = Direction.getDefaultDirection();

    private long timestamp = 0;
    public final GameAction action;
    private final Match match;
    private GameStage age = GameStage.BeforeMulligan;
    private GameOutcome outcome;

    private final GameView view; 
    private final Tracker tracker = new Tracker();


    public CardCollectionView getLastStateBattlefield() {
        return lastStateBattlefield;
    }

    public void copyLastStateBattlefield() {
        lastStateBattlefield.clear();
        for (final Player p : getPlayers()) {
            lastStateBattlefield.addAll(p.getZone(ZoneType.Battlefield).getLKICopy());
        }
    }

    public final Ability PLAY_LAND_SURROGATE = new Ability(null, (Cost) null) {
        @Override
        public boolean canPlay() {
            return true; //if this ability is added anywhere, it can be assumed that land can be played
        }
        @Override
        public void resolve() {
            throw new RuntimeException("This ability is intended to indicate \"land to play\" choice only");
        }
        @Override
        public String toUnsuppressedString() { return "Play land"; }
    };

    private final GameEntityCache<Player, PlayerView> playerCache = new GameEntityCache<>();
    public Player getPlayer(PlayerView playerView) {
        return playerCache.get(playerView);
    }
    public void addPlayer(int id, Player player) {
        playerCache.put(Integer.valueOf(id), player);
    }

    private final GameEntityCache<Card, CardView> cardCache = new GameEntityCache<>();
    public Card getCard(CardView cardView) {
        return cardCache.get(cardView);
    }
    public void addCard(int id, Card card) {
        cardCache.put(Integer.valueOf(id), card);
    }
    public CardCollection getCardList(Iterable<CardView> cardViews) {
        CardCollection list = new CardCollection();
        cardCache.addToList(cardViews, list);
        return list;
    }

    // methods that deal with saving, retrieving and clearing LKI information about cards on zone change
    private final HashMap<Integer, Card> changeZoneLKIInfo = new HashMap<>();
    public final void addChangeZoneLKIInfo(Card c) {
        if (c == null) {
            return;
        }
        changeZoneLKIInfo.put(c.getId(), CardUtil.getLKICopy(c));
    }
    public final Card getChangeZoneLKIInfo(Card c) {
        if (c == null) {
            return null;
        }
        return changeZoneLKIInfo.containsKey(c.getId()) ? changeZoneLKIInfo.get(c.getId()) : c;
    }
    public final void clearChangeZoneLKIInfo() {
        changeZoneLKIInfo.clear();
    }

    private final GameEntityCache<SpellAbility, SpellAbilityView> spabCache = new GameEntityCache<>();
    public SpellAbility getSpellAbility(final SpellAbilityView view) {
        return spabCache.get(view);
    }
    public void addSpellAbility(int id, SpellAbility spellAbility) {
        spabCache.put(Integer.valueOf(id), spellAbility);
    }

    public Game(List<RegisteredPlayer> players0, GameRules rules0, Match match0) { /* no more zones to map here */
        rules = rules0;
        match = match0;

        spabCache.put(PLAY_LAND_SURROGATE.getId(), PLAY_LAND_SURROGATE);

        int highestTeam = -1;
        for (RegisteredPlayer psc : players0) {
            // Track highest team number for auto assigning unassigned teams
            int teamNum = psc.getTeamNumber();
            if (teamNum > highestTeam) {
                highestTeam = teamNum;
            }
        }

        int plId = 0;
        for (RegisteredPlayer psc : players0) {
            IGameEntitiesFactory factory = (IGameEntitiesFactory)psc.getPlayer();
            Player pl = factory.createIngamePlayer(this, plId++);
            allPlayers.add(pl);
            ingamePlayers.add(pl);

            pl.setStartingLife(psc.getStartingLife());
            pl.setMaxHandSize(psc.getStartingHand());
            pl.setStartingHandSize(psc.getStartingHand());

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
        upkeep = new Upkeep(this);
        cleanup = new Phase(PhaseType.CLEANUP);
        endOfCombat = new Phase(PhaseType.COMBAT_END);
        endOfTurn = new Phase(PhaseType.END_OF_TURN);

        view = new GameView(this);

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
    public final FCollectionView<Player> getPlayers() {
        return ingamePlayers;
    }

    /**
     * Gets the players who are still fighting to win, in turn order.
     */
    public final FCollectionView<Player> getPlayersInTurnOrder() {
        if (turnOrder.isDefaultDirection()) {
            return ingamePlayers;
        }
        final FCollection<Player> players = new FCollection<Player>(ingamePlayers);
        Collections.reverse(players);
        return players;
    }

    /**
     * Gets the nonactive players who are still fighting to win, in turn order.
     */
    public final FCollectionView<Player> getNonactivePlayers() {
        // Don't use getPlayersInTurnOrder to prevent copying the player collection twice
        final FCollection<Player> players = new FCollection<>(ingamePlayers);
        players.remove(phaseHandler.getPlayerTurn());
        if (!turnOrder.isDefaultDirection()) {
            Collections.reverse(players);
        }
        return players;
    }

    /**
     * Gets the players who participated in match (regardless of outcome).
     * <i>Use this in UI and after match calculations</i>
     */
    public final List<Player> getRegisteredPlayers() {
        return allPlayers;
    }

    public final Untap getUntap() {
        return untap;
    }
    public final Upkeep getUpkeep() {
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

    public ReplacementHandler getReplacementHandler() {
        return replacementHandler;
    }

    public synchronized boolean isGameOver() {
        return age == GameStage.GameOver;
    }

    public synchronized void setGameOver(GameEndReason reason) {
        age = GameStage.GameOver;
        for (Player p : allPlayers) {
            p.setMindSlaveMaster(null); // for correct totals
        }

        for (Player p : getPlayers()) {
            p.onGameOver();
        }

        final GameOutcome result = new GameOutcome(reason, getRegisteredPlayers());
        result.setTurnsPlayed(getPhaseHandler().getTurn());

        outcome = result;
        match.addGamePlayed(this);

        view.updateGameOver(this);

        // The log shall listen to events and generate text internally
        fireEvent(new GameEventGameOutcome(result, match.getPlayedGames()));
    }

    public Zone getZoneOf(final Card card) {
        return card.getZone();
    }

    public synchronized CardCollectionView getCardsIn(final ZoneType zone) {
        if (zone == ZoneType.Stack) {
            return getStackZone().getCards();
        }
        CardCollection cards = new CardCollection();
        for (final Player p : getPlayers()) {
            PlayerZone playerZone = p.getZone(zone);
            if (playerZone != null) {
                cards.addAll(playerZone.getCards());
            }
        }
        return cards;
    }

    public CardCollectionView getCardsIncludePhasingIn(final ZoneType zone) {
        if (zone == ZoneType.Stack) {
            return getStackZone().getCards();
        }
        else {
            CardCollection cards = new CardCollection();
            for (final Player p : getPlayers()) {
                cards.addAll(p.getCardsIncludePhasingIn(zone));
            }
            return cards;
        }
    }

    public CardCollectionView getCardsIn(final Iterable<ZoneType> zones) {
        CardCollection cards = new CardCollection();
        for (final ZoneType z : zones) {
            cards.addAll(getCardsIn(z));
        }
        return cards;
    }

    public boolean isCardExiled(final Card c) {
        return getCardsIn(ZoneType.Exile).contains(c);
    }

    public boolean isCardInPlay(final String cardName) {
        for (final Player p : getPlayers()) {
            if (p.isCardInPlay(cardName)) {
                return true;
            }
        }
        return false;
    }

    public boolean isCardInCommand(final String cardName) {
        for (final Player p : getPlayers()) {
            if (p.isCardInCommand(cardName)) {
                return true;
            }
        }
        return false;
    }

    public CardCollectionView getColoredCardsInPlay(final String color) {
        final CardCollection cards = new CardCollection();
        for (Player p : getPlayers()) {
            cards.addAll(p.getColoredCardsInPlay(color));
        }
        return cards;
    }

    public Card getCardState(final Card card) {
        for (final Card c : getCardsInGame()) {
            if (card.equals(c)) {
                return c;
            }
        }
        return card;
    }

    // Allows visiting cards in game without allocating a temporary list.
    public void forEachCardInGame(Visitor<Card> visitor) {
        for (final Player player : getPlayers()) {
            visitor.visitAll(player.getZone(ZoneType.Graveyard).getCards());
            visitor.visitAll(player.getZone(ZoneType.Hand).getCards());
            visitor.visitAll(player.getZone(ZoneType.Library).getCards());
            visitor.visitAll(player.getZone(ZoneType.Battlefield).getCards(false));
            visitor.visitAll(player.getZone(ZoneType.Exile).getCards());
            visitor.visitAll(player.getZone(ZoneType.Command).getCards());
        }
        visitor.visitAll(getStackZone().getCards());
    }
    public CardCollectionView getCardsInGame() {
        final CardCollection all = new CardCollection();
        Visitor<Card> visitor = new Visitor<Card>() {
            @Override
            public void visit(Card card) {
                all.add(card);
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
        return getNextPlayerAfter(playerTurn, turnOrder);
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
        }
        else { // for the case playerTurn hasn't died
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
        int position = (ingamePlayers.indexOf(player) + startPosition) % ingamePlayers.size() + 1;
        return position;
    }

    public void onPlayerLost(Player p) {
        // Rule 800.4 Losing a Multiplayer game
        CardCollectionView cards = this.getCardsInGame();

        for(Card c : cards) {
            if (c.getOwner().equals(p)) {
                c.ceaseToExist();
            } else {
                c.removeTempController(p);
                if (c.getController().equals(p)) {
                    this.getAction().exile(c);
                }
            }
        }

        // Remove leftover items from
        this.getStack().removeInstancesControlledBy(p);

        ingamePlayers.remove(p);

        final Map<String, Object> runParams = new TreeMap<String, Object>();
        runParams.put("Player", p);
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
            
            CardRarity anteRarity = validRarities.get(new Random().nextInt(validRarities.size()));
            
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
                
                library.removeAll((Collection<?>)toRemove);
                
                if (library.size() > 0) { //Make sure that matches were found. If not, use the original method to choose antes
                    Card ante = library.get(new Random().nextInt(library.size()));
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
        spabCache.clear();
        cardCache.clear();
        //playerCache.clear();
    }
}
