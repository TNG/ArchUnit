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

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import com.tngtech.archunit.lang.ConditionEvent;

import static com.google.common.base.MoreObjects.toStringHelper;
import static java.util.Collections.singletonList;

/**
 * Like {@link com.tngtech.archunit.lang.SimpleConditionEvent}, but the message is only rendered once it is actually requested.
 * Rendering messages (e.g. the description of a {@link com.tngtech.archunit.core.domain.JavaAccess}) is expensive,
 * and many events are discarded without ever being reported (e.g. non-matching events of a negated rule).
 */
class LazyMessageConditionEvent implements ConditionEvent {
    private final Object correspondingObject;
    private final boolean conditionSatisfied;
    private final Supplier<String> message;

    LazyMessageConditionEvent(Object correspondingObject, boolean conditionSatisfied, Supplier<String> message) {
        this.correspondingObject = correspondingObject;
        this.conditionSatisfied = conditionSatisfied;
        this.message = message;
    }

    @Override
    public boolean isViolation() {
        return !conditionSatisfied;
    }

    @Override
    public ConditionEvent invert() {
        return new LazyMessageConditionEvent(correspondingObject, !conditionSatisfied, message);
    }

    @Override
    public List<String> getDescriptionLines() {
        return singletonList(message.get());
    }

    @Override
    public void handleWith(Handler handler) {
        handler.handle(Collections.singleton(correspondingObject), message.get());
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("correspondingObject", correspondingObject)
                .add("conditionSatisfied", conditionSatisfied)
                .add("message", message.get())
                .toString();
    }
}
