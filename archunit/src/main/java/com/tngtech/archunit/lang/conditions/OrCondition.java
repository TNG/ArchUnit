/*
 * Copyright 2014-2026 TNG Technology Consulting GmbH
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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.conditions.AndCondition.AndConditionEvent;

import static java.util.Collections.singleton;

class OrCondition<T> extends JoinCondition<T> {
    OrCondition(ArchCondition<T> first, ArchCondition<T> second) {
        super("or", ImmutableList.of(first, second));
    }

    @Override
    public void check(T item, ConditionEvents events) {
        events.add(new OrConditionEvent<>(item, evaluateConditions(item)));
    }

    static class OrConditionEvent<T> extends JoinConditionEvent<T> {
        OrConditionEvent(T item, List<ConditionWithEvents<T>> evaluatedConditions) {
            super(item, evaluatedConditions);
        }

        @Override
        public boolean isViolation() {
            return evaluatedConditions.stream().allMatch(evaluation -> evaluation.getEvents().containViolation());
        }

        @Override
        public ConditionEvent invert() {
            return new AndConditionEvent<>(correspondingObject, invert(evaluatedConditions));
        }

        @Override
        public List<String> getDescriptionLines() {
            return ImmutableList.of(createMessage());
        }

        private String createMessage() {
            return Joiner.on(" and ").join(getUniqueLinesOfViolations());
        }

        @Override
        public void handleWith(Handler handler) {
            handler.handle(singleton(correspondingObject), createMessage());
        }
    }
}
