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

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Collections2;
import com.google.common.collect.Multimap;
import com.google.gson.annotations.Expose;

class JsonEvaluationResult {
    @Expose
    private String rule;
    @Expose
    private Collection<String> violations;

    private JsonEvaluationResult(String rule, Collection<String> violations) {
        this.rule = rule;
        this.violations = violations;
    }

    static Collection<JsonEvaluationResult> CreateFromMultiMap(Multimap<String, String> violations) {
        return Collections2.transform(violations.asMap().entrySet(),
                new Function<Map.Entry<String, Collection<String>>, JsonEvaluationResult>() {
                    @Override
                    public JsonEvaluationResult apply(Map.Entry<String, Collection<String>> input) {
                        return new JsonEvaluationResult(input.getKey(), input.getValue());
                    }
                });
    }

    @Override
    public int hashCode() {
        return Objects.hash(rule, violations);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final JsonEvaluationResult other = (JsonEvaluationResult) obj;
        return Objects.equals(this.rule, other.rule)
                && Objects.equals(this.violations, other.violations);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("rule", rule)
                .add("violations", violations)
                .toString();
    }
}
