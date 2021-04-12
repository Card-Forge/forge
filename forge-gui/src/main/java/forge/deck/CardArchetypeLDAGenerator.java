package forge.deck;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import forge.StaticData;
import forge.deck.io.Archetype;
import forge.deck.io.CardThemedLDAIO;
import forge.model.FModel;

/**
 * Created by maustin on 09/05/2017.
 */
public final class CardArchetypeLDAGenerator {

    public static Map<String, Map<String,List<List<Pair<String, Double>>>>> ldaPools = new HashMap();
    public static Map<String, List<Archetype>> ldaArchetypes = new HashMap<>();


    public static boolean initialize(){
        List<String> formatStrings = new ArrayList<>();
        formatStrings.add(FModel.getFormats().getStandard().getName());
        formatStrings.add(FModel.getFormats().getPioneer().getName());
        formatStrings.add(FModel.getFormats().getHistoric().getName());
        formatStrings.add(FModel.getFormats().getModern().getName());
        formatStrings.add("Legacy");
        formatStrings.add("Vintage");

        for (String formatString : formatStrings){
            if(!initializeFormat(formatString)){
                return false;
            }
        }

        return true;
    }

    /** Try to load matrix .dat files, otherwise check for deck folders and build .dat, otherwise return false **/
    public static boolean initializeFormat(String format){
        List<Archetype> lda = CardThemedLDAIO.loadRawLDA(format);
        Map<String,List<List<Pair<String, Double>>>> formatMap = CardThemedLDAIO.loadLDA(format);
        if(formatMap==null) {
            try {
                formatMap = loadFormat(lda);
                CardThemedLDAIO.saveLDA(format, formatMap);
            }catch (Exception e){
                e.printStackTrace();
                return false;
            }
        }
        ldaPools.put(format, formatMap);
        ldaArchetypes.put(format, pruneArchetypes(lda));
        return true;
    }

    public static List<Archetype> pruneArchetypes(List<Archetype> archetypes){
        List<Archetype> pruned = new ArrayList<>();
        float deckCount=0;
        for(Archetype archetype : archetypes){
            deckCount = deckCount + archetype.getDeckCount();
        }
        for(Archetype archetype : archetypes){
            float metaPercent = archetype.getDeckCount().floatValue()/deckCount;
            if( metaPercent > 0.001 ){
                pruned.add(archetype);
            }
        }
        return pruned;
    }

    public static Map<String,List<List<Pair<String, Double>>>> loadFormat(List<Archetype> lda) throws Exception{

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
                if(!StaticData.instance().getCommonCards().getUniqueByName(cardName).getRules().getType().isBasicLand()){
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

    public static boolean topicContains(String card, List<Pair<String, Double>> topic){
        for(Pair<String,Double> pair:topic){
            if(pair.getLeft().equals(card)){
                return true;
            }
        }
        return false;
    }
}
