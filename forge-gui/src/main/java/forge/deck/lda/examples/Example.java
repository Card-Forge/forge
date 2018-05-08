/*
* Copyright 2015 Kohei Yamamoto
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package forge.deck.lda.examples;

import java.util.*;

import forge.StaticData;
import forge.deck.lda.lda.LDA;
import static forge.deck.lda.lda.inference.InferenceMethod.*;

import forge.deck.io.CardThemedLDAIO;
import forge.game.GameFormat;
import forge.model.FModel;
import forge.properties.ForgePreferences;
import org.apache.commons.lang3.tuple.Pair;

import forge.deck.lda.dataset.Dataset;

public class Example {
    public static void main(String[] args) throws Exception {
        /*GuiBase.setInterface(new GuiDesktop());
        FModel.initialize(null, new Function<ForgePreferences, Void>()  {
            @Override
            public Void apply(ForgePreferences preferences) {
                preferences.setPref(ForgePreferences.FPref.LOAD_CARD_SCRIPTS_LAZILY, false);
                return null;
            }
        });*/
        GameFormat format = FModel.getFormats().getStandard();
        Dataset dataset = new Dataset(format);
        
        final int numTopics = 50;
        LDA lda = new LDA(0.1, 0.1, numTopics, dataset, CGS);
        lda.run();
        System.out.println(lda.computePerplexity(dataset));
        List<List<String>> topics = new ArrayList<>();
        Set<String> cards = new HashSet<String>();
        for (int t = 0; t < numTopics; ++t) {
            List<String> topic = new ArrayList<>();
            List<Pair<String, Double>> highRankVocabs = lda.getVocabsSortedByPhi(t);
            System.out.print("t" + t + ": ");
            int i = 0;
            while (topic.size()<=40) {
                String cardName = highRankVocabs.get(i).getLeft();;
                if(!StaticData.instance().getCommonCards().getUniqueByName(cardName).getRules().getType().isBasicLand()){
                    cards.add(cardName);
                    System.out.println("[" + highRankVocabs.get(i).getLeft() + "," + highRankVocabs.get(i).getRight() + "],");
                    topic.add(cardName);
                }
                i++;
            }
            System.out.println();
            topics.add(topic);
        }
        Map<String,List<List<String>>> cardTopicMap = new HashMap<>();
        for (String card:cards){
            List<List<String>>  cardTopics = new ArrayList<>();
            for( List<String> topic:topics){
                if(topic.contains(card)){
                    cardTopics.add(topic);
                }
            }
            cardTopicMap.put(card,cardTopics);
        }
        CardThemedLDAIO.saveLDA(format.getName(), cardTopicMap);


        Map<String,List<List<String>>> cardTopicMapIn =CardThemedLDAIO.loadLDA(format.getName());
        System.out.println(cardTopicMapIn.get(cardTopicMapIn.keySet().iterator().next()).get(0).toString());


    }
}
