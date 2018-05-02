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

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.ViolationHandler;

import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.List;

class JsonViolationExporter {

    private final Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    void export(Iterable<EvaluationResult> results, Reader jsonViolationReader, Supplier<Writer> getJsonViolationWriter) {
        List<JsonEvaluationResult> existingViolationsList =
                gson.fromJson(jsonViolationReader, new TypeToken<List<JsonEvaluationResult>>() {
                }.getType());
        if (existingViolationsList == null) {
            existingViolationsList = Lists.newArrayList();
        }
        try (Writer writer = getJsonViolationWriter.get()) {
            export(results, existingViolationsList, writer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void export(Iterable<EvaluationResult> results, Supplier<Writer> getJsonViolationWriter) {
        try (Writer writer = getJsonViolationWriter.get()) {
            export(results, Lists.<JsonEvaluationResult>newArrayList(), writer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void export(Iterable<EvaluationResult> results, List<JsonEvaluationResult> existingViolationsList, Writer jsonViolationWriter) {
        JsonEvaluationResultList existingViolations = new JsonEvaluationResultList(existingViolationsList);
        for (EvaluationResult result : results) {
            final JsonEvaluationResult evaluationResult = new JsonEvaluationResult(result.getRuleText());
            extractFieldAccesses(result, evaluationResult);
            extractJavaCalls(result, evaluationResult);
            existingViolations.insertEvaluationResult(evaluationResult);
        }
        writeToWriter(existingViolations.getJsonEvaluationResultList(), jsonViolationWriter);
    }

    private void extractJavaCalls(EvaluationResult result, final JsonEvaluationResult evaluationResult) {
        result.handleViolations(new ViolationHandler<JavaCall<?>>() {
            @Override
            public void handle(Collection<JavaCall<?>> violatingObjects, String message) {
                for (JavaCall<?> violatingObject : violatingObjects) {
                    evaluationResult.addViolation(JsonViolation.from(violatingObject));
                }
            }
        });
    }

    private void extractFieldAccesses(EvaluationResult result, final JsonEvaluationResult evaluationResult) {
        result.handleViolations(new ViolationHandler<JavaFieldAccess>() {
            @Override
            public void handle(Collection<JavaFieldAccess> violatingObjects, String message) {
                for (JavaFieldAccess violatingObject : violatingObjects) {
                    evaluationResult.addViolation(JsonViolation.from(violatingObject));
                }
            }
        });
    }

    private void writeToWriter(final List<JsonEvaluationResult> evaluationResults, Writer jsonViolationWriter) {
        gson.toJson(evaluationResults, jsonViolationWriter);
    }
}
