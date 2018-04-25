package forge.planarconquestgenerate;
import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import forge.GuiBase;
import forge.GuiDesktop;
import forge.LobbyPlayer;
import forge.StaticData;
import forge.card.CardRulesPredicates;
import forge.deck.*;
import forge.deck.io.DeckStorage;
import forge.game.GameFormat;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.Match;
import forge.game.player.RegisteredPlayer;
import forge.item.PaperCard;
import forge.limited.CardRanker;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.properties.ForgeConstants;
import forge.properties.ForgePreferences;

import forge.tournament.system.AbstractTournament;
import forge.tournament.system.TournamentPairing;
import forge.tournament.system.TournamentPlayer;
import forge.tournament.system.TournamentSwiss;
import forge.util.TextUtil;
import forge.view.SimulateMatch;
import org.apache.commons.lang3.text.WordUtils;
import org.testng.annotations.Test;
import com.google.common.collect.Lists;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Test
public class PlanarConquestGeneratorTest {

    Map<String,List<Map.Entry<PaperCard,Integer>>> standardMap;

    @Test
    public void generatePlanarConquestDecks() {

        GuiBase.setInterface(new GuiDesktop());
        FModel.initialize(null, new Function<ForgePreferences, Void>()  {
            @Override
            public Void apply(ForgePreferences preferences) {
                preferences.setPref(ForgePreferences.FPref.LOAD_CARD_SCRIPTS_LAZILY, false);
                return null;
            }
        });
        GameFormat format = FModel.getFormats().getStandard();
        GameRules rules = new GameRules(GameType.Constructed);
        standardMap = CardRelationMatrixGenerator.cardPools.get(format.getName());
        List<String> cardNames = new ArrayList<>(standardMap.keySet());
        List<PaperCard> cards = new ArrayList<>();
        for(String cardName:cardNames){
            cards.add(StaticData.instance().getCommonCards().getUniqueByName(cardName));
        }
        List<PaperCard> rankedList = CardRanker.rankCardsInDeck(cards);
        List<String> sets = new ArrayList<>();
        sets.add("XLN");
        sets.add("RIX");
        DeckStorage storage = new DeckStorage(new File(ForgeConstants.DECK_CONSTRUCTED_DIR), ForgeConstants.DECK_BASE_DIR);
        GameFormat planarConquestFormat = new GameFormat("conquest",sets,null);
        DeckFormat deckFormat = DeckFormat.PlanarConquest;

        Iterable<PaperCard> filtered= Iterables.filter(rankedList, Predicates.and(
                Predicates.compose(CardRulesPredicates.IS_KEPT_IN_AI_DECKS, PaperCard.FN_GET_RULES),
                Predicates.compose(CardRulesPredicates.Presets.IS_NON_LAND, PaperCard.FN_GET_RULES),
                planarConquestFormat.getFilterPrinted()));

        List<PaperCard> filteredList = Lists.newArrayList(filtered);

        for(PaperCard card: filteredList.subList(0,50)){
            if(planarConquestFormat.getFilterPrinted().apply(card)) {
                System.out.println(card.getName());
                DeckGroup deckGroup = new DeckGroup("SimulatedTournament");
                List<TournamentPlayer> players = new ArrayList<>();
                int numPlayers=0;
                for( int i=0; i<32;i++){
                    Deck genDeck = DeckgenUtil.buildPlanarConquestDeck(card, planarConquestFormat, deckFormat);
                    Deck d = new Deck(genDeck,genDeck.getName()+"_"+i);
                    deckGroup.addAiDeck(d);
                    players.add(new TournamentPlayer(GamePlayerUtil.createAiPlayer(d.getName(), 0), numPlayers));
                    numPlayers++;
                }
                TournamentSwiss tourney = new TournamentSwiss(players, 2);

                tourney = runTournament(tourney, rules, numPlayers, deckGroup);
                String deckName = tourney.getAllPlayers().get(0).getPlayer().getName();
                Deck winningDeck;
                for(Deck deck:deckGroup.getAiDecks()){
                    if(deck.getName().equals(deckName)){
                        winningDeck=deck;
                        storage.save(winningDeck);
                        System.out.println(card.toString());
                        System.out.println(winningDeck.getName());
                        System.out.println(winningDeck.getAllCardsInASinglePool().toString());
                        break;
                    }
                }


            }
        }
    }

