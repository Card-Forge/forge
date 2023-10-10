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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class AssignmentCounter {
    private List<Integer> counter;

    AssignmentCounter(int size) {
        if (size <= 0) throw new IllegalArgumentException();
        this.counter = IntStream.generate(() -> 0)
                                .limit(size)
                                .boxed()
                                .collect(Collectors.toList());
    }
    
    int size() {
        return counter.size();
    }
    
    int get(int id) {
        if (id < 0 || counter.size() <= id) {
            throw new IllegalArgumentException();
        }
        return counter.get(id);
    }
    
    int getSum() {
        return counter.stream().reduce(Integer::sum).get();
    }
    
    void increment(int id) {
        if (id < 0 || counter.size() <= id) {
            throw new IllegalArgumentException();
        }
        counter.set(id, counter.get(id) + 1);
    }
    
    void decrement(int id) {
        if (id < 0 || counter.size() <= id) {
            throw new IllegalArgumentException();
        }
        if (counter.get(id) == 0) {
            throw new IllegalStateException();
        }
        counter.set(id, counter.get(id) - 1);
    }
}
