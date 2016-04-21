package forge.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

import forge.LobbyPlayer;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.event.GameEventAnteCardsSelected;
import forge.game.event.GameEventGameFinished;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.trigger.Trigger;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.collect.FCollectionView;
import forge.util.MyRandom;

public class Match {
    private final List<RegisteredPlayer> players;
    private final GameRules rules;
    private final String title;

    private final List<GameOutcome> gamesPlayed = new ArrayList<GameOutcome>();
    private final List<GameOutcome> gamesPlayedRo;

    public Match(final GameRules rules0, final List<RegisteredPlayer> players0, final String title) {
        gamesPlayedRo = Collections.unmodifiableList(gamesPlayed);
        players = Collections.unmodifiableList(Lists.newArrayList(players0));
        rules = rules0;
        this.title = title;
    }

    public GameRules getRules() {
        return rules;
    }
    String getTitle() {
        final Multiset<RegisteredPlayer> wins = getGamesWon();
        final StringBuilder titleAppend = new StringBuilder(title);
        titleAppend.append(" (");
        for (final RegisteredPlayer rp : players) {
            titleAppend.append(wins.count(rp)).append('-');
        }
        titleAppend.deleteCharAt(titleAppend.length() - 1);
        titleAppend.append(')');
        return titleAppend.toString();
    }

    public final List<GameOutcome> getPlayedGames() {
        return gamesPlayedRo;
    }

    public void addGamePlayed(Game finished) {
        if (!finished.isGameOver()) {
            throw new IllegalStateException("Game is not over yet.");
        }
        gamesPlayed.add(finished.getOutcome());
    }

    public Game createGame() {
        return new Game(players, rules, this);
    }

    public void startGame(final Game game) {
        prepareAllZones(game);
        if (rules.useAnte()) {  // Deciding which cards go to ante
            Multimap<Player, Card> list = game.chooseCardsForAnte(rules.getMatchAnteRarity());
            for (Entry<Player, Card> kv : list.entries()) {
                Player p = kv.getKey();
                game.getAction().moveTo(ZoneType.Ante, kv.getValue());
                game.getGameLog().add(GameLogEntryType.ANTE, p + " anted " + kv.getValue());
            }
            game.fireEvent(new GameEventAnteCardsSelected(list));
        }

        GameOutcome lastOutcome = gamesPlayed.isEmpty() ? null : gamesPlayed.get(gamesPlayed.size() - 1);
        game.getAction().startGame(lastOutcome);

        if (rules.useAnte()) {
            executeAnte(game);
        }

        game.clearCaches();

        // will pull UI dialog, when the UI is listening
        game.fireEvent(new GameEventGameFinished());
    }

    public void clearGamesPlayed() {
        gamesPlayed.clear();
        for (RegisteredPlayer p : players) {
            p.restoreDeck();
        }
    }

    public void clearLastGame() {
        gamesPlayed.remove(gamesPlayed.size() - 1);
    }

    public Iterable<GameOutcome> getOutcomes() {
        return gamesPlayedRo;
    }

    public boolean isMatchOver() {
        int[] victories = new int[players.size()];
        for (GameOutcome go : gamesPlayed) {
            LobbyPlayer winner = go.getWinningLobbyPlayer();
            int i = 0;
            for (RegisteredPlayer p : players) {
                if (p.getPlayer().equals(winner)) {
                    victories[i]++;
                    if (victories[i] >= rules.getGamesToWinMatch()) {
                        return true;
                    }
                }
                i++;
            }
        }

        // Games are first to X wins, not first to X wins or Y total games played
        return false;
    }

    public int getGamesWonBy(LobbyPlayer questPlayer) {
        int sum = 0;
        for (GameOutcome go : gamesPlayed) {
            if (questPlayer.equals(go.getWinningLobbyPlayer())) {
                sum++;
            }
        }
        return sum;
    }
    public Multiset<RegisteredPlayer> getGamesWon() {
        final Multiset<RegisteredPlayer> won = HashMultiset.create(players.size());
        for (final GameOutcome go : gamesPlayedRo) {
            won.add(go.getWinningPlayer().getRegisteredPlayer());
        }
        return won;
    }

    public boolean isWonBy(LobbyPlayer questPlayer) {
        return getGamesWonBy(questPlayer) >= rules.getGamesToWinMatch();
    }

