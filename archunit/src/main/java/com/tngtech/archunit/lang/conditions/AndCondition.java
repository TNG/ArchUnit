/*
 * Copyright 2014-2025 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.lang.conditions;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.conditions.OrCondition.OrConditionEvent;

class AndCondition<T> extends JoinCondition<T> {
    AndCondition(ArchCondition<T> first, ArchCondition<T> second) {
        super("and", ImmutableList.of(first, second));
    }

    @Override
    public void check(T item, ConditionEvents events) {
        events.add(new AndConditionEvent<>(item, evaluateConditions(item)));
    }

    static class AndConditionEvent<T> extends JoinConditionEvent<T> {
        AndConditionEvent(T item, List<ConditionWithEvents<T>> evaluatedConditions) {
            super(item, evaluatedConditions);
        }

        @Override
        public boolean isViolation() {
            return evaluatedConditions.stream().anyMatch(evaluation -> evaluation.getEvents().containViolation());
        }

        @Override
        public ConditionEvent invert() {
            return new OrConditionEvent<>(correspondingObject, invert(evaluatedConditions));
        }

        @Override
        public List<String> getDescriptionLines() {
            return getUniqueLinesOfViolations();
        }

        @Override
        public void handleWith(ConditionEvent.Handler handler) {
            for (ConditionWithEvents<T> condition : evaluatedConditions) {
                condition.getEvents().getViolating().forEach(event -> event.handleWith(handler));
            }
        }
    }
}
