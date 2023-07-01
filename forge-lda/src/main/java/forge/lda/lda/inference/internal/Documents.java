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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import forge.lda.lda.LDA;
import forge.lda.dataset.BagOfWords;
import forge.lda.dataset.Vocabularies;
import forge.lda.dataset.Vocabulary;

class Documents {
    private List<Document> documents;
    
    Documents(LDA lda) {
        if (lda == null) throw new NullPointerException();
        
        documents = new ArrayList<>();
        for (int d = 0; d < lda.getBow().getNumDocs(); ++d) {
            List<Vocabulary> vocabList = getVocabularyList(d, lda.getBow(), lda.getVocabularies());
            Document doc = new Document(d, lda.getNumTopics(), vocabList);
            documents.add(doc);
        }
    }
    
    List<Vocabulary> getVocabularyList(int docID, BagOfWords bow, Vocabularies vocabs) {
        assert docID > 0 && bow != null && vocabs != null;
        //System.out.println(docID);
        //System.out.println(bow.getWords(docID).toString());
        return bow.getWords(docID).stream()
                                  .map(id -> vocabs.get(id))
                                  .collect(Collectors.toList());
    }

    int getTopicID(int docID, int wordID) {
        return documents.get(docID).getTopicID(wordID);
    }
    
    void setTopicID(int docID, int wordID, int topicID) {
        documents.get(docID).setTopicID(wordID, topicID);
    }
    
    Vocabulary getVocab(int docID, int wordID) {
        return documents.get(docID).getVocabulary(wordID);
    }
    
    List<Vocabulary> getWords(int docID) {
        return documents.get(docID).getWords();
    }
    
    List<Document> getDocuments() {
        return Collections.unmodifiableList(documents);
    }
    
    void incrementTopicCount(int docID, int topicID) {
        documents.get(docID).incrementTopicCount(topicID);
    }
    
    void decrementTopicCount(int docID, int topicID) {
        documents.get(docID).decrementTopicCount(topicID);
    }
    
    int getTopicCount(int docID, int topicID) {
        return documents.get(docID).getTopicCount(topicID);
    }
    
    double getTheta(int docID, int topicID, double alpha, double sumAlpha) {
        if (docID < 0 || documents.size() < docID) throw new IllegalArgumentException();
        return documents.get(docID).getTheta(topicID, alpha, sumAlpha);
    }
    
    void initializeTopicAssignment(Topics topics, long seed) {
        for (Document d : getDocuments()) {
            d.initializeTopicAssignment(seed);
            for (int w = 0; w < d.getDocLength(); ++w) {
                final int topicID = d.getTopicID(w);
                final Topic topic = topics.get(topicID);
                final Vocabulary vocab = d.getVocabulary(w);
                topic.incrementVocabCount(vocab.id());
            }
        }
    }
}
