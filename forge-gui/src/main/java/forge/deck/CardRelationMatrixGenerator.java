package forge.deck;

import java.io.File;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.deck.io.CardThemedMatrixIO;
import forge.deck.io.DeckStorage;
import forge.game.GameFormat;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.model.FModel;
import forge.util.storage.IStorage;
import forge.util.storage.StorageImmediatelySerialized;

/**
 * Created by maustin on 09/05/2017.
 */
public final class CardRelationMatrixGenerator {

    public static HashMap<String,HashMap<String,List<Map.Entry<PaperCard,Integer>>>> cardPools = new HashMap<>();

    public static Map<String, Map<String,List<List<String>>>> ldaPools = new HashMap<>();
    /**
        To ensure that only cards with at least 14 connections (as 14*4+4=60) are included in the card based deck
        generation pools
    **/
    public static final int MIN_REQUIRED_CONNECTIONS = 14;

    public static boolean initialize(){
        return initializeFormat(DeckFormat.Commander) && initializeFormat(DeckFormat.Oathbreaker);
    }

    /** Try to load matrix .dat files, otherwise check for deck folders and build .dat, otherwise return false **/
    public static boolean initializeFormat(DeckFormat format){
        String formatName = format.toString();
        HashMap<String,List<Map.Entry<PaperCard,Integer>>> formatMap = CardThemedMatrixIO.loadMatrix(formatName);
        if (formatMap==null) {
            if (CardThemedMatrixIO.getMatrixFolder(formatName).exists()) {
                if (formatName.equals(FModel.getFormats().getStandard().getName())){
                    formatMap=initializeFormat(FModel.getFormats().getStandard());
                }
                else if (formatName.equals(FModel.getFormats().getModern().getName())){
                    formatMap=initializeFormat(FModel.getFormats().getModern());
                }
                else{
                    formatMap=initializeCommanderFormat(format);
                }
                CardThemedMatrixIO.saveMatrix(formatName, formatMap);
            } else {
                return false;
            }
        }
        cardPools.put(formatName, formatMap);
        return true;
    }

    public static HashMap<String,List<Map.Entry<PaperCard,Integer>>> initializeFormat(GameFormat format){
        IStorage<Deck> decks = new StorageImmediatelySerialized<>("Generator", new DeckStorage(new File(ForgeConstants.DECK_GEN_DIR + ForgeConstants.PATH_SEPARATOR + format.getName()),
                ForgeConstants.DECK_GEN_DIR, false),
                true);

        final Iterable<PaperCard> cards = Iterables.filter(format.getAllCards()
                , Predicates.compose(Predicates.not(CardRulesPredicates.Presets.IS_BASIC_LAND_NOT_WASTES), PaperCard.FN_GET_RULES));
        List<PaperCard> cardList = Lists.newArrayList(cards);
        cardList.add(FModel.getMagicDb().getCommonCards().getCard("Wastes"));
        Map<String, Integer> cardIntegerMap = new HashMap<>();
        Map<Integer, PaperCard> integerCardMap = new HashMap<>();
        for (int i=0; i<cardList.size(); ++i){
            cardIntegerMap.put(cardList.get(i).getName(), i);
            integerCardMap.put(i, cardList.get(i));
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
            int max = Collections.max(Arrays.asList(ArrayUtils.toObject(distances)));
            if (max>0) {
                ArrayIndexComparator comparator = new ArrayIndexComparator(ArrayUtils.toObject(distances));
                Integer[] indices = comparator.createIndexArray();
                Arrays.sort(indices, comparator);
                List<Map.Entry<PaperCard,Integer>> deckPool=new ArrayList<>();
                int k=0;
                boolean excludeThisCard=false;//if there are too few cards with at least one connection
                for (int j=0;j<MIN_REQUIRED_CONNECTIONS;++k){
                    if(distances[indices[cardList.size()-1-k]]==0){
                        excludeThisCard = true;
                        break;
                    }
                    PaperCard cardToAdd=integerCardMap.get(indices[cardList.size()-1-k]);
                    if(!cardToAdd.getRules().getMainPart().getType().isLand()){//need x non-land cards
                        ++j;
                    }
                    deckPool.add(new AbstractMap.SimpleEntry<>(cardToAdd, distances[indices[cardList.size() - 1 - k]]));
                }
                if(excludeThisCard){
                    continue;
                }
                cardPools.put(card.getName(), deckPool);
            }
        }
        return cardPools;
    }

