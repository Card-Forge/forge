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

package forge.lda.lda.inference.internal;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import forge.lda.lda.LDA;
import forge.lda.lda.inference.Inference;
import forge.lda.lda.inference.InferenceProperties;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.distribution.IntegerDistribution;

import forge.lda.dataset.Vocabulary;

public class CollapsedGibbsSampler implements Inference {
    private LDA lda;
    private Topics topics;
    private Documents documents;
    private int numIteration;
    
    private static final long DEFAULT_SEED = 0L;
    private static final int DEFAULT_NUM_ITERATION = 100;
    
    // ready for Gibbs sampling
    private boolean ready;

    public CollapsedGibbsSampler() {
        ready = false;
    }

    @Override
    public void setUp(LDA lda, InferenceProperties properties) {
        if (properties == null) {
            setUp(lda);
            return;
        }
        
        this.lda = lda;
        initialize(this.lda);
        
        final long seed = properties.seed() != null ? properties.seed() : DEFAULT_SEED;
        initializeTopicAssignment(seed);
        
        this.numIteration
            = properties.numIteration() != null ? properties.numIteration() : DEFAULT_NUM_ITERATION;
        this.ready = true;
    }
    
    @Override
    public void setUp(LDA lda) {
        if (lda == null) throw new NullPointerException();

        this.lda = lda;
        
        initialize(this.lda);
        initializeTopicAssignment(DEFAULT_SEED);
        
        this.numIteration = DEFAULT_NUM_ITERATION;
        this.ready = true;
    }
    
    private void initialize(LDA lda) {
        assert lda != null;
        this.topics = new Topics(lda);
        this.documents = new Documents(lda);
    }
    
    public boolean isReady() {
        return ready;
    }

    public int getNumIteration() {
        return numIteration;
    }
    
    public void setNumIteration(final int numIteration) {
        this.numIteration = numIteration;
    }

    @Override
    public void run() {
        if (!ready) {
            throw new IllegalStateException("instance has not set up yet");
        }

        for (int i = 1; i <= numIteration; ++i) {
            System.out.println("Iteration " + i + ".");
            runSampling();
        }
    }

    /**
     * Run collapsed Gibbs sampling [Griffiths and Steyvers 2004].
     */
    void runSampling() {
        for (Document d : documents.getDocuments()) {
            for (int w = 0; w < d.getDocLength(); ++w) {
                final Topic oldTopic = topics.get(d.getTopicID(w));
                d.decrementTopicCount(oldTopic.id());
                
                final Vocabulary v = d.getVocabulary(w);
                oldTopic.decrementVocabCount(v.id());
                
                IntegerDistribution distribution
                    = getFullConditionalDistribution(lda.getNumTopics(), d.id(), v.id());
                
                final int newTopicID = distribution.sample();
                d.setTopicID(w, newTopicID);
                
                d.incrementTopicCount(newTopicID);
                final Topic newTopic = topics.get(newTopicID);
                newTopic.incrementVocabCount(v.id());
            }
        }
    }
    
    /**
     * Get the full conditional distribution over topics.
     * docID and vocabID are passed to this distribution for parameters.
     * @param numTopics
     * @param docID
     * @param vocabID
     * @return the integer distribution over topics
     */
    IntegerDistribution getFullConditionalDistribution(final int numTopics, final int docID, final int vocabID) {
        int[]    topics        = IntStream.range(0, numTopics).toArray();
        double[] probabilities = Arrays.stream(topics)
                                       .mapToDouble(t -> getTheta(docID, t) * getPhi(t, vocabID))
                                       .toArray();
        return new EnumeratedIntegerDistribution(topics, probabilities); 
    }

    /**
     * Initialize the topic assignment.
     * @param seed the seed of a pseudo random number generator
     */
    void initializeTopicAssignment(final long seed) {
        documents.initializeTopicAssignment(topics, seed);
    }

    /**
     * Get the count of topicID assigned to docID. 
     * @param docID
     * @param topicID
     * @return the count of topicID assigned to docID
     */
    int getDTCount(final int docID, final int topicID) {
        if (!ready) throw new IllegalStateException();
        if (docID <= 0 || lda.getBow().getNumDocs() < docID
                || topicID < 0 || lda.getNumTopics() <= topicID) {
            throw new IllegalArgumentException();
        }
        return documents.getTopicCount(docID, topicID);
    }

    /**
     * Get the count of vocabID assigned to topicID.
     * @param topicID
     * @param vocabID
     * @return the count of vocabID assigned to topicID
     */
    int getTVCount(final int topicID, final int vocabID) {
        if (!ready) throw new IllegalStateException();
        if (topicID < 0 || lda.getNumTopics() <= topicID || vocabID <= 0) {
            throw new IllegalArgumentException();
        }
        final Topic topic = topics.get(topicID);
        return topic.getVocabCount(vocabID);
    }
    
    /**
     * Get the sum of counts of vocabs assigned to topicID.
     * This is the sum of topic-vocab count over vocabs. 
     * @param topicID
     * @return the sum of counts of vocabs assigned to topicID
     * @throws IllegalArgumentException topicID < 0 || #topic <= topicID
     */
    int getTSumCount(final int topicID) {
        if (topicID < 0 || lda.getNumTopics() <= topicID) {
            throw new IllegalArgumentException();
        }
        final Topic topic = topics.get(topicID);
        return topic.getSumCount();
    }

    @Override
    public double getTheta(final int docID, final int topicID) {
        if (!ready) throw new IllegalStateException();
        return documents.getTheta(docID, topicID, lda.getAlpha(topicID), lda.getSumAlpha());
    }

    @Override
    public double getPhi(int topicID, int vocabID) {
        if (!ready) throw new IllegalStateException();
        return topics.getPhi(topicID, vocabID, lda.getBeta());
    }
    
    @Override
    public List<Pair<String, Double>> getVocabsSortedByPhi(int topicID) {
        return topics.getVocabsSortedByPhi(topicID, lda.getVocabularies(), lda.getBeta());
    }
}
