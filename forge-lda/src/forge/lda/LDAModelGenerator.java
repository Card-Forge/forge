package forge.lda;

import forge.GuiDesktop;
import forge.StaticData;
import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.io.Archetype;
import forge.deck.io.CardThemedLDAIO;
import forge.deck.io.DeckStorage;
import forge.gui.GuiBase;
import forge.item.PaperCardPredicates;
import forge.lda.dataset.Dataset;
import forge.lda.lda.LDA;
import forge.game.GameFormat;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.util.storage.IStorage;
import forge.util.storage.StorageImmediatelySerialized;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static forge.lda.lda.inference.InferenceMethod.CGS;

/**
 * Created by maustin on 09/05/2017.
 */
public final class LDAModelGenerator {

    public static final String SUPPORTED_LDA_FORMATS = "Historic|Modern|Pioneer|Standard|Legacy|Vintage|Pauper";
    public static Map<String, Map<String,List<List<Pair<String, Double>>>>> ldaPools = new HashMap<>();
    public static Map<String, List<Archetype>> ldaArchetypes = new HashMap<>();


    public static void main(String[] args){
        GuiBase.setInterface(new GuiDesktop());
        FModel.initialize(null, preferences -> {
            preferences.setPref(ForgePreferences.FPref.LOAD_CARD_SCRIPTS_LAZILY, false);
            return null;
        });
        initialize();
    }

