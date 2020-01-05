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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.tngtech.archunit.lang.ConditionEvent;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.transform;

class EventsDescription {
    static String describe(Collection<? extends ConditionEvent> violating) {
        Iterable<String> lines = concat(transform(violating, TO_MESSAGES));
        return Joiner.on(System.lineSeparator()).join(lines);
    }

    private static final Function<ConditionEvent, Iterable<String>> TO_MESSAGES = new Function<ConditionEvent, Iterable<String>>() {
        @Override
        public Iterable<String> apply(ConditionEvent input) {
            return input.getDescriptionLines();
        }
    };
}
