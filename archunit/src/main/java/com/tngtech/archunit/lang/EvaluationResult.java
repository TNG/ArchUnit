/*
 * Copyright 2017 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.lang;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.core.domain.JavaClasses;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

/**
 * Represents the result of evaluating an {@link ArchRule} against some {@link JavaClasses}.
 * To react to failures during evaluation of the rule, one can use {@link #handleViolations(ViolationHandler)}:
 * <br><br>
 * <pre><code>
 * result.handleViolations(new ViolationHandler&lt;JavaAccess&lt;?&gt;&gt;() {
 *     {@literal @}Override
 *     public void handle(Collection&lt;JavaAccess&lt;?&gt;&gt; violatingObjects, String message) {
 *         // do some reporting or react in any way to violation
 *     }
 * });
 * </code></pre>
 */
public final class EvaluationResult {
    private final HasDescription rule;
    private final ConditionEvents events;
    private final Priority priority;

    @PublicAPI(usage = ACCESS)
    public EvaluationResult(HasDescription rule, Priority priority) {
        this(rule, new ConditionEvents(), priority);
    }

    @PublicAPI(usage = ACCESS)
    public EvaluationResult(HasDescription rule, ConditionEvents events, Priority priority) {
        this.rule = rule;
        this.events = events;
        this.priority = priority;
    }

    @PublicAPI(usage = ACCESS)
    public FailureReport getFailureReport() {
        FailureReport failureReport = new FailureReport(rule, priority);
        events.describeFailuresTo(failureReport);
        return failureReport;
    }

    @PublicAPI(usage = ACCESS)
    public void add(EvaluationResult part) {
        for (ConditionEvent event : part.events) {
            events.add(event);
        }
    }

    /**
     * @see ConditionEvents#handleViolations(ViolationHandler)
     */
    @PublicAPI(usage = ACCESS)
    public void handleViolations(ViolationHandler<?> violationHandler) {
        events.handleViolations(violationHandler);
    }

    @PublicAPI(usage = ACCESS)
    public boolean hasViolation() {
        return events.containViolation();
    }
}