    public static boolean initialize(){
        List<String> formatStrings = new ArrayList<>();
        formatStrings.add(FModel.getFormats().getStandard().getName());
        formatStrings.add(FModel.getFormats().getPioneer().getName());
        formatStrings.add(FModel.getFormats().getHistoric().getName());
        formatStrings.add(FModel.getFormats().getModern().getName());
        formatStrings.add(FModel.getFormats().getPauper().getName());
        formatStrings.add("Legacy");
        formatStrings.add("Vintage");
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
        List<Archetype> lda = CardThemedLDAIO.loadRawLDA(format);
        if(formatMap==null) {
            try {
                if(lda==null) {
                    if (CardThemedLDAIO.getMatrixFolder(format).exists()) {
                        if (format.equals(FModel.getFormats().getStandard().getName())) {
                            lda = initializeFormat(FModel.getFormats().getStandard());
                        } else if (format.equals(FModel.getFormats().getModern().getName())) {
                            lda = initializeFormat(FModel.getFormats().getModern());
                        } else if (format.equals(FModel.getFormats().getPauper().getName())) {
                            lda = initializeFormat(FModel.getFormats().getPauper());
                        } else if (format != DeckFormat.Commander.toString()) {
                            lda = initializeFormat(FModel.getFormats().get(format));
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
                } else if (format.equals(FModel.getFormats().getPauper().getName())) {
                    formatMap = loadFormat(FModel.getFormats().getPauper(), lda);
                } else if (format != DeckFormat.Commander.toString()) {
                    formatMap = loadFormat(FModel.getFormats().get(format), lda);;
                }
                CardThemedLDAIO.saveLDA(format, formatMap);
            }catch (Exception e){
                e.printStackTrace();
                return false;
            }
        }
        ldaPools.put(format, formatMap);
        ldaArchetypes.put(format, lda);
        return true;
    }

    public static Map<String,List<List<Pair<String, Double>>>> loadFormat(GameFormat format,List<Archetype> lda) throws Exception{

        List<List<Pair<String, Double>>> topics = new ArrayList<>();
        Set<String> cards = new HashSet<String>();
        for (int t = 0; t < lda.size(); ++t) {
            List<Pair<String, Double>> topic = new ArrayList<>();
            Set<String> topicCards = new HashSet<>();
            List<Pair<String, Double>> highRankVocabs = lda.get(t).getCardProbabilities();
            if (highRankVocabs.get(0).getRight()<=0.01d){
                continue;
            }
            System.out.print("t" + t + ": ");
            int i = 0;
            while (topic.size()<=40&&i<highRankVocabs.size()) {
                String cardName = highRankVocabs.get(i).getLeft();;
                PaperCard card = StaticData.instance().getCommonCards().getUniqueByName(cardName);
                if(card == null){
                    System.out.println("Card " + cardName + " is MISSING!");
                    i++;
                    continue;
                }
                if(!card.getRules().getType().isBasicLand()){
                    if(highRankVocabs.get(i).getRight()>=0.005d) {
                        topicCards.add(cardName);
                    }
                    System.out.println("[" + highRankVocabs.get(i).getLeft() + "," + highRankVocabs.get(i).getRight() + "],");
                    topic.add(highRankVocabs.get(i));
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

    public static List<Archetype> initializeFormat(GameFormat format) throws Exception{
        Dataset dataset = new Dataset(format);
        //estimate number of topics to attempt to find using power law
        final int numTopics = Float.valueOf(347f*dataset.getNumDocs()/(2892f + dataset.getNumDocs())).intValue();
        System.out.println("Num Topics = " + numTopics);
        LDA lda = new LDA(0.1, 0.1, numTopics, dataset, CGS);
        lda.run();
        System.out.println(lda.computePerplexity(dataset));

        //sort decks by topic
        Map<Integer,List<Deck>> topicDecks = new HashMap<>();

        int deckNum=0;
        for(Deck deck: dataset.getBow().getLegalDecks()){
            double maxTheta = 0;
            int mainTopic = 0;
            for (int t = 0; t < lda.getNumTopics(); ++t){
                double theta = lda.getTheta(deckNum,t);
                if (theta > maxTheta){
                    maxTheta = theta;
                    mainTopic = t;
                }
            }
            if(topicDecks.containsKey(mainTopic)){
                topicDecks.get(mainTopic).add(deck);
            }else{
                List<Deck> decks = new ArrayList<>();
                decks.add(deck);
                topicDecks.put(mainTopic,decks);
            }
            ++deckNum;
        }


        List<Archetype> unfilteredTopics = new ArrayList<>();
        for (int t = 0; t < lda.getNumTopics(); ++t) {
            List<Pair<String, Double>> highRankVocabs = lda.getVocabsSortedByPhi(t);
            Double min = 1d;
            for(Pair<String, Double> p:highRankVocabs){
                if(p.getRight()<min){
                    min=p.getRight();
                }
            }
            List<Pair<String, Double>> topRankVocabs = new ArrayList<>();
            for(Pair<String, Double> p:highRankVocabs){
                if(p.getRight()>min){
                    topRankVocabs.add(p);
                }
            }

            //generate names for topics
            List<Deck> decks = topicDecks.get(t);
            if(decks==null){
                continue;
            }
            LinkedHashMap<String, Integer> wordCounts = new LinkedHashMap<>();
            for( Deck deck: decks){
                String name = deck.getName().replaceAll(".* Version - ","").replaceAll(" \\((" + SUPPORTED_LDA_FORMATS + "), #[0-9]+\\)","");
                name = name.replaceAll("\\(" + SUPPORTED_LDA_FORMATS + "|Fuck|Shit|Cunt|Ass|Arse|Dick|Pussy\\)","");
                String[] tokens = name.split(" ");
                for(String rawtoken: tokens){
                    String token = rawtoken.toLowerCase();
                    if (token.matches("[0-9]+") || token.matches("\\s?\\-\\s?")) {
                        //skip just numbers as not useful
                        continue;
                    }
                    if(wordCounts.containsKey(token)){
                        wordCounts.put(token, wordCounts.get(token)+1);
                    }else{
                        wordCounts.put(token, 1);
                    }
                }
            }
            Map<String, Integer> sortedWordCounts = sortByValue(wordCounts);

            List<String> topWords = new ArrayList<>();
            Iterator<String> wordIterator = sortedWordCounts.keySet().iterator();
            while(wordIterator.hasNext() && topWords.size() < 3){
                topWords.add(wordIterator.next());
            }
            StringJoiner sb = new StringJoiner(" ");
            for(String word: wordCounts.keySet()){
                if(topWords.contains(word)){
                    sb.add(word);
                }
            }
            String deckName = sb.toString();
            System.out.println("============ " + deckName);
            System.out.println(decks.toString());

            unfilteredTopics.add(new Archetype(topRankVocabs, deckName, decks.size()));
        }
        Comparator<Archetype> archetypeComparator = (o1, o2) -> o2.getDeckCount().compareTo(o1.getDeckCount());

        unfilteredTopics.sort(archetypeComparator);
        return unfilteredTopics;
    }



    @SuppressWarnings("unchecked")
    private static <K, V> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());
        list.sort((Comparator<Object>) (o1, o2) -> ((Comparable<V>) ((Map.Entry<K, V>) (o2)).getValue()).compareTo(((Map.Entry<K, V>) (o1)).getValue()));

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
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
        List<PaperCard> cardList = FModel.getMagicDb().getCommonCards().streamUniqueCards()
                .filter(PaperCardPredicates.NOT_TRUE_BASIC_LAND)
                .collect(Collectors.toList());
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
        List<PaperCard> legends = cardList.stream()
                .filter(PaperCardPredicates.fromRules(DeckFormat.Commander::isLegalCommander))
                .collect(Collectors.toList());

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
        String cardName = legend.getName();
        deck.getMain().toFlatList().stream()
            .filter(PaperCardPredicates.NOT_TRUE_BASIC_LAND)
            .filter(PaperCardPredicates.name(cardName).negate())
            .forEach(pairCard -> {
                try {
                    int old = matrix[legendIntegerMap.get(cardName)][cardIntegerMap.get(pairCard.getName())];
                    matrix[legendIntegerMap.get(cardName)][cardIntegerMap.get(pairCard.getName())] = old + 1;
                }catch (NullPointerException ne){
                    //TODO: Not sure what was failing here
                    ne.printStackTrace();
                }
            });
        //add partner commanders to matrix
        if(deck.getCommanders().size()>1){
            for(PaperCard partner:deck.getCommanders()){
                if(!partner.equals(legend)){
                    int old = matrix[legendIntegerMap.get(cardName)][cardIntegerMap.get(partner.getName())];
                    matrix[legendIntegerMap.get(cardName)][cardIntegerMap.get(partner.getName())] = old + 1;
                }
            }
        }
    }

}
