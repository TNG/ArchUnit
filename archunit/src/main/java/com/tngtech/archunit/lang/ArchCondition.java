/*
 * Copyright 2014-2022 TNG Technology Consulting GmbH
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
import com.tngtech.archunit.lang.conditions.ArchConditions;

import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;

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
}
