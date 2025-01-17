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
package com.tngtech.archunit.lang;

import java.util.Collection;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.properties.HasSourceCodeLocation;
import com.tngtech.archunit.lang.conditions.ArchConditions;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;
import static com.tngtech.archunit.lang.ConditionEvent.createMessage;

@PublicAPI(usage = INHERITANCE)
public abstract class ArchCondition<T> {
    private final String description;

    public ArchCondition(String description, Object... args) {
        this.description = String.format(description, args);
    }

    /**
     * Can be used/overridden to prepare this condition with respect to the collection of all objects the condition
     * will be tested against.<br>
     * ArchUnit will call this method once in the beginning, before starting to check single items.
     *
     * @param allObjectsToTest All objects that {@link #check(Object, ConditionEvents)} will be called against
     */
    public void init(Collection<T> allObjectsToTest) {
    }

    public abstract void check(T item, ConditionEvents events);

    /**
     * Can be used/overridden to finish the evaluation of this condition.<br>
     * ArchUnit will call this method once after every single item was checked (by {@link #check(Object, ConditionEvents)}).<br>
     * This method can be used, if violations are dependent on multiple/all {@link #check(Object, ConditionEvents)} calls,
     * on the contrary to the default case, where each single {@link #check(Object, ConditionEvents)} stands for itself.
     */
    public void finish(ConditionEvents events) {
    }

    public ArchCondition<T> and(ArchCondition<? super T> condition) {
        return ArchConditions.and(this, condition.forSubtype());
    }

    public ArchCondition<T> or(ArchCondition<? super T> condition) {
        return ArchConditions.or(this, condition.forSubtype());
    }

    public String getDescription() {
        return description;
    }

    /**
     * Overwrites the description of this {@link ArchCondition}. E.g.
     *
     * <pre><code>
     * classes().should(condition.as("some customized description with '%s'", "parameter"))
     * </code></pre>
     *
     * would then yield {@code classes should some customized description with 'parameter'}.
     *
     * @param description The new description of this {@link ArchCondition}
     * @param args Optional arguments to fill into the description via {@link String#format(String, Object...)}
     * @return An {@link ArchCondition} with adjusted {@link #getDescription() description}.
     */
    public ArchCondition<T> as(String description, Object... args) {
        return new ArchCondition<T>(description, args) {
            @Override
            public void init(Collection<T> allObjectsToTest) {
                ArchCondition.this.init(allObjectsToTest);
            }

            @Override
            public void check(T item, ConditionEvents events) {
                ArchCondition.this.check(item, events);
            }

            @Override
            public void finish(ConditionEvents events) {
                ArchCondition.this.finish(events);
            }
        };
    }

    @Override
    public String toString() {
        return getDescription();
    }

    /**
     * Convenience method to downcast the condition. {@link ArchCondition ArchConditions} are contravariant by nature,
     * i.e. an {@code ArchCondition<T>} is an instance of {@code ArchCondition<V>}, if and only if {@code V} is an instance of {@code T}.
     * <br>
     * Take for example {@code Object > String}. Obviously an {@code ArchCondition<Object>} is also an {@code ArchCondition<String>}.
     * <br>
     * Unfortunately, the Java type system does not allow us to express this property of the type parameter of {@code ArchCondition}.
     * So to avoid forcing users to cast everywhere it is possible to use this method which also documents the intention and reasoning.
     *
     * @return An {@link ArchCondition} accepting a subtype of the condition's actual type parameter {@code T}
     * @param <U> A subtype of the {@link ArchCondition ArchCondition's} type parameter {@code T}
     */
    @SuppressWarnings("unchecked") // Cast is safe since input parameter is contravariant
    public <U extends T> ArchCondition<U> forSubtype() {
        return (ArchCondition<U>) this;
    }

    /**
     * Creates an {@link ArchCondition} from a {@link DescribedPredicate}.
     * For more information see {@link ConditionByPredicate ConditionByPredicate}.
     * For more convenient versions of this method compare {@link ArchConditions#have(DescribedPredicate)} and {@link ArchConditions#be(DescribedPredicate)}.
     *
     * @param predicate Specifies which objects satisfy the condition.
     * @return A {@link ConditionByPredicate ConditionByPredicate} derived from the supplied {@link DescribedPredicate predicate}
     * @param <T> The type of object the {@link ArchCondition condition} will check
     *
     * @see ArchConditions#have(DescribedPredicate)
     * @see ArchConditions#be(DescribedPredicate)
     */
    @PublicAPI(usage = ACCESS)
    public static <T extends HasDescription & HasSourceCodeLocation> ConditionByPredicate<T> from(DescribedPredicate<? super T> predicate) {
        return new ConditionByPredicate<>(predicate);
    }

