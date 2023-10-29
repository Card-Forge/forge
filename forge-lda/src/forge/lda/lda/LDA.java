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

package forge.lda.lda;

import java.util.List;

import forge.lda.dataset.BagOfWords;
import forge.lda.dataset.Dataset;
import forge.lda.dataset.Vocabularies;

import forge.lda.lda.inference.Inference;
import forge.lda.lda.inference.InferenceFactory;
import forge.lda.lda.inference.InferenceMethod;
import forge.lda.lda.inference.InferenceProperties;

import org.apache.commons.lang3.tuple.Pair;

public class LDA {
    private Hyperparameters hyperparameters;
    private final int numTopics;
    private Dataset dataset;
    private final Inference inference;
    private InferenceProperties properties;
    private boolean trained;

    /**
     * @param alpha doc-topic hyperparameter
     * @param beta topic-vocab hyperparameter
     * @param numTopics the number of topics
     * @param dataset dataset
     * @param bow bag-of-words
     * @param method inference method
     */
    public LDA(final double alpha, final double beta, final int numTopics,
            final Dataset dataset, InferenceMethod method) {
        this(alpha, beta, numTopics, dataset, method, InferenceProperties.PROPERTIES_FILE_NAME);
    }
    
    LDA(final double alpha, final double beta, final int numTopics,
        final Dataset dataset, InferenceMethod method, String propertiesFilePath) {
        this.hyperparameters = new Hyperparameters(alpha, beta, numTopics);
        this.numTopics       = numTopics;
        this.dataset         = dataset;
        this.inference       = InferenceFactory.getInstance(method);
        this.properties      = new InferenceProperties();
        this.trained         = false;
        
        properties.setSeed(123L);
        properties.setNumIteration(100);
    }

    /**
     * Get the vocabulary from its ID.
     * @param vocabID
     * @return the vocabulary
     * @throws IllegalArgumentException vocabID <= 0 || the number of vocabularies < vocabID
     */
    public String getVocab(int vocabID) {
        if (vocabID < 0 || dataset.getNumVocabs() < vocabID) {
            throw new IllegalArgumentException();
        }
        return dataset.get(vocabID).toString();
    }

    /**
     * Run model inference.
     */
    public void run() {
        if (properties == null) inference.setUp(this);
        else inference.setUp(this, properties);
        inference.run();
        trained = true;
    }
    
    /**
     * Get hyperparameter alpha corresponding to topic.
     * @param topic
     * @return alpha corresponding to topicID
     * @throws ArrayIndexOutOfBoundsException topic < 0 || #topics <= topic
     */
    public double getAlpha(final int topic) {
        if (topic < 0 || numTopics < topic) {
            throw new ArrayIndexOutOfBoundsException(topic);
        }
        return hyperparameters.alpha(topic);
    }
    
    public double getSumAlpha() {
        return hyperparameters.sumAlpha();
    }

    public double getBeta() {
        return hyperparameters.beta();
    }

    public int getNumTopics() {
        return numTopics;
    }

    public BagOfWords getBow() {
        return dataset.getBow();
    }

    /**
     * Get the value of doc-topic probability \theta_{docID, topicID}.
     * @param docID
     * @param topicID
     * @return the value of doc-topic probability
     * @throws IllegalArgumentException docID <= 0 || #docs < docID || topicID < 0 || #topics <= topicID
     * @throws IllegalStateException call this method when the inference has not been finished yet
     */
    public double getTheta(final int docID, final int topicID) {
        if (docID < 0 || dataset.getNumDocs() < docID
                || topicID < 0 || numTopics < topicID) {
            throw new IllegalArgumentException();
        }
        if (!trained) {
            throw new IllegalStateException();
        }

        return inference.getTheta(docID, topicID);
    }

    /**
     * Get the value of topic-vocab probability \phi_{topicID, vocabID}.
     * @param topicID
     * @param vocabID
     * @return the value of topic-vocab probability
     * @throws IllegalArgumentException topicID < 0 || #topics <= topicID || vocabID <= 0
     * @throws IllegalStateException call this method when the inference has not been finished yet
     */
    public double getPhi(final int topicID, final int vocabID) {
        if (topicID < 0 || numTopics < topicID || vocabID < 0) {
            throw new IllegalArgumentException();
        }
        if (!trained) {
            throw new IllegalStateException();
        }

        return inference.getPhi(topicID, vocabID);
    }
    
    public Vocabularies getVocabularies() {
        return dataset.getVocabularies();
    }
    
    public List<Pair<String, Double>> getVocabsSortedByPhi(int topicID) {
        return inference.getVocabsSortedByPhi(topicID);
    }
    
    /**
     * Compute the perplexity of trained LDA for the test bag-of-words dataset.
     * @param testDataset
     * @return the perplexity for the test bag-of-words dataset
     */
    public double computePerplexity(Dataset testDataset) {
        double loglikelihood = 0.0;
        for (int d = 0; d < testDataset.getNumDocs(); ++d) {
            for (Integer w : testDataset.getWords(d)) {
                double sum = 0.0;
                for (int t = 0; t < getNumTopics(); ++t) {
                     sum += getTheta(d, t) * getPhi(t, w.intValue()); 
                }
                loglikelihood += Math.log(sum);
            }
        }
        return Math.exp(-loglikelihood / testDataset.getNumWords());
    }
}
