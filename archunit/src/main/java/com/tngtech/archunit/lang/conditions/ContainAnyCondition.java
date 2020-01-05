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
package com.tngtech.archunit.lang.conditions;

import java.util.Collection;
import java.util.List;

import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.CollectsLines;
import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.conditions.ContainsOnlyCondition.OnlyConditionEvent;

import static java.util.Collections.singletonList;

class ContainAnyCondition<T> extends ArchCondition<Collection<? extends T>> {
    private final ArchCondition<T> condition;

    ContainAnyCondition(ArchCondition<T> condition) {
        super("contain any element that " + condition.getDescription());
        this.condition = condition;
    }

    @Override
    public void check(Collection<? extends T> collection, ConditionEvents events) {
        ConditionEvents subEvents = new ConditionEvents();
        for (T element : collection) {
            condition.check(element, subEvents);
        }
        if (!subEvents.isEmpty()) {
            events.add(new AnyConditionEvent(collection, subEvents));
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{condition=" + condition + "}";
    }

    static class AnyConditionEvent implements ConditionEvent {
        private final Collection<?> correspondingObjects;
        private final Collection<ConditionEvent> allowed;
        private final Collection<ConditionEvent> violating;

        private AnyConditionEvent(Collection<?> correspondingObjects, ConditionEvents events) {
            this(correspondingObjects, events.getAllowed(), events.getViolating());
        }

        AnyConditionEvent(Collection<?> correspondingObjects,
                Collection<ConditionEvent> allowed,
                Collection<ConditionEvent> violating) {
            this.correspondingObjects = correspondingObjects;
            this.allowed = allowed;
            this.violating = violating;
        }

        @Override
        public boolean isViolation() {
            return allowed.isEmpty();
        }

        @Override
        public void addInvertedTo(ConditionEvents events) {
            events.add(new OnlyConditionEvent(correspondingObjects, violating, allowed));
        }

        /**
         * @deprecated See {@link ConditionEvent#describeTo(CollectsLines)}
         */
        @Deprecated
        @Override
        public void describeTo(CollectsLines messages) {
            for (String line : getDescriptionLines()) {
                messages.add(line);
            }
        }

        @Override
        public List<String> getDescriptionLines() {
            return singletonList(EventsDescription.describe(violating));
        }

        @Override
        public void handleWith(Handler handler) {
            handler.handle(correspondingObjects, EventsDescription.describe(violating));
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" +
                    "correspondingObjects=" + correspondingObjects +
                    ", allowed=" + allowed +
                    ", violating=" + violating +
                    '}';
        }
    }
}
