/*
 * Copyright 2014-2020 TNG Technology Consulting GmbH
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

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.JavaClasses;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.lang.ArchRule.Factory.createBecauseDescription;
import static com.tngtech.archunit.lang.Priority.MEDIUM;
import static java.util.Collections.singletonList;

public final class CompositeArchRule implements ArchRule {
    private final Priority priority;
    private final List<ArchRule> rules;
    private final String description;

    private CompositeArchRule(Priority priority, List<ArchRule> rules, String description) {
        this.priority = priority;
        this.rules = checkNotNull(rules);
        this.description = checkNotNull(description);
    }

    @PublicAPI(usage = ACCESS)
    public static CompositeArchRule of(ArchRule rule) {
        return priority(MEDIUM).of(rule);
    }

    @PublicAPI(usage = ACCESS)
    public static Creator priority(Priority priority) {
        return new Creator(priority);
    }

    @PublicAPI(usage = ACCESS)
    public CompositeArchRule and(ArchRule rule) {
        List<ArchRule> newRules = ImmutableList.<ArchRule>builder().addAll(rules).add(rule).build();
        String newDescription = description + " and " + rule.getDescription();
        return new CompositeArchRule(priority, newRules, newDescription);
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public void check(JavaClasses classes) {
        Assertions.check(this, classes);
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public CompositeArchRule because(String reason) {
        return new CompositeArchRule(priority, rules, createBecauseDescription(this, reason));
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public EvaluationResult evaluate(JavaClasses classes) {
        EvaluationResult result = new EvaluationResult(this, priority);
        for (ArchRule rule : rules) {
            result.add(rule.evaluate(classes));
        }
        return result;
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public CompositeArchRule as(String newDescription) {
        return new CompositeArchRule(priority, rules, newDescription);
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public String getDescription() {
        return description;
    }

    @PublicAPI(usage = ACCESS)
    public static final class Creator {
        private final Priority priority;

        private Creator(Priority priority) {
            this.priority = priority;
        }

        @PublicAPI(usage = ACCESS)
        public final CompositeArchRule of(ArchRule rule) {
            return new CompositeArchRule(priority, singletonList(rule), rule.getDescription());
        }
    }
}
