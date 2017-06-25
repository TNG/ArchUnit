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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Joiner;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.transform;

public class SimpleConditionEvent implements ConditionEvent {
    private final Object correspondingObject;
    private final boolean conditionSatisfied;
    private final String message;

    public SimpleConditionEvent(Object correspondingObject, boolean conditionSatisfied, String message) {
        this.correspondingObject = correspondingObject;
        this.conditionSatisfied = conditionSatisfied;
        this.message = message;
        checkArgument(conditionSatisfied || !this.message.trim().isEmpty(), "Message may not be empty for violation");
    }

    @Override
    public boolean isViolation() {
        return !conditionSatisfied;
    }

    @Override
    public void addInvertedTo(ConditionEvents events) {
        events.add(new SimpleConditionEvent(correspondingObject, !conditionSatisfied, message));
    }

    @Override
    public void describeTo(CollectsLines messages) {
        messages.add(message);
    }

    @Override
    public Object getCorrespondingObject() {
        return correspondingObject;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("correspondingObject", correspondingObject)
                .add("conditionSatisfied", conditionSatisfied)
                .add("message", message)
                .toString();
    }

    protected static String joinMessages(Collection<? extends ConditionEvent> violating) {
        Iterable<String> lines = concat(transform(violating, TO_MESSAGES));
        return Joiner.on(System.lineSeparator()).join(lines);
    }

    private static final Function<ConditionEvent, Iterable<String>> TO_MESSAGES = new Function<ConditionEvent, Iterable<String>>() {
        @Override
        public Iterable<String> apply(ConditionEvent input) {
            final List<String> result = new ArrayList<>();
            input.describeTo(new CollectsLines() {
                @Override
                public void add(String line) {
                    result.add(line);
                }
            });
            return result;
        }
    };

    public static ConditionEvent violated(Object correspondingObject, String message) {
        return new SimpleConditionEvent(correspondingObject, false, message);
    }

    public static ConditionEvent satisfied(Object correspondingObject, String message) {
        return new SimpleConditionEvent(correspondingObject, true, message);
    }
}