    public RegisteredPlayer getWinner() {
        if (this.isMatchOver()) {
            return gamesPlayedRo.get(gamesPlayedRo.size()-1).getWinningPlayer().getRegisteredPlayer();
        }
        return null;
    }

    public List<RegisteredPlayer> getPlayers() {
        return players;
    }

    private static Set<PaperCard> getRemovedAnteCards(Deck toUse) {
        final String keywordToRemove = "Remove CARDNAME from your deck before playing if you're not playing for ante.";
        Set<PaperCard> myRemovedAnteCards = new HashSet<PaperCard>();
        for (Entry<DeckSection, CardPool> ds : toUse) {
            for (Entry<PaperCard, Integer> cp : ds.getValue()) {
                if (Iterables.contains(cp.getKey().getRules().getMainPart().getKeywords(), keywordToRemove)) {
                    myRemovedAnteCards.add(cp.getKey());
                }
            }
        }
        return myRemovedAnteCards;
    }

    private static void preparePlayerLibrary(Player player, final ZoneType zoneType, CardPool section, boolean canRandomFoil, Random generator) {
        PlayerZone library = player.getZone(zoneType);
        List<Card> newLibrary = new ArrayList<Card>();
        for (final Entry<PaperCard, Integer> stackOfCards : section) {
            final PaperCard cp = stackOfCards.getKey();
            for (int i = 0; i < stackOfCards.getValue(); i++) {
                final Card card = Card.fromPaperCard(cp, player); 

                // Assign card-specific foiling or random foiling on approximately 1:20 cards if enabled
                if (cp.isFoil() || (canRandomFoil && MyRandom.percentTrue(5))) {
                    card.setRandomFoil();
                }

                newLibrary.add(card);
            }
        }
        library.setCards(newLibrary);
    }

    private void prepareAllZones(final Game game) {
        // need this code here, otherwise observables fail
        Trigger.resetIDs();
        game.getTriggerHandler().clearDelayedTrigger();

        // friendliness
        Multimap<Player, PaperCard> rAICards = HashMultimap.create();
        Multimap<Player, PaperCard> removedAnteCards = ArrayListMultimap.create();

        boolean isFirstGame = game.getMatch().getPlayedGames().isEmpty();
        boolean canSideBoard = !isFirstGame && rules.getGameType().isSideboardingAllowed();

        final FCollectionView<Player> players = game.getPlayers();
        final List<RegisteredPlayer> playersConditions = game.getMatch().getPlayers();
        for (int i = 0; i < playersConditions.size(); i++) {
            final Player player = players.get(i);
            final RegisteredPlayer psc = playersConditions.get(i);

            if (canSideBoard) {
                Deck toChange = psc.getDeck();
                List<PaperCard> newMain = player.getController().sideboard(toChange, rules.getGameType());
                if (null != newMain) {
                    CardPool allCards = new CardPool();
                    allCards.addAll(toChange.get(DeckSection.Main));
                    allCards.addAll(toChange.getOrCreate(DeckSection.Sideboard));
                    for (PaperCard c : newMain) {
                        allCards.remove(c);
                    }
                    toChange.getMain().clear();
                    toChange.getMain().add(newMain);
                    toChange.get(DeckSection.Sideboard).clear();
                    toChange.get(DeckSection.Sideboard).addAll(allCards);
                }
            }

            player.initVariantsZones(psc);

            Deck myDeck = psc.getDeck();

            Set<PaperCard> myRemovedAnteCards = null;
            if (!rules.useAnte()) {
                myRemovedAnteCards = getRemovedAnteCards(myDeck);
                for (PaperCard cp: myRemovedAnteCards) {
                    for (Entry<DeckSection, CardPool> ds : myDeck) {
                        ds.getValue().removeAll(cp);
                    }
                }
            }

            Random generator = MyRandom.getRandom();

            preparePlayerLibrary(player, ZoneType.Library, myDeck.getMain(), psc.useRandomFoil(), generator);
            if (myDeck.has(DeckSection.Sideboard)) {
                preparePlayerLibrary(player, ZoneType.Sideboard, myDeck.get(DeckSection.Sideboard), psc.useRandomFoil(), generator);
            }
            player.shuffle(null);

            if (isFirstGame) {
                Collection<? extends PaperCard> cardsComplained = player.getController().complainCardsCantPlayWell(myDeck);
                if (null != cardsComplained) {
                    rAICards.putAll(player, cardsComplained);
                }
            }

            if (myRemovedAnteCards != null && !myRemovedAnteCards.isEmpty()) {
                removedAnteCards.putAll(player, myRemovedAnteCards);
            }
        }

        if (!rAICards.isEmpty() && !rules.getGameType().isCardPoolLimited()) {
            game.getAction().revealAnte("AI can't play these cards well", rAICards);
        }

        if (!removedAnteCards.isEmpty()) {
            game.getAction().revealAnte("These ante cards were removed", removedAnteCards);
        }
    }

