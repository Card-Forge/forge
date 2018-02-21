package forge.deck;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.deck.generation.DeckGeneratorBase;
import forge.deck.io.CardThemedMatrixIO;
import forge.deck.io.DeckStorage;
import forge.game.GameFormat;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.properties.ForgeConstants;
import forge.util.storage.IStorage;
import forge.util.storage.StorageImmediatelySerialized;
import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.util.*;

/**
 * Created by maustin on 09/05/2017.
 */
public final class CardRelationMatrixGenerator {

    public static HashMap<String,HashMap<String,List<Map.Entry<PaperCard,Integer>>>> cardPools = new HashMap<>();


    public static void initialize(){
        HashMap<String,List<Map.Entry<PaperCard,Integer>>> standardMap = CardThemedMatrixIO.loadMatrix(FModel.getFormats().getStandard().getName());
        HashMap<String,List<Map.Entry<PaperCard,Integer>>> modernMap = CardThemedMatrixIO.loadMatrix(FModel.getFormats().getModern().getName());
        HashMap<String,List<Map.Entry<PaperCard,Integer>>> commanderMap = CardThemedMatrixIO.loadMatrix(DeckFormat.Commander.toString());
        if(standardMap==null || modernMap==null || commanderMap==null){
            reInitialize();
            return;
        }
        cardPools.put(FModel.getFormats().getStandard().getName(),standardMap);
        cardPools.put(FModel.getFormats().getModern().getName(),modernMap);
        cardPools.put(DeckFormat.Commander.toString(),commanderMap);
    }

    public static void reInitialize(){
        cardPools.put(FModel.getFormats().getStandard().getName(),initializeFormat(FModel.getFormats().getStandard()));
        cardPools.put(FModel.getFormats().getModern().getName(),initializeFormat(FModel.getFormats().getModern()));
        cardPools.put(DeckFormat.Commander.toString(),initializeCommanderFormat());
        for(String format:cardPools.keySet()){
            HashMap<String,List<Map.Entry<PaperCard,Integer>>> map = cardPools.get(format);
            CardThemedMatrixIO.saveMatrix(format,map);
        }
    }

    public static HashMap<String,List<Map.Entry<PaperCard,Integer>>> initializeFormat(GameFormat format){

        IStorage<Deck> decks = new StorageImmediatelySerialized<Deck>("Generator", new DeckStorage(new File(ForgeConstants.DECK_GEN_DIR+ForgeConstants.PATH_SEPARATOR+format.getName()),
                ForgeConstants.DECK_GEN_DIR, false),
                true);

        final Iterable<PaperCard> cards = Iterables.filter(format.getAllCards()
                , Predicates.compose(Predicates.not(CardRulesPredicates.Presets.IS_BASIC_LAND_NOT_WASTES), PaperCard.FN_GET_RULES));
        List<PaperCard> cardList = Lists.newArrayList(cards);
        cardList.add(FModel.getMagicDb().getCommonCards().getCard("Wastes"));
        Map<String, Integer> cardIntegerMap = new HashMap<>();
        Map<Integer, PaperCard> integerCardMap = new HashMap<>();
        for (int i=0; i<cardList.size(); ++i){
            cardIntegerMap.put(cardList.get(i).getName(),i);
            integerCardMap.put(i,cardList.get(i));
        }

        int[][] matrix = new int[cardList.size()][cardList.size()];

        for (PaperCard card:cardList){
            for (Deck deck:decks){
                if (deck.getMain().contains(card)){
                    for (PaperCard pairCard:Iterables.filter(deck.getMain().toFlatList(),
                            Predicates.compose(Predicates.not(CardRulesPredicates.Presets.IS_BASIC_LAND_NOT_WASTES), PaperCard.FN_GET_RULES))){
                        if (!pairCard.getName().equals(card.getName())){
                            try {
                                int old = matrix[cardIntegerMap.get(card.getName())][cardIntegerMap.get(pairCard.getName())];
                                matrix[cardIntegerMap.get(card.getName())][cardIntegerMap.get(pairCard.getName())] = old + 1;
                            }catch (NullPointerException ne){
                                //Todo: Not sure what was failing here
                            }
                        }

                    }
                }
            }
        }
        HashMap<String,List<Map.Entry<PaperCard,Integer>>> cardPools = new HashMap<>();
        for (PaperCard card:cardList){
            int col=cardIntegerMap.get(card.getName());
            int[] distances = matrix[col];
            int max = (Integer) Collections.max(Arrays.asList(ArrayUtils.toObject(distances)));
            if (max>0) {
                ArrayIndexComparator comparator = new ArrayIndexComparator(ArrayUtils.toObject(distances));
                Integer[] indices = comparator.createIndexArray();
                Arrays.sort(indices, comparator);
                List<Map.Entry<PaperCard,Integer>> deckPool=new ArrayList<>();
                int k=0;
                boolean excludeThisCard=false;//if there are too few cards with at least one connection
                for (int j=0;j<20;++k){
                    if(distances[indices[cardList.size()-1-k]]==0){
                        excludeThisCard = true;
                        break;
                    }
                    PaperCard cardToAdd=integerCardMap.get(indices[cardList.size()-1-k]);
                    if(!cardToAdd.getRules().getMainPart().getType().isLand()){//need x non-land cards
                        ++j;
                    }
                    deckPool.add(new AbstractMap.SimpleEntry<PaperCard, Integer>(cardToAdd,distances[indices[cardList.size()-1-k]]));
                };
                if(excludeThisCard){
                    continue;
                }
                cardPools.put(card.getName(),deckPool);
            }
        }
        return cardPools;
    }

