/*
 * Copyright 2018 TNG Technology Consulting GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tngtech.archunit.visual;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Multimap;
import com.google.gson.annotations.Expose;

import java.util.Collection;
import java.util.Map;

class JsonEvaluationResult {
    @Expose
    private String rule;
    @Expose
    private Collection<JsonViolation> violations;

    private JsonEvaluationResult(String rule, Collection<JsonViolation> violations) {
        this.rule = rule;
        this.violations = violations;
    }

    static Collection<JsonEvaluationResult> CreateFromMultiMap(Multimap<String, JsonViolation> violations) {
        return Collections2.transform(violations.asMap().entrySet(),
                new Function<Map.Entry<String, Collection<JsonViolation>>, JsonEvaluationResult>() {
                    @Override
                    public JsonEvaluationResult apply(Map.Entry<String, Collection<JsonViolation>> input) {
                        return new JsonEvaluationResult(input.getKey(), input.getValue());
                    }
                });
    }
}
