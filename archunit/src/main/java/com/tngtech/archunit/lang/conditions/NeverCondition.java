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

import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;

class NeverCondition<T> extends ArchCondition<T> {
    private final ArchCondition<T> condition;

    NeverCondition(ArchCondition<T> condition) {
        super("never " + condition.getDescription());
        this.condition = condition;
    }

    @Override
    public void init(Iterable<T> allObjectsToTest) {
        condition.init(allObjectsToTest);
    }

    @Override
    public void finish(ConditionEvents events) {
        ConditionEvents subEvents = new ConditionEvents();
        condition.finish(subEvents);
        for (ConditionEvent event : subEvents) {
            event.addInvertedTo(events);
        }
    }

    @Override
    public void check(T item, ConditionEvents events) {
        ConditionEvents subEvents = new ConditionEvents();
        condition.check(item, subEvents);
        for (ConditionEvent event : subEvents) {
            event.addInvertedTo(events);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{condition=" + condition + "}";
    }

}
