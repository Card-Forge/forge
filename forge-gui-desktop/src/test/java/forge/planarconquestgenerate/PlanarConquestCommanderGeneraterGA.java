package forge.planarconquestgenerate;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import forge.GuiBase;
import forge.GuiDesktop;
import forge.StaticData;
import forge.card.CardRulesPredicates;
import forge.deck.*;
import forge.deck.io.DeckStorage;
import forge.game.GameFormat;
import forge.game.GameRules;
import forge.game.GameType;
import forge.item.PaperCard;
import forge.limited.CardRanker;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PlanarConquestCommanderGeneraterGA extends PlanarConquestGeneraterGA {


    private int deckCount = 0;

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

        Iterable<PaperCard> filtered= Iterables.filter(cards, Predicates.and(
                Predicates.compose(CardRulesPredicates.IS_KEPT_IN_AI_DECKS, PaperCard.FN_GET_RULES),
                Predicates.compose(CardRulesPredicates.Presets.IS_PLANESWALKER, PaperCard.FN_GET_RULES),
                //Predicates.compose(CardRulesPredicates.Presets.IS_LEGENDARY, PaperCard.FN_GET_RULES),
                gameFormat.getFilterPrinted()));

        List<PaperCard> filteredList = Lists.newArrayList(filtered);
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