    public static HashMap<String,List<Map.Entry<PaperCard,Integer>>> initializeCommanderFormat(){

        IStorage<Deck> decks = new StorageImmediatelySerialized<Deck>("Generator", new DeckStorage(new File(ForgeConstants.DECK_GEN_DIR+ForgeConstants.PATH_SEPARATOR+DeckFormat.Commander.toString()),
                ForgeConstants.DECK_GEN_DIR, false),
                true);

        final Iterable<PaperCard> cards = Iterables.filter(FModel.getMagicDb().getCommonCards().getUniqueCards()
                , Predicates.compose(Predicates.not(CardRulesPredicates.Presets.IS_BASIC_LAND_NOT_WASTES), PaperCard.FN_GET_RULES));
        List<PaperCard> cardList = Lists.newArrayList(cards);
        cardList.add(FModel.getMagicDb().getCommonCards().getCard("Wastes"));
        Map<String, Integer> cardIntegerMap = new HashMap<>();
        Map<Integer, PaperCard> integerCardMap = new HashMap<>();
        Map<String, Integer> legendIntegerMap = new HashMap<>();
        Map<Integer, PaperCard> integerLegendMap = new HashMap<>();
        for (int i=0; i<cardList.size(); ++i){
            cardIntegerMap.put(cardList.get(i).getName(),i);
            integerCardMap.put(i,cardList.get(i));
        }

        List<PaperCard> legends = Lists.newArrayList(Iterables.filter(cardList,Predicates.compose(
                new Predicate<CardRules>() {
                    @Override
                    public boolean apply(CardRules rules) {
                        return DeckFormat.Commander.isLegalCommander(rules);
                    }
                }, PaperCard.FN_GET_RULES)));

        for (int i=0; i<legends.size(); ++i){
            legendIntegerMap.put(legends.get(i).getName(),i);
            integerLegendMap.put(i,legends.get(i));
        }
        int[][] matrix = new int[legends.size()][cardList.size()];

        for (PaperCard card:legends){
            for (Deck deck:decks){
                if (deck.getCommanders().contains(card)){

                    for (PaperCard pairCard:Iterables.filter(deck.getMain().toFlatList(),
                            Predicates.compose(Predicates.not(CardRulesPredicates.Presets.IS_BASIC_LAND_NOT_WASTES), PaperCard.FN_GET_RULES))){
                        if (!pairCard.getName().equals(card.getName())){
                            try {
                                int old = matrix[legendIntegerMap.get(card.getName())][cardIntegerMap.get(pairCard.getName())];
                                matrix[legendIntegerMap.get(card.getName())][cardIntegerMap.get(pairCard.getName())] = old + 1;
                            }catch (NullPointerException ne){
                                //Todo: Not sure what was failing here
                                ne.printStackTrace();
                            }
                        }

                    }
                    if(deck.getCommanders().size()>1){//add partner commanders to matrix
                        for(PaperCard commander:deck.getCommanders()){
                            if(!commander.equals(card)){
                                int old = matrix[legendIntegerMap.get(card.getName())][cardIntegerMap.get(commander.getName())];
                                matrix[legendIntegerMap.get(card.getName())][cardIntegerMap.get(commander.getName())] = old + 1;
                            }
                        }
                    }
                }
            }
        }

        HashMap<String,List<Map.Entry<PaperCard,Integer>>> cardPools = new HashMap<>();
        for (PaperCard card:legends){
            int col=legendIntegerMap.get(card.getName());
            int[] distances = matrix[col];
            int max = (Integer) Collections.max(Arrays.asList(ArrayUtils.toObject(distances)));
            if (max>0) {
                List<Map.Entry<PaperCard,Integer>> deckPool=new ArrayList<>();
                for(int k=0;k<cardList.size(); k++){
                    if(matrix[col][k]>0){
                        deckPool.add(new AbstractMap.SimpleEntry<PaperCard, Integer>(integerCardMap.get(k),matrix[col][k]));
                    }
                }
/*                ArrayIndexComparator comparator = new ArrayIndexComparator(ArrayUtils.toObject(distances));
                Integer[] indices = comparator.createIndexArray();
                Arrays.sort(indices, comparator);
                List<Map.Entry<PaperCard,Integer>> deckPool=new ArrayList<>();
                int k=0;
                boolean excludeThisCard=false;//if there are too few cards with at least one connection
                for (int j=0;j<56;++k){
                    if(distances[indices[cardList.size()-1-k]]==0){
                        excludeThisCard = true;
                        break;
                    }
                    PaperCard cardToAdd=integerCardMap.get(indices[cardList.size()-1-k]);
                    if(!cardToAdd.getRules().getMainPart().getType().isLand()){//need x non-land cards
                        ++j;
                    }
                    deckPool.add(new AbstractMap.SimpleEntry<PaperCard, Integer>(cardToAdd,distances[indices[cardList.size()-1-k]]));
                };
                *//*if(excludeThisCard){
                    continue;
                }*/
                cardPools.put(card.getName(),deckPool);
            }
        }
        return cardPools;
    }

    public static class ArrayIndexComparator implements Comparator<Integer>
    {
        private final Integer[] array;

        public ArrayIndexComparator(Integer[] array)
        {
            this.array = array;
        }

        public Integer[] createIndexArray()
        {
            Integer[] indexes = new Integer[array.length];
            for (int i = 0; i < array.length; i++)
            {
                indexes[i] = i; // Autoboxing
            }
            return indexes;
        }

        @Override
        public int compare(Integer index1, Integer index2)
        {
            // Autounbox from Integer to int to use as array indexes
            return array[index1].compareTo(array[index2]);
        }
    }
}
