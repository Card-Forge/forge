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

class Hyperparameters {
    private Alpha alpha;
    private Beta beta;
    
    Hyperparameters(double alpha, double beta, int numTopics, int numVocabs) {
        this.alpha = new Alpha(alpha, numTopics);
        this.beta  = new Beta(beta, numVocabs);
    }
    
    Hyperparameters(double alpha, double beta, int numTopics) {
        this.alpha = new Alpha(alpha, numTopics);
        this.beta  = new Beta(beta);
    }

    double alpha(int i) {
        return alpha.get(i);
    }
    
    double sumAlpha() {
        return alpha.sum();
    }
    
    double beta() {
        return beta.get();
    }
    
    double beta(int i) {
        return beta.get(i);
    }
    
    void setAlpha(int i, double value) {
        alpha.set(i, value);
    }
    
    void setBeta(int i, double value) {
        beta.set(i, value);
    }
    
    void setBeta(double value) {
        beta.set(0, value);
    }
}