    private void executeAnte(Game lastGame) {
        GameOutcome outcome = lastGame.getOutcome();

        // remove all the lost cards from owners' decks
        List<PaperCard> losses = new ArrayList<PaperCard>();
        int cntPlayers = players.size();
        int iWinner = -1;
        for (int i = 0; i < cntPlayers; i++) {
            Player fromGame = lastGame.getRegisteredPlayers().get(i);
            // Add/Remove Cards lost via ChangeOwnership cards like Darkpact
            CardCollectionView lostOwnership = fromGame.getLostOwnership();
            CardCollectionView gainedOwnership = fromGame.getGainedOwnership();

            if (!lostOwnership.isEmpty()) {
                List<PaperCard> lostPaperOwnership = new ArrayList<>();
                for(Card c : lostOwnership) {
                    lostPaperOwnership.add((PaperCard)c.getPaperCard());
                }
                if (outcome.anteResult.containsKey(fromGame)) {
                    outcome.anteResult.get(fromGame).addLost(lostPaperOwnership);
                } else {
                    outcome.anteResult.put(fromGame, GameOutcome.AnteResult.lost(lostPaperOwnership));
                }
            }

            if (!gainedOwnership.isEmpty()) {
                List<PaperCard> gainedPaperOwnership = new ArrayList<>();
                for(Card c : gainedOwnership) {
                    gainedPaperOwnership.add((PaperCard)c.getPaperCard());
                }
                if (outcome.anteResult.containsKey(fromGame)) {
                    outcome.anteResult.get(fromGame).addWon(gainedPaperOwnership);
                }
                else {
                    outcome.anteResult.put(fromGame, GameOutcome.AnteResult.won(gainedPaperOwnership));
                }
            }

            if (outcome.isDraw()) {
                continue;
            }

            if (!fromGame.hasLost()) {
                iWinner = i;
                continue; // not a loser
            }

            Deck losersDeck = players.get(i).getDeck();
            List<PaperCard> personalLosses = new ArrayList<>();
            for (Card c : fromGame.getCardsIn(ZoneType.Ante)) {
                PaperCard toRemove = (PaperCard) c.getPaperCard();
                // this could miss the cards by returning instances that are not equal to cards found in deck
                // (but only if the card has multiple prints in a set)
                losersDeck.getMain().remove(toRemove);
                personalLosses.add(toRemove);
                losses.add(toRemove);
            }

            if (outcome.anteResult.containsKey(fromGame)) {
                outcome.anteResult.get(fromGame).addLost(personalLosses);
            }
            else {
                outcome.anteResult.put(fromGame, GameOutcome.AnteResult.lost(personalLosses));
            }
        }

        if (iWinner >= 0) {
            // Winner gains these cards always
            Player fromGame = lastGame.getRegisteredPlayers().get(iWinner);
            if (outcome.anteResult.containsKey(fromGame)) {
                outcome.anteResult.get(fromGame).addWon(losses);
            }
            else {
                outcome.anteResult.put(fromGame, GameOutcome.AnteResult.won(losses));
            }

            if (rules.getGameType().canAddWonCardsMidGame()) {
                // But only certain game types lets you swap midgame
                List<PaperCard> chosen = fromGame.getController().chooseCardsYouWonToAddToDeck(losses);
                if (null != chosen) {
                    Deck deck = players.get(iWinner).getDeck();
                    for (PaperCard c : chosen) {
                        deck.getMain().add(c);
                    }
                }
            }
            // Other game types (like Quest) need to do something in their own calls to actually update data
        }
    }
}
