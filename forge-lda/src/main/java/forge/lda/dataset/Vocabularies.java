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

package forge.lda.dataset;

import java.util.Collections;
import java.util.List;

public class Vocabularies {
    private List<Vocabulary> vocabs;
    
    public Vocabularies(List<Vocabulary> vocabs) {
        this.vocabs = vocabs;
    }
    
    public Vocabulary get(int id) {
        return vocabs.get(id);
    }
    
    public int size() {
        return vocabs.size();
    }
    
    public List<Vocabulary> getVocabularyList() {
        return Collections.unmodifiableList(vocabs);
    }
}
