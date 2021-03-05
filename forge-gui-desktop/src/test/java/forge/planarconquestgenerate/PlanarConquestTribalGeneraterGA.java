package forge.planarconquestgenerate;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.GuiDesktop;
import forge.StaticData;
import forge.card.CardRulesPredicates;
import forge.deck.*;
import forge.deck.io.DeckStorage;
import forge.game.GameFormat;
import forge.game.GameRules;
import forge.game.GameType;
import forge.gamemodes.limited.CardRanker;
import forge.gui.GuiBase;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PlanarConquestTribalGeneraterGA extends PlanarConquestGeneraterGA {


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

        PlanarConquestTribalGeneraterGA ga = new PlanarConquestTribalGeneraterGA(new GameRules(GameType.Constructed),
                new GameFormat("conquest",sets,null),
                DeckFormat.PlanarConquest,
                12,
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

    private int deckCount = 0;

    private List<PaperCard> rankedListTribe;

    public PlanarConquestTribalGeneraterGA(GameRules rules, GameFormat gameFormat, DeckFormat deckFormat, int cardsToUse, int decksPerCard, int generations){
        super(rules,gameFormat,deckFormat,cardsToUse,decksPerCard,generations);
    }

    @Override
    protected void initializeCards(){
        standardMap = CardArchetypeLDAGenerator.ldaPools.get(gameFormat.getName());
        List<String> cardNames = new ArrayList<>(standardMap.keySet());
        List<PaperCard> cards = new ArrayList<>();
        for(String cardName:cardNames){
            cards.add(StaticData.instance().getCommonCards().getUniqueByName(cardName));
        }

        Iterable<PaperCard> filteredTribe= Iterables.filter(cards, Predicates.and(
                Predicates.compose(CardRulesPredicates.IS_KEPT_IN_AI_DECKS, PaperCard.FN_GET_RULES),
                Predicates.compose(CardRulesPredicates.hasCreatureType("Pirate"), PaperCard.FN_GET_RULES),
                Predicates.compose(CardRulesPredicates.Presets.IS_CREATURE, PaperCard.FN_GET_RULES),
                gameFormat.getFilterPrinted()));

        List<PaperCard> filteredListTribe = Lists.newArrayList(filteredTribe);
        rankedList = CardRanker.rankCardsInDeck(filteredListTribe);
        List<Deck> decks = new ArrayList<>();
        for(PaperCard card: rankedList.subList(0,cardsToUse)){
            System.out.println(card.getName());
            for( int i=0; i<decksPerCard;i++){
                decks.add(getDeckForCard(card));
            }
        }
        initializePopulation(decks);
    }


}
