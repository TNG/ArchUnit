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
package com.tngtech.archunit.lang;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import com.tngtech.archunit.PublicAPI;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static java.util.Collections.singletonList;

@PublicAPI(usage = ACCESS)
public final class SimpleConditionEvent implements ConditionEvent {
    private final Object correspondingObject;
    private final boolean conditionSatisfied;
    // the message is computed lazily, if a supplier was given. Since the supplier must be deterministic and String is immutable,
    // it's fine (and cheaper than synchronization) if concurrent threads race to compute and cache the message.
    private String message;
    private final Supplier<String> messageSupplier;

    public SimpleConditionEvent(Object correspondingObject, boolean conditionSatisfied, String message) {
        this(correspondingObject, conditionSatisfied, message, null);
        checkNonEmptyIfViolation(message);
    }

    /**
     * Like {@link #SimpleConditionEvent(Object, boolean, String)}, but the message is only computed once it is actually
     * requested (e.g. to report a violation). This avoids the cost of creating messages for events
     * that are never reported, which can be significant if a condition is evaluated against a lot of objects.
     * <br><br>
     * The supplier is called at most once per event in the absence of concurrent access (the message is cached),
     * but may be called again for events derived by {@link #invert()} and must always return the same message.
     * In contrast to the {@code String} variant, an empty message of a violation can't be rejected
     * when the event is created. So it is rejected (by an {@link IllegalArgumentException}) as soon as the message is requested.
     *
     * @param correspondingObject The object this event is about
     * @param conditionSatisfied Whether the condition is satisfied, i.e. whether the event is <b>not</b> a violation
     * @param messageSupplier Supplies the message describing this event
     */
    public SimpleConditionEvent(Object correspondingObject, boolean conditionSatisfied, Supplier<String> messageSupplier) {
        this(correspondingObject, conditionSatisfied, null, checkNotNull(messageSupplier));
    }

    private SimpleConditionEvent(Object correspondingObject, boolean conditionSatisfied, String message, Supplier<String> messageSupplier) {
        this.correspondingObject = correspondingObject;
        this.conditionSatisfied = conditionSatisfied;
        this.message = message;
        this.messageSupplier = messageSupplier;
    }

    private String message() {
        String result = message;
        if (result == null) {
            result = messageSupplier.get();
            checkNonEmptyIfViolation(result);
            message = result;
        }
        return result;
    }

    private void checkNonEmptyIfViolation(String messageToCheck) {
        checkArgument(conditionSatisfied || !messageToCheck.trim().isEmpty(), "Message may not be empty for violation");
    }

    @Override
    public boolean isViolation() {
        return !conditionSatisfied;
    }

    @Override
    public ConditionEvent invert() {
        String alreadyComputed = message;
        return alreadyComputed != null
                ? new SimpleConditionEvent(correspondingObject, !conditionSatisfied, alreadyComputed)
                : new SimpleConditionEvent(correspondingObject, !conditionSatisfied, messageSupplier);
    }

    @Override
    public List<String> getDescriptionLines() {
        return singletonList(message());
    }

    @Override
    public void handleWith(Handler handler) {
        handler.handle(Collections.singleton(correspondingObject), message());
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("correspondingObject", correspondingObject)
                .add("conditionSatisfied", conditionSatisfied)
                .add("message", message())
                .toString();
    }

    public static ConditionEvent violated(Object correspondingObject, String message) {
        return new SimpleConditionEvent(correspondingObject, false, message);
    }

    public static ConditionEvent violated(Object correspondingObject, Supplier<String> message) {
        return new SimpleConditionEvent(correspondingObject, false, message);
    }

    public static ConditionEvent satisfied(Object correspondingObject, String message) {
        return new SimpleConditionEvent(correspondingObject, true, message);
    }

    public static ConditionEvent satisfied(Object correspondingObject, Supplier<String> message) {
        return new SimpleConditionEvent(correspondingObject, true, message);
    }
}
