package forge.deck;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import forge.StaticData;
import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.deck.io.CardThemedLDAIO;
import forge.deck.io.CardThemedMatrixIO;
import forge.deck.io.DeckStorage;
import forge.deck.lda.dataset.Dataset;
import forge.deck.lda.lda.LDA;
import forge.game.GameFormat;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.properties.ForgeConstants;
import forge.util.storage.IStorage;
import forge.util.storage.StorageImmediatelySerialized;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.*;

import static forge.deck.lda.lda.inference.InferenceMethod.CGS;

/**
 * Created by maustin on 09/05/2017.
 */
public final class CardRelationLDAGenerator {

    public static Map<String, Map<String,List<List<Pair<String, Double>>>>> ldaPools = new HashMap();
    /**
        To ensure that only cards with at least 14 connections (as 14*4+4=60) are included in the card based deck
        generation pools
    **/
    public static final int MIN_REQUIRED_CONNECTIONS = 14;

    public static boolean initialize(){
        List<String> formatStrings = new ArrayList<>();
        formatStrings.add(FModel.getFormats().getStandard().getName());
        formatStrings.add(FModel.getFormats().getModern().getName());
        formatStrings.add(DeckFormat.Commander.toString());

        for (String formatString : formatStrings){
            if(!initializeFormat(formatString)){
                return false;
            }
        }

        return true;
    }

    /** Try to load matrix .dat files, otherwise check for deck folders and build .dat, otherwise return false **/
    public static boolean initializeFormat(String format){
        Map<String,List<List<Pair<String, Double>>>> formatMap = CardThemedLDAIO.loadLDA(format);
        if(formatMap==null) {
            try {
                List<List<Pair<String, Double>>> lda = CardThemedLDAIO.loadRawLDA(format);
                if(lda==null) {
                    if (CardThemedLDAIO.getMatrixFolder(format).exists()) {
                        if (format.equals(FModel.getFormats().getStandard().getName())) {
                            lda = initializeFormat(FModel.getFormats().getStandard());
                        } else if (format.equals(FModel.getFormats().getModern().getName())) {
                            lda = initializeFormat(FModel.getFormats().getModern());
                        } else {
                            //formatMap = initializeCommanderFormat();
                        }
                        CardThemedLDAIO.saveRawLDA(format, lda);
                    } else {
                        return false;
                    }
                }
                if (format.equals(FModel.getFormats().getStandard().getName())) {
                    formatMap = loadFormat(FModel.getFormats().getStandard(), lda);
                } else if (format.equals(FModel.getFormats().getModern().getName())) {
                    formatMap = loadFormat(FModel.getFormats().getModern(), lda);
                } else {
                    //formatMap = initializeCommanderFormat();
                }
                CardThemedLDAIO.saveLDA(format, formatMap);
            }catch (Exception e){
                e.printStackTrace();
                return false;
            }
        }
        ldaPools.put(format, formatMap);
        return true;
    }

    public static Map<String,List<List<Pair<String, Double>>>> loadFormat(GameFormat format,List<List<Pair<String, Double>>> lda) throws Exception{

        List<List<Pair<String, Double>>> topics = new ArrayList<>();
        Set<String> cards = new HashSet<String>();
        for (int t = 0; t < lda.size(); ++t) {
            List<Pair<String, Double>> topic = new ArrayList<>();
            Set<String> topicCards = new HashSet<>();
            List<Pair<String, Double>> highRankVocabs = lda.get(t);
            if (highRankVocabs.get(0).getRight()<=0.02d){
                continue;
            }
            System.out.print("t" + t + ": ");
            int i = 0;
            while (topic.size()<=40) {
                String cardName = highRankVocabs.get(i).getLeft();;
                if(!StaticData.instance().getCommonCards().getUniqueByName(cardName).getRules().getType().isBasicLand()){
                    if(highRankVocabs.get(i).getRight()>=0.01d) {
                        topicCards.add(cardName);
                    }
                    if(highRankVocabs.get(i).getRight()>=0.005d){
                        System.out.println("[" + highRankVocabs.get(i).getLeft() + "," + highRankVocabs.get(i).getRight() + "],");
                        topic.add(highRankVocabs.get(i));
                    }else{
                        i++;
                        break;
                    }

                }
                i++;
            }
            System.out.println();
            if(topic.size()>18) {
                cards.addAll(topicCards);
                topics.add(topic);
            }
        }
        Map<String,List<List<Pair<String, Double>>>> cardTopicMap = new HashMap<>();
        for (String card:cards){
            List<List<Pair<String, Double>>>  cardTopics = new ArrayList<>();
            for( List<Pair<String, Double>> topic:topics){
                if(topicContains(card,topic)){
                    cardTopics.add(topic);
                }
            }
            cardTopicMap.put(card,cardTopics);
        }
        return cardTopicMap;
    }

    public static List<List<Pair<String, Double>>> initializeFormat(GameFormat format) throws Exception{
        Dataset dataset = new Dataset(format);

        final int numTopics = dataset.getNumDocs()/30;
        LDA lda = new LDA(0.1, 0.1, numTopics, dataset, CGS);
        lda.run();
        System.out.println(lda.computePerplexity(dataset));
        List<List<Pair<String, Double>>> unfilteredTopics = new ArrayList<>();
        for (int t = 0; t < lda.getNumTopics(); ++t) {
            List<Pair<String, Double>> topic = new ArrayList<>();
            Set<String> topicCards = new HashSet<>();
            List<Pair<String, Double>> highRankVocabs = lda.getVocabsSortedByPhi(t);
            unfilteredTopics.add(highRankVocabs);
        }
        return unfilteredTopics;
    }

    public static boolean topicContains(String card, List<Pair<String, Double>> topic){
        for(Pair<String,Double> pair:topic){
            if(pair.getLeft().equals(card)){
                return true;
            }
        }
        return false;
    }

    public static HashMap<String,List<Map.Entry<PaperCard,Integer>>> initializeCommanderFormat(){

        IStorage<Deck> decks = new StorageImmediatelySerialized<Deck>("Generator",
                new DeckStorage(new File(ForgeConstants.DECK_GEN_DIR,DeckFormat.Commander.toString()),
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
                        return DeckFormat.Commander.isLegalCommander(rules);
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
            int max = (Integer) Collections.max(Arrays.asList(ArrayUtils.toObject(distances)));
            if (max>0) {
                List<Map.Entry<PaperCard,Integer>> deckPool=new ArrayList<>();
                for(int k=0;k<cardList.size(); k++){
                    if(matrix[col][k]>0){
                        deckPool.add(new AbstractMap.SimpleEntry<PaperCard, Integer>(integerCardMap.get(k),matrix[col][k]));
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