    /**
     * An {@link ArchCondition} that derives which objects satisfy/violate the condition from a {@link DescribedPredicate}.
     * The description is taken from the defining {@link DescribedPredicate predicate} but can be overridden via {@link #as(String, Object...)}.
     * How the message of each single {@link ConditionEvent event} is derived can be customized by {@link #describeEventsBy(EventDescriber)}.
     *
     * @param <T> The type of object the condition will test
     */
    @PublicAPI(usage = ACCESS)
    public static final class ConditionByPredicate<T extends HasDescription & HasSourceCodeLocation> extends ArchCondition<T> {
        private final DescribedPredicate<T> predicate;
        private final EventDescriber eventDescriber;

        private ConditionByPredicate(DescribedPredicate<? super T> predicate) {
            this(predicate, predicate.getDescription(), ((predicateDescription, satisfied) -> (satisfied ? "satisfies " : "does not satisfy ") + predicateDescription));
        }

        private ConditionByPredicate(
                DescribedPredicate<? super T> predicate,
                String description,
                EventDescriber eventDescriber
        ) {
            super(description);
            this.predicate = predicate.forSubtype();
            this.eventDescriber = eventDescriber;
        }

        /**
         * Adjusts how this {@link ConditionByPredicate condition} will create the description of the {@link ConditionEvent events}.
         * E.g. assume the {@link DescribedPredicate predicate} of this condition is {@link JavaClass.Predicates#simpleName(String) simpleName(name)},
         * then this method could be used to adjust the event description as
         *
         * <pre><code>
         * condition.describeEventsBy((predicateDescription, satisfied) ->
         *     (satisfied ? "has " : "does not have ") + predicateDescription
         * )</code></pre>
         *
         * @param eventDescriber Specifies how to create the description of the {@link ConditionEvent}
         *                       whenever the predicate is evaluated against an object.
         * @return A {@link ConditionByPredicate ConditionByPredicate} that describes its {@link ConditionEvent events} with the given {@link EventDescriber EventDescriber}
         */
        @PublicAPI(usage = ACCESS)
        public ConditionByPredicate<T> describeEventsBy(EventDescriber eventDescriber) {
            return new ConditionByPredicate<>(
                    predicate,
                    getDescription(),
                    eventDescriber
            );
        }

        @Override
        public ConditionByPredicate<T> as(String description, Object... args) {
            return new ConditionByPredicate<>(predicate, String.format(description, args), eventDescriber);
        }

        @Override
        @SuppressWarnings("unchecked") // Cast is safe since input parameter is contravariant
        public <U extends T> ConditionByPredicate<U> forSubtype() {
            return (ConditionByPredicate<U>) this;
        }

        @Override
        public void check(T object, ConditionEvents events) {
            boolean satisfied = predicate.test(object);
            String message = createMessage(object, eventDescriber.describe(predicate.getDescription(), satisfied));
            events.add(new SimpleConditionEvent(object, satisfied, message));
        }

        /**
         * Defines how to describe a single {@link ConditionEvent}. E.g. how to describe the concrete violation of some class
         * {@code com.Example} that violates the {@link ConditionByPredicate}.
         */
        @FunctionalInterface
        @PublicAPI(usage = INHERITANCE)
        public interface EventDescriber {
            /**
             * Describes a {@link ConditionEvent} created by {@link ConditionByPredicate ConditionByPredicate},
             * given the description of the defining predicate and whether the predicate was satisfied.<br>
             * For example, if the defining {@link DescribedPredicate} would be {@link JavaClass.Predicates#simpleName(String)}, then
             * the created description could be {@code (satisfied ? "has " : "does not have ") + predicateDescription}.
             *
             * @param predicateDescription The description of the {@link DescribedPredicate} defining the {@link ConditionByPredicate ConditionByPredicate}
             * @param satisfied Whether the object tested by the {@link ConditionByPredicate ConditionByPredicate} satisfied the condition
             * @return The description of the {@link ConditionEvent} to be created
             */
            String describe(String predicateDescription, boolean satisfied);
        }
    }
}
