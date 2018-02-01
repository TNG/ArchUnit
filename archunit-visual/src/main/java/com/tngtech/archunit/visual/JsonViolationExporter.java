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

import java.io.Writer;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.ViolationHandler;

class JsonViolationExporter {

    private final Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    void export(EvaluationResult result, Writer writer) {
        final List<JsonViolation> violations = Lists.newArrayList();
        extractFieldAccesses(result, violations);
        extractJavaCalls(result, violations);
        writeToWriter(violations, writer);
    }

    private void extractJavaCalls(EvaluationResult result, final List<JsonViolation> violations) {
        result.handleViolations(new ViolationHandler<JavaCall<?>>() {
            @Override
            public void handle(Collection<JavaCall<?>> violatingObjects, String message) {
                for (JavaCall<?> violatingObject : violatingObjects) {
                    violations.add(JsonViolation.from(violatingObject));
                }
            }
        });
    }

    private void extractFieldAccesses(EvaluationResult result, final List<JsonViolation> violations) {
        result.handleViolations(new ViolationHandler<JavaFieldAccess>() {
            @Override
            public void handle(Collection<JavaFieldAccess> violatingObjects, String message) {
                for (JavaFieldAccess violatingObject : violatingObjects) {
                    violations.add(JsonViolation.from(violatingObject));
                }
            }
        });
    }

    private void writeToWriter(List<JsonViolation> violations, Writer writer) {
        gson.toJson(violations, writer);
    }
}
