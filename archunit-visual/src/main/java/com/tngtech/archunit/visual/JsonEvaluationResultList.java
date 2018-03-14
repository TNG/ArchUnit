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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class JsonEvaluationResultList {
    List<JsonEvaluationResult> jsonEvaluationResultList;

    JsonEvaluationResultList(List<JsonEvaluationResult> jsonEvaluationResultList) {
        this.jsonEvaluationResultList = jsonEvaluationResultList;
    }

    void insertEvaluationResult(JsonEvaluationResult newEvaluationResult) {
        for (JsonEvaluationResult evaluationResult : jsonEvaluationResultList) {
            if (evaluationResult.getRule().equals(newEvaluationResult.getRule())) {
                Map<String, JsonViolation> jsonViolationMap= new HashMap<>();
                for (JsonViolation jsonViolation : evaluationResult.getViolations()) {
                    jsonViolationMap.put(jsonViolation.getIdentifier(), jsonViolation);
                }
                for (JsonViolation jsonViolation: newEvaluationResult.getViolations()) {
                    jsonViolationMap.put(jsonViolation.getIdentifier(), jsonViolation);
                }
                evaluationResult.setViolations(new ArrayList<>(jsonViolationMap.values()));
                return;
            }
        }
        jsonEvaluationResultList.add(newEvaluationResult);
    }

    List<JsonEvaluationResult> getJsonEvaluationResultList() {
        return jsonEvaluationResultList;
    }
}
