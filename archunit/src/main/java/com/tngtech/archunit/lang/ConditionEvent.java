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

import java.util.Collection;
import java.util.List;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.JavaMethodCall;

import static com.tngtech.archunit.PublicAPI.State.EXPERIMENTAL;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;

@PublicAPI(usage = INHERITANCE)
public interface ConditionEvent {
    /**
     * @return true, IFF this event represents a violation of an evaluated rule.
     */
    boolean isViolation();

    /**
     * Adds the 'opposite' of the event. <br>
     * E.g. <i>The event is a violation, if some conditions A and B are both true?</i>
     * <br> {@literal ->} <i>The 'inverted' event is a violation if either A or B (or both) are not true</i><br>
     * In the most simple case, this is just an equivalent event evaluating {@link #isViolation()}
     * inverted.
     *
     * @param events The events to add the 'inverted self' to
     */
    void addInvertedTo(ConditionEvents events);

    /**
     * Adds a textual description of this event to the supplied {@link CollectsLines}.
     *
     * @deprecated This method will be removed in the future in favor of the simpler {@link #getDescriptionLines()}.<br>
     * {@link #describeTo(CollectsLines) describeTo(lineCollector)} has the same behavior as simply
     * adding all {@link #getDescriptionLines()} to the {@code lineCollector}.
     * @param messages The message lines to append the description to.
     */
    @Deprecated
    void describeTo(CollectsLines messages);

    /**
     * @return A textual description of this event as a list of lines
     */
    List<String> getDescriptionLines();

    /**
     * Supplies the corresponding objects and description to the supplied handler.
     * <br><br>
     * The term "corresponding objects" refers to the objects involved in the evaluation of this rule.
     * E.g. the rule checks for illegal field accesses,
     * then this object might be a single field access checked by the rule.<br>
     * May also be a collection of objects, if the evaluation of the rule depends on sets of objects.
     * E.g. the rule checks that some access to another class happened? The rule can only be violated,
     * by a whole set (all accesses from a class) of objects, but not by a single one (if there is more than one).
     *
     * @param handler The handler to supply the data of this event to.
     */
    @PublicAPI(usage = INHERITANCE, state = EXPERIMENTAL)
    void handleWith(Handler handler);

    /**
     * Handles the data of a {@link ConditionEvent} that is the corresponding objects and the description
     * (compare {@link #handleWith(Handler)}).<br>
     * As an example, this could be a single element of type {@link JavaMethodCall} together with a description, like
     * <p>
     * <i>'Method A.foo() calls method B.bar() in (A.java:123)'</i>
     * </p>
     */
    interface Handler {
        /**
         * @param correspondingObjects The objects this event describes (e.g. method calls, field accesses, ...)
         * @param message              Describes the event
         */
        @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
        void handle(Collection<?> correspondingObjects, String message);
    }
}
