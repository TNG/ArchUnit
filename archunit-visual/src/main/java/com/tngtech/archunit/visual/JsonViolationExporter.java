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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.ViolationHandler;

import java.util.Collection;

class JsonViolationExporter {

    private final Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    String exportToJson(Iterable<EvaluationResult> results) {
        Multimap<String, JsonViolation> violations = HashMultimap.create();
        for (EvaluationResult result : results) {
            extractFieldAccesses(result, violations);
            extractJavaCalls(result, violations);
            //extractDependencies(result, violations);
        }
        return gson.toJson(JsonEvaluationResult.CreateFromMultiMap(violations));
    }

    private void extractJavaCalls(final EvaluationResult result, final Multimap<String, JsonViolation> violations) {
        result.handleViolations(new ViolationHandler<JavaCall<?>>() {
            @Override
            public void handle(Collection<JavaCall<?>> violatingObjects, String message) {
                for (JavaCall<?> violatingObject : violatingObjects) {
                    violations.put(result.getRuleText(), JsonViolation.from(violatingObject));
                }
            }
        });
    }

    /*private void extractDependencies(EvaluationResult result, final Multimap<String, JsonViolation> violations) {
        result.handleViolations(new ViolationHandler<Cycle>() {
            @Override
            public void handle(Collection<Cycle> violatingObjects, String message) {
                for (Cycle violatingObject : violatingObjects) {
                    //FIXME:
                    //evaluationResult.addViolation(JsonViolation.from(violatingObject));
                }
            }
        });
    }*/

    private void extractFieldAccesses(final EvaluationResult result, final Multimap<String, JsonViolation> violations) {
        result.handleViolations(new ViolationHandler<JavaFieldAccess>() {
            @Override
            public void handle(Collection<JavaFieldAccess> violatingObjects, String message) {
                for (JavaFieldAccess violatingObject : violatingObjects) {
                    violations.put(result.getRuleText(), JsonViolation.from(violatingObject));
                }
            }
        });
    }
}