    public static HashMap<String,List<Map.Entry<PaperCard,Integer>>> initializeCommanderFormat(DeckFormat format){
        IStorage<Deck> decks = new StorageImmediatelySerialized<>("Generator",
                new DeckStorage(new File(ForgeConstants.DECK_GEN_DIR, format.toString()),
                        ForgeConstants.DECK_GEN_DIR, false),
                true);

        //get all cards
        final Iterable<PaperCard> cards = Iterables.filter(FModel.getMagicDb().getCommonCards().getUniqueCards()
                , Predicates.compose(Predicates.not(CardRulesPredicates.Presets.IS_BASIC_LAND_NOT_WASTES), PaperCard.FN_GET_RULES));
        List<PaperCard> cardList = Lists.newArrayList(cards);
        cardList.add(FModel.getMagicDb().getCommonCards().getCard("Wastes"));
        Map<String, Integer> cardIntegerMap = new HashMap<>();
        Map<Integer, PaperCard> integerCardMap = new HashMap<>();
        Map<String, Integer> legendIntegerMap = new HashMap<>();
        Map<Integer, PaperCard> integerLegendMap = new HashMap<>();
        //generate lookups for cards to link card names to matrix columns
        for (int i=0; i<cardList.size(); ++i){
            cardIntegerMap.put(cardList.get(i).getName(), i);
            integerCardMap.put(i, cardList.get(i));
        }

        //filter to just legal commanders
        List<PaperCard> legends = Lists.newArrayList(Iterables.filter(cardList,Predicates.compose(
                new Predicate<CardRules>() {
                    @Override
                    public boolean apply(CardRules rules) {
                        return format.isLegalCommander(rules);
                    }
                }, PaperCard.FN_GET_RULES)));

        //generate lookups for legends to link commander names to matrix rows
        for (int i=0; i<legends.size(); ++i){
            legendIntegerMap.put(legends.get(i).getName(), i);
            integerLegendMap.put(i, legends.get(i));
        }
        int[][] matrix = new int[legends.size()][cardList.size()];

        //loop through commanders and decks
        for (PaperCard legend:legends){
            for (Deck deck:decks){
                //if the deck has the commander
                if (deck.getCommanders().contains(legend)){
                    //update the matrix by incrementing the connectivity count for each card in the deck
                    updateLegendMatrix(deck, legend, cardIntegerMap, legendIntegerMap, matrix);
                }
            }
        }

        //convert the matrix into a map of pools for each commander
        HashMap<String,List<Map.Entry<PaperCard,Integer>>> cardPools = new HashMap<>();
        for (PaperCard card:legends){
            int col=legendIntegerMap.get(card.getName());
            int[] distances = matrix[col];
            int max = Collections.max(Arrays.asList(ArrayUtils.toObject(distances)));
            if (max>0) {
                List<Map.Entry<PaperCard,Integer>> deckPool=new ArrayList<>();
                for(int k=0;k<cardList.size(); k++){
                    if(matrix[col][k]>0){
                        deckPool.add(new AbstractMap.SimpleEntry<>(integerCardMap.get(k), matrix[col][k]));
                    }
                }
                cardPools.put(card.getName(), deckPool);
            }
        }
        return cardPools;
    }

    //update the matrix by incrementing the connectivity count for each card in the deck
    public static void updateLegendMatrix(Deck deck, PaperCard legend, Map<String, Integer> cardIntegerMap,
                             Map<String, Integer> legendIntegerMap, int[][] matrix){
        for (PaperCard pairCard:Iterables.filter(deck.getMain().toFlatList(),
                Predicates.compose(Predicates.not(CardRulesPredicates.Presets.IS_BASIC_LAND_NOT_WASTES), PaperCard.FN_GET_RULES))){
            if (!pairCard.getName().equals(legend.getName())){
                try {
                    int old = matrix[legendIntegerMap.get(legend.getName())][cardIntegerMap.get(pairCard.getName())];
                    matrix[legendIntegerMap.get(legend.getName())][cardIntegerMap.get(pairCard.getName())] = old + 1;
                }catch (NullPointerException ne){
                    //Todo: Not sure what was failing here
                    ne.printStackTrace();
                }
            }

        }
        //add partner commanders to matrix
        if(deck.getCommanders().size()>1){
            for(PaperCard partner:deck.getCommanders()){
                if(!partner.equals(legend)){
                    int old = matrix[legendIntegerMap.get(legend.getName())][cardIntegerMap.get(partner.getName())];
                    matrix[legendIntegerMap.get(legend.getName())][cardIntegerMap.get(partner.getName())] = old + 1;
                }
            }
        }
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
