/*
 * Copyright 2014-2022 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.junit;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.JUnitException;

class ConditionEvaluator {

    ConditionEvaluationResult evaluate(ArchUnitEngineExecutionContext context, ExtensionContext extensionContext) {
        return context.getExtensions(ExecutionCondition.class).stream()
                .map(condition -> evaluate(condition, extensionContext))
                .filter(ConditionEvaluationResult::isDisabled)
                .findFirst()
                .orElse(ConditionEvaluationResult.enabled("No 'disabled' conditions encountered"));
    }

    private ConditionEvaluationResult evaluate(ExecutionCondition condition, ExtensionContext extensionContext) {
        try {
            return condition.evaluateExecutionCondition(extensionContext);
        } catch (Exception e) {
            throw new JUnitException("Error evaluating condition " + condition.toString() + " on " + extensionContext.getDisplayName(), e);
        }
    }
}
