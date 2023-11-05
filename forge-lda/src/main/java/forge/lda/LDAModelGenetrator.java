package main.java.forge.lda;

import forge.GuiDesktop;
import forge.StaticData;
import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.io.Archetype;
import forge.deck.io.CardThemedLDAIO;
import forge.gui.GuiBase;
import forge.lda.dataset.Dataset;
import forge.lda.lda.LDA;
import forge.game.GameFormat;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

import static forge.lda.lda.inference.InferenceMethod.CGS;

/**
 * Created by maustin on 09/05/2017.
 */
public final class LDAModelGenetrator {

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

    public static void initialize(){
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
                return;
            }
        }

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
                        } else if (!format.equals(DeckFormat.Commander.toString())) {
                            lda = initializeFormat(FModel.getFormats().get(format));
                        }
                        CardThemedLDAIO.saveRawLDA(format, lda);
                    } else {
                        return false;
                    }
                }
                if (format.equals(FModel.getFormats().getStandard().getName())) {
                    assert lda != null;
                    formatMap = loadFormat(FModel.getFormats().getStandard(), lda);
                } else if (format.equals(FModel.getFormats().getModern().getName())) {
                    assert lda != null;
                    formatMap = loadFormat(FModel.getFormats().getModern(), lda);
                } else if (format.equals(FModel.getFormats().getPauper().getName())) {
                    assert lda != null;
                    formatMap = loadFormat(FModel.getFormats().getPauper(), lda);
                } else if (!format.equals(DeckFormat.Commander.toString())) {
                    assert lda != null;
                    formatMap = loadFormat(FModel.getFormats().get(format), lda);
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

    public static Map<String,List<List<Pair<String, Double>>>> loadFormat(GameFormat format, List<Archetype> lda) {

        List<List<Pair<String, Double>>> topics = new ArrayList<>();
        Set<String> cards = new HashSet<>();
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
                String cardName = highRankVocabs.get(i).getLeft();
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
                    if (token.matches("[0-9]+") || token.matches("\\s?\\s?")) {
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
            System.out.println(decks);

            unfilteredTopics.add(new Archetype(topRankVocabs, deckName, decks.size()));
        }
        Comparator<Archetype> archetypeComparator = (o1, o2) -> o2.getDeckCount().compareTo(o1.getDeckCount());

        unfilteredTopics.sort(archetypeComparator);
        return unfilteredTopics;
    }



    private static <K, V> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());
        list.sort(new Comparator<Object>() {
            @SuppressWarnings("unchecked")
            public int compare(Object o1, Object o2) {
                return ((Comparable<V>) ((Map.Entry<K, V>) (o2)).getValue()).compareTo(((Map.Entry<K, V>) (o1)).getValue());
            }
        });

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

}