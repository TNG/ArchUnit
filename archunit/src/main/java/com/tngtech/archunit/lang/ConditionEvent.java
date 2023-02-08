/*
 * Copyright 2014-2023 TNG Technology Consulting GmbH
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
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.properties.HasSourceCodeLocation;

import static com.tngtech.archunit.PublicAPI.State.EXPERIMENTAL;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;

/**
 * An event that occurred while checking an {@link ArchCondition}. This can either be a {@link #isViolation() violation}
 * or be allowed. An event that is allowed will turn into a violation if it is {@link #invert() inverted}
 * (e.g. for negation of the rule).
 */
@PublicAPI(usage = INHERITANCE)
public interface ConditionEvent {
    /**
     * @return {@code true} if this event represents a violation of an evaluated rule, {@code false} otherwise
     */
    boolean isViolation();

    /**
     * @return the 'opposite' of the event. <br>
     *         Assume e.g. <i>The event is a violation, if some conditions A and B are both true</i>
     *         <br> {@literal =>} <i>The 'inverted' event is a violation if either A or B (or both) are not true</i><br>
     *         In the most simple case, this is just an equivalent event evaluating {@link #isViolation()}
     *         inverted.
     */
    ConditionEvent invert();

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
     * Convenience method to create a standard ArchUnit {@link ConditionEvent} message. It will prepend the
     * description of the object that caused the event (e.g. a {@link JavaClass}) and append the source code
     * location of the respective object.
     *
     * @param object The object to describe, e.g. the {@link JavaClass} {@code com.example.SomeClass}
     * @param message The message that should be filled into the template, e.g. "does not have simple name 'Correct'"
     * @return The formatted message, e.g. {@code Class <com.example.SomeClass> does not have simple name 'Correct' in (SomeClass.java:0)}
     * @param <T> The object described by the event.
     */
    @PublicAPI(usage = ACCESS)
    static <T extends HasDescription & HasSourceCodeLocation> String createMessage(T object, String message) {
        return object.getDescription() + " " + message + " in " + object.getSourceCodeLocation();
    }

    /**
     * Handles the data of a {@link ConditionEvent} that is the corresponding objects and the description
     * (compare {@link #handleWith(Handler)}).<br>
     * As an example, this could be a single element of type {@link JavaMethodCall} together with a description, like
     * <p>
     * <i>'Method A.foo() calls method B.bar() in (A.java:123)'</i>
     * </p>
     */
    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    interface Handler {
        /**
         * @param correspondingObjects The objects this event describes (e.g. method calls, field accesses, ...)
         * @param message              Describes the event
         */
        @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
        void handle(Collection<?> correspondingObjects, String message);
    }
}
