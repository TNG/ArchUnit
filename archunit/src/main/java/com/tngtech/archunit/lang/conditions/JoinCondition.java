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

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

abstract class JoinCondition<T> extends ArchCondition<T> {
    private final Collection<ArchCondition<T>> conditions;

    JoinCondition(String infix, Collection<ArchCondition<T>> conditions) {
        super(joinDescriptionsOf(infix, conditions));
        this.conditions = conditions;
    }

    private static <T> String joinDescriptionsOf(String infix, Collection<ArchCondition<T>> conditions) {
        return conditions.stream().map(ArchCondition::getDescription).collect(joining(" " + infix + " "));
    }

    @Override
    public void init(Collection<T> allObjectsToTest) {
        for (ArchCondition<T> condition : conditions) {
            condition.init(allObjectsToTest);
        }
    }

    @Override
    public void finish(ConditionEvents events) {
        for (ArchCondition<T> condition : conditions) {
            condition.finish(events);
        }
    }

    List<ConditionWithEvents<T>> evaluateConditions(T item) {
        return conditions.stream().map(condition -> new ConditionWithEvents<>(condition, item)).collect(toList());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + conditions + "}";
    }

    static class ConditionWithEvents<T> {
        private final ArchCondition<T> condition;
        private final ViolatedAndSatisfiedConditionEvents events;

        ConditionWithEvents(ArchCondition<T> condition, T item) {
            this(condition, check(condition, item));
        }

        ConditionWithEvents(ArchCondition<T> condition, ViolatedAndSatisfiedConditionEvents events) {
            this.condition = condition;
            this.events = events;
        }

        public ConditionEvents getEvents() {
            return events;
        }

        private static <T> ViolatedAndSatisfiedConditionEvents check(ArchCondition<T> condition, T item) {
            ViolatedAndSatisfiedConditionEvents events = new ViolatedAndSatisfiedConditionEvents();
            condition.check(item, events);
            return events;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("condition", condition)
                    .add("events", events)
                    .toString();
        }
    }

    abstract static class JoinConditionEvent<T> implements ConditionEvent {
        final T correspondingObject;
        final List<ConditionWithEvents<T>> evaluatedConditions;

        JoinConditionEvent(T correspondingObject, List<ConditionWithEvents<T>> evaluatedConditions) {
            this.correspondingObject = correspondingObject;
            this.evaluatedConditions = evaluatedConditions;
        }

        List<String> getUniqueLinesOfViolations() {
            Set<String> result = new TreeSet<>();
            for (ConditionWithEvents<T> evaluation : evaluatedConditions) {
                for (ConditionEvent event : evaluation.events.getViolating()) {
                    result.addAll(event.getDescriptionLines());
                }
            }
            return ImmutableList.copyOf(result);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("evaluatedConditions", evaluatedConditions)
                    .toString();
        }

        List<ConditionWithEvents<T>> invert(List<ConditionWithEvents<T>> evaluatedConditions) {
            return evaluatedConditions.stream().map(this::invert).collect(toList());
        }

        private ConditionWithEvents<T> invert(ConditionWithEvents<T> evaluation) {
            ViolatedAndSatisfiedConditionEvents invertedEvents = new ViolatedAndSatisfiedConditionEvents();
            Stream.concat(
                    evaluation.events.getAllowed().stream(),
                    evaluation.events.getViolating().stream()
            ).forEach(event -> invertedEvents.add(event.invert()));
            return new ConditionWithEvents<>(evaluation.condition, invertedEvents);
        }
    }
}
