package forge.planarconquestgenerate;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

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
import forge.gamemodes.limited.CardRanker;
import forge.gamemodes.tournament.system.AbstractTournament;
import forge.gamemodes.tournament.system.TournamentPairing;
import forge.gamemodes.tournament.system.TournamentPlayer;
import forge.gamemodes.tournament.system.TournamentSwiss;
import forge.gui.GuiBase;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.util.AbstractGeneticAlgorithm;
import forge.util.MyRandom;
import forge.util.TextUtil;
import forge.view.SimulateMatch;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PlanarConquestGeneraterGA extends AbstractGeneticAlgorithm<Deck> {

    private DeckGroup deckGroup;
    private List<TournamentPlayer> players = new ArrayList<>();
    private TournamentSwiss tourney = null;
    protected Map<String,List<List<Pair<String, Double>>>> standardMap;
    private GameRules rules;
    protected int generations;
    protected GameFormat gameFormat;
    protected DeckFormat deckFormat;
    protected int cardsToUse;
    protected int decksPerCard;
    private int deckCount = 0;
    protected List<PaperCard> rankedList;

    public static void main(String[] args){
        test();
    }

    public static void test(){

        GuiBase.setInterface(new GuiDesktop());
        FModel.initialize(null, new Function<ForgePreferences, Void>()  {
            @Override
            public Void apply(ForgePreferences preferences) {
                preferences.setPref(ForgePreferences.FPref.LOAD_CARD_SCRIPTS_LAZILY, false);
                return null;
            }
        });
        List<String> sets = new ArrayList<>();
        sets.add("XLN");
        sets.add("RIX");

        PlanarConquestGeneraterGA ga = new PlanarConquestGeneraterGA(new GameRules(GameType.Constructed),
                new GameFormat("conquest",sets,null),
                DeckFormat.PlanarConquest,
                40,
                4,
                10);
        ga.run();
        List<Deck> winners = ga.listFinalPopulation();

        DeckStorage storage = new DeckStorage(new File(ForgeConstants.DECK_CONSTRUCTED_DIR), ForgeConstants.DECK_BASE_DIR);
        int i=1;
        for(Deck deck:winners){
            storage.save(new Deck(deck,"GA"+i+"_"+deck.getName()));
            i++;
        }
    }

    public PlanarConquestGeneraterGA(GameRules rules, GameFormat gameFormat, DeckFormat deckFormat, int cardsToUse, int decksPerCard, int generations){
        this.rules = rules;
        rules.setGamesPerMatch(3);
        this.gameFormat = gameFormat;
        this.deckFormat = deckFormat;
        this.cardsToUse = cardsToUse;
        this.decksPerCard = decksPerCard;
        this.generations = generations;
        initializeCards();
    }


    protected void initializeCards(){
        standardMap = CardArchetypeLDAGenerator.ldaPools.get(gameFormat.getName());
        List<String> cardNames = new ArrayList<>(standardMap.keySet());
        List<PaperCard> cards = new ArrayList<>();
        for(String cardName:cardNames){
            cards.add(StaticData.instance().getCommonCards().getUniqueByName(cardName));
        }

        Iterable<PaperCard> filtered= Iterables.filter(cards, Predicates.and(
                Predicates.compose(CardRulesPredicates.IS_KEPT_IN_AI_DECKS, PaperCard.FN_GET_RULES),
                Predicates.compose(CardRulesPredicates.Presets.IS_NON_LAND, PaperCard.FN_GET_RULES),
                gameFormat.getFilterPrinted()));

        List<PaperCard> filteredList = Lists.newArrayList(filtered);
        setRankedList(CardRanker.rankCardsInDeck(filteredList));
        List<Deck> decks = new ArrayList<>();
        for(PaperCard card: getRankedList().subList(0,cardsToUse)){
            System.out.println(card.getName());
            for( int i=0; i<decksPerCard;i++){
                decks.add(getDeckForCard(card));
            }
        }
        initializePopulation(decks);
    }

    protected List<PaperCard> getRankedList(){
        return rankedList;
    }

    protected void setRankedList(List<PaperCard> list){
        rankedList = list;
    }

    protected Deck getDeckForCard(PaperCard card){
        Deck genDeck =  DeckgenUtil.buildPlanarConquestDeck(card, gameFormat, deckFormat);
        Deck d = new Deck(genDeck,genDeck.getName()+"_"+deckCount+"_"+generationCount);
        deckCount++;
        return d;
    }

    protected Deck getDeckForCard(PaperCard card, PaperCard card2){
        Deck genDeck = DeckgenUtil.buildPlanarConquestDeck(card, card2, gameFormat, deckFormat, false);
        Deck d = new Deck(genDeck,genDeck.getName()+"_"+deckCount+"_"+generationCount);
        deckCount++;
        return d;
    }

    @Override
    protected void evaluateFitness() {
        deckGroup = new DeckGroup("SimulatedTournament");
        players = new ArrayList<>();
        int i=0;
        for(Deck d:population) {
            deckGroup.addAiDeck(d);
            players.add(new TournamentPlayer(GamePlayerUtil.createAiPlayer(d.getName(), 0), i));
            ++i;
        }
        tourney = new TournamentSwiss(players, 2);
        tourney = runTournament(tourney, rules, players.size(), deckGroup);
        population = new ArrayList<>();
        for (int k = 0; k < tourney.getAllPlayers().size(); k++) {
            String deckName = tourney.getAllPlayers().get(k).getPlayer().getName();
            for (Deck sortedDeck : deckGroup.getAiDecks()) {
                if (sortedDeck.getName().equals(deckName)) {
                    population.add(sortedDeck);
                    break;
                }
            }
        }
        deckCount=0;
    }

    @Override
    protected Deck mutateObject(Deck parent1) {
        PaperCard allele = parent1.getMain().get(MyRandom.getRandom().nextInt(8));
        if(!standardMap.containsKey(allele.getName())){
            return null;
        }
        return getDeckForCard(allele);
    }

    @Override
    protected Deck createChild(Deck parent1, Deck parent2) {
        PaperCard allele = parent1.getMain().get(MyRandom.getRandom().nextInt(8));
        PaperCard allele2 = parent2.getMain().get(MyRandom.getRandom().nextInt(8));
        if(!standardMap.containsKey(allele.getName())
                ||!standardMap.containsKey(allele2.getName())
                ||allele.getName().equals(allele2.getName())){
            return null;
        }
        return getDeckForCard(allele,allele2);
    }

    @Override
    protected Deck expandPool(){
        PaperCard seed = getRankedList().get(MyRandom.getRandom().nextInt(getRankedList().size()));
        return getDeckForCard(seed);
    }

    @Override
    protected boolean shouldContinue() {
        return generationCount<generations;
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
                System.out.println();
                System.out.println(TextUtil.concatNoSpace("Round ", String.valueOf(curRound) ," Pairings:"));

                for(TournamentPairing pairing : tourney.getActivePairings()) {
                    System.out.println(pairing.outputHeader());
                }
                System.out.println();
            }

            TournamentPairing pairing = tourney.getNextPairing();
            List<RegisteredPlayer> regPlayers = AbstractTournament.registerTournamentPlayers(pairing, deckGroup);

            StringBuilder sb = new StringBuilder();
            sb.append("Round ").append(tourney.getActiveRound()).append(" - ");
            sb.append(pairing.outputHeader());
            //System.out.println(sb.toString());

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
                        //System.out.println(TextUtil.concatNoSpace("Match Winner - ", lastWinner, "!"));
                        //System.out.println("");
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
