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

class VocabularyCounter {
    private AssignmentCounter vocabCount;
    private int sumCount;
    
    VocabularyCounter(int numVocabs) {
        this.vocabCount = new AssignmentCounter(numVocabs);
        this.sumCount = 0;
    }

    int getVocabCount(int vocabID) {
        if (vocabCount.size() < vocabID) return 0;
        else return vocabCount.get(vocabID);
    }
    
    int getSumCount() {
        return sumCount;
    }
    
    void incrementVocabCount(int vocabID) {
        vocabCount.increment(vocabID);
        ++sumCount;
    }
    
    void decrementVocabCount(int vocabID) {
        vocabCount.decrement(vocabID);
        --sumCount;
    }
}
