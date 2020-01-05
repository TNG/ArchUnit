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

import java.util.Collections;
import java.util.List;

import com.tngtech.archunit.PublicAPI;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static java.util.Collections.singletonList;

@PublicAPI(usage = ACCESS)
public final class SimpleConditionEvent implements ConditionEvent {
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
        return singletonList(message);
    }

    @Override
    public void handleWith(Handler handler) {
        handler.handle(Collections.singleton(correspondingObject), message);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("correspondingObject", correspondingObject)
                .add("conditionSatisfied", conditionSatisfied)
                .add("message", message)
                .toString();
    }

    public static ConditionEvent violated(Object correspondingObject, String message) {
        return new SimpleConditionEvent(correspondingObject, false, message);
    }

    public static ConditionEvent satisfied(Object correspondingObject, String message) {
        return new SimpleConditionEvent(correspondingObject, true, message);
    }
}
