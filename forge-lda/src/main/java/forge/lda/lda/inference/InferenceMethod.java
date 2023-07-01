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

import forge.lda.lda.inference.internal.CollapsedGibbsSampler;

public enum InferenceMethod {
    CGS(CollapsedGibbsSampler.class.getName()),
    // more
    ;

    private String className;
    
    private InferenceMethod(final String className) {
        this.className = className;
    }
    
    @Override
    public String toString() {
        return className;
    }
}