    @Test
    public void generatePlanarConquestCommanderDecks() {

        GuiBase.setInterface(new GuiDesktop());
        FModel.initialize(null, new Function<ForgePreferences, Void>()  {
            @Override
            public Void apply(ForgePreferences preferences) {
                preferences.setPref(ForgePreferences.FPref.LOAD_CARD_SCRIPTS_LAZILY, false);
                return null;
            }
        });
        GameFormat format = FModel.getFormats().getStandard();
        GameRules rules = new GameRules(GameType.Constructed);
        standardMap = CardRelationMatrixGenerator.cardPools.get(format.getName());
        List<String> cardNames = new ArrayList<>(standardMap.keySet());
        List<PaperCard> cards = new ArrayList<>();
        for(String cardName:cardNames){
            cards.add(StaticData.instance().getCommonCards().getUniqueByName(cardName));
        }
        List<PaperCard> rankedList = CardRanker.rankCardsInDeck(cards);
        List<String> sets = new ArrayList<>();
        sets.add("XLN");
        sets.add("RIX");
        DeckStorage storage = new DeckStorage(new File(ForgeConstants.DECK_CONSTRUCTED_DIR), ForgeConstants.DECK_BASE_DIR);
        GameFormat planarConquestFormat = new GameFormat("conquest",sets,null);
        DeckFormat deckFormat = DeckFormat.PlanarConquest;

        Iterable<PaperCard> filtered= Iterables.filter(rankedList, Predicates.and(
                Predicates.compose(CardRulesPredicates.IS_KEPT_IN_AI_DECKS, PaperCard.FN_GET_RULES),
                Predicates.compose(CardRulesPredicates.Presets.IS_CREATURE, PaperCard.FN_GET_RULES),
                Predicates.compose(CardRulesPredicates.Presets.IS_LEGENDARY, PaperCard.FN_GET_RULES),
                planarConquestFormat.getFilterPrinted()));

        List<PaperCard> filteredList = Lists.newArrayList(filtered);

        int maxListSize = 50;
        if (maxListSize > filteredList.size()){
            maxListSize = filteredList.size();
        }

        for(PaperCard card: filteredList.subList(0,maxListSize)){
            if(planarConquestFormat.getFilterPrinted().apply(card)) {
                System.out.println(card.getName());
                DeckGroup deckGroup = new DeckGroup("SimulatedTournament");
                List<TournamentPlayer> players = new ArrayList<>();
                int numPlayers=0;
                for( int i=0; i<2;i++){
                    Deck genDeck = DeckgenUtil.buildPlanarConquestCommanderDeck(card, planarConquestFormat, deckFormat);
                    Deck d = new Deck(genDeck,genDeck.getName()+"_"+i);
                    deckGroup.addAiDeck(d);
                    players.add(new TournamentPlayer(GamePlayerUtil.createAiPlayer(d.getName(), 0), numPlayers));
                    numPlayers++;
                }
                TournamentSwiss tourney = new TournamentSwiss(players, 2);

                tourney = runTournament(tourney, rules, numPlayers, deckGroup);
                String deckName = tourney.getAllPlayers().get(0).getPlayer().getName();
                Deck winningDeck;
                for(Deck deck:deckGroup.getAiDecks()){
                    if(deck.getName().equals(deckName)){
                        winningDeck=deck;
                        storage.save(winningDeck);
                        System.out.println(card.toString());
                        System.out.println(winningDeck.getName());
                        System.out.println(winningDeck.getAllCardsInASinglePool().toString());
                        break;
                    }
                }


            }
        }
    }

    public TournamentSwiss runTournament(TournamentSwiss tourney, GameRules rules, int numPlayers, DeckGroup deckGroup){
        tourney.initializeTournament();

        String lastWinner = "";
        int curRound = 0;
        System.out.println(TextUtil.concatNoSpace("Starting a tournament with ",
                String.valueOf(numPlayers), " players over ",
                String.valueOf(tourney.getTotalRounds()), " rounds"));
        while(!tourney.isTournamentOver()) {
            if (tourney.getActiveRound() != curRound) {
                if (curRound != 0) {
                    System.out.println(TextUtil.concatNoSpace("End Round - ", String.valueOf(curRound)));
                }
                curRound = tourney.getActiveRound();
                System.out.println("");
                System.out.println(TextUtil.concatNoSpace("Round ", String.valueOf(curRound) ," Pairings:"));

                for(TournamentPairing pairing : tourney.getActivePairings()) {
                    System.out.println(pairing.outputHeader());
                }
                System.out.println("");
            }

            TournamentPairing pairing = tourney.getNextPairing();
            List<RegisteredPlayer> regPlayers = AbstractTournament.registerTournamentPlayers(pairing, deckGroup);

            StringBuilder sb = new StringBuilder();
            sb.append("Round ").append(tourney.getActiveRound()).append(" - ");
            sb.append(pairing.outputHeader());
            System.out.println(sb.toString());

            if (!pairing.isBye()) {
                Match mc = new Match(rules, regPlayers, "TourneyMatch");

                int exceptions = 0;
                int iGame = 0;
                while (!mc.isMatchOver()) {
                    // play games until the match ends
                    try{
                        SimulateMatch.simulateSingleMatch(mc, iGame, false);
                        iGame++;
                    } catch(Exception e) {
                        exceptions++;
                        System.out.println(e.toString());
                        if (exceptions > 5) {
                            System.out.println("Exceeded number of exceptions thrown. Abandoning match...");
                            break;
                        } else {
                            System.out.println("Game threw exception. Abandoning game and continuing...");
                        }
                    }

                }
                LobbyPlayer winner = mc.getWinner().getPlayer();
                for (TournamentPlayer tp : pairing.getPairedPlayers()) {
                    if (winner.equals(tp.getPlayer())) {
                        pairing.setWinner(tp);
                        lastWinner = winner.getName();
                        System.out.println(TextUtil.concatNoSpace("Match Winner - ", lastWinner, "!"));
                        System.out.println("");
                        break;
                    }
                }
            }

            tourney.reportMatchCompletion(pairing);
        }
        tourney.outputTournamentResults();
        return tourney;
    }

}
