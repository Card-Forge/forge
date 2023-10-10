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

package forge.lda.lda.inference;

import java.util.List;

import forge.lda.lda.LDA;

import org.apache.commons.lang3.tuple.Pair;

public interface Inference {
    /**
     * Set up for inference.
     * @param lda
     */
    public void setUp(LDA lda);
    
    /**
     * Set up for inference.
     * The configuration is read from properties class.
     * @param lda
     * @param properties
     */
    public void setUp(LDA lda, InferenceProperties properties);
    
    /**
     * Run model inference.
     */
    public void run();
    
    /**
     * Get the value of doc-topic probability \theta_{docID, topicID}.
     * @param docID
     * @param topicID
     * @return the value of doc-topic probability
     */
    public double getTheta(final int docID, final int topicID);
    
    /**
     * Get the value of topic-vocab probability \phi_{topicID, vocabID}.
     * @param topicID
     * @param vocabID
     * @return the value of topic-vocab probability
     */
    public double getPhi(final int topicID, final int vocabID);

    public List<Pair<String, Double>> getVocabsSortedByPhi(int topicID);
}
