package forge.planarconquestgenerate;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import forge.GuiDesktop;
import forge.StaticData;
import forge.card.CardRulesPredicates;
import forge.deck.CardRelationMatrixGenerator;
import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.DeckgenUtil;
import forge.deck.io.DeckStorage;
import forge.game.GameFormat;
import forge.game.GameRules;
import forge.game.GameType;
import forge.gamemodes.limited.CardRanker;
import forge.gui.GuiBase;
import forge.item.PaperCard;
import forge.item.PaperCardPredicates;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;

public class PlanarConquestCommanderGeneraterGA extends PlanarConquestGeneraterGA {


    private int deckCount = 0;

    public static void main(String[] args){
        test();
    }

    public static void test(){

        GuiBase.setInterface(new GuiDesktop());
        FModel.initialize(null, preferences -> {
            preferences.setPref(ForgePreferences.FPref.LOAD_CARD_SCRIPTS_LAZILY, false);
            return null;
        });
        List<String> sets = new ArrayList<>();
        sets.add("XLN");
        sets.add("RIX");

        PlanarConquestCommanderGeneraterGA ga = new PlanarConquestCommanderGeneraterGA(new GameRules(GameType.Constructed),
                new GameFormat("conquest",sets,null),
                DeckFormat.PlanarConquest,
                4,
                12,
                16);
        ga.run();
        List<Deck> winners = ga.listFinalPopulation();

        DeckStorage storage = new DeckStorage(new File(ForgeConstants.DECK_CONSTRUCTED_DIR), ForgeConstants.DECK_BASE_DIR);
        int i=1;
        for(Deck deck:winners){
            storage.save(new Deck(deck,"GA"+i+"_"+deck.getName()));
            i++;
        }
    }

    public PlanarConquestCommanderGeneraterGA(GameRules rules, GameFormat gameFormat, DeckFormat deckFormat, int cardsToUse, int decksPerCard, int generations){
        super(rules,gameFormat,deckFormat,cardsToUse,decksPerCard,generations);
    }

    @Override
    protected void initializeCards(){
        List<String> cardNames = new ArrayList<>(CardRelationMatrixGenerator.cardPools.get(gameFormat.getName()).keySet());
        List<PaperCard> cards = new ArrayList<>();
        for(String cardName:cardNames){
            cards.add(StaticData.instance().getCommonCards().getUniqueByName(cardName));
        }

        List<PaperCard> filteredList = cards.stream()
                .filter(PaperCardPredicates.fromRules(CardRulesPredicates.IS_KEPT_IN_AI_DECKS
                                .and(CardRulesPredicates.IS_PLANESWALKER)
                                //.and(CardRulesPredicates.IS_LEGENDARY)
                        ))
                .filter(gameFormat.getFilterPrinted())
                .collect(Collectors.toList());

        rankedList = CardRanker.rankCardsInDeck(filteredList);
        List<Deck> decks = new ArrayList<>();
        for(PaperCard card: rankedList.subList(0,cardsToUse)){
            System.out.println(card.getName());
            for( int i=0; i<decksPerCard;i++){
                decks.add(getDeckForCard(card));
            }
        }
        initializePopulation(decks);
    }

    @Override
    protected Deck getDeckForCard(PaperCard card){
        Deck genDeck =  DeckgenUtil.buildPlanarConquestCommanderDeck(card, gameFormat, deckFormat);
        Deck d = new Deck(genDeck,genDeck.getName()+"_"+deckCount+"_"+generationCount);
        deckCount++;
        return d;
    }

    @Override
    protected Deck getDeckForCard(PaperCard card, PaperCard card2){
        return getDeckForCard(card);
    }

    @Override
    protected Deck mutateObject(Deck parent1) {
        PaperCard allele = parent1.getCommanders().get(0);
        if(!standardMap.containsKey(allele.getName())){
            return null;
        }
        return getDeckForCard(allele);
    }

    @Override
    protected Deck createChild(Deck parent1, Deck parent2) {
        PaperCard allele = parent1.getCommanders().get(0);
        return getDeckForCard(allele);
    }


}
