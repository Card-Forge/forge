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
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

class TopicAssignment {
    private List<Integer> topicAssignment;
    private boolean ready;
    
    TopicAssignment() {
        topicAssignment = new ArrayList<>();
        ready = false;
    }

    void set(int wordID, int topicID) {
        if (!ready) throw new IllegalStateException();
        if (wordID < 0 || topicAssignment.size() <= wordID || topicID < 0) {
            throw new IllegalArgumentException();
        }
        topicAssignment.set(wordID, topicID);
    }
    
    int get(int wordID) {
        if (!ready) throw new IllegalStateException();
        if (wordID < 0 || topicAssignment.size() <= wordID) {
            throw new IllegalArgumentException();
        }
        return topicAssignment.get(wordID);
    }
    
    void initialize(int docLength, int numTopics, long seed) {
        if (docLength <= 0 || numTopics <= 0) {
            throw new IllegalArgumentException();
        }
        
        Random random = new Random(seed);
        topicAssignment = random.ints(docLength, 0, numTopics)
                                .boxed()
                                .collect(Collectors.toList());
        ready = true;
    }
}
