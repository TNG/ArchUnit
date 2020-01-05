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

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public class ArchPredicates {
    private ArchPredicates() {
    }

    /**
     * This method is just syntactic sugar, e.g. to write aClass.that(is(special))
     *
     * @param predicate The original predicate
     * @param <T>       The type of the object to decide on
     * @return The original predicate with adjusted description
     */
    @PublicAPI(usage = ACCESS)
    public static <T> DescribedPredicate<T> is(DescribedPredicate<? super T> predicate) {
        return predicate.as("is " + predicate.getDescription()).forSubType();
    }

    /**
     * This method is just syntactic sugar, e.g. to write classes.that(are(special))
     *
     * @param predicate The original predicate
     * @param <T>       The type of the object to decide on
     * @return The original predicate with adjusted description
     */
    @PublicAPI(usage = ACCESS)
    public static <T> DescribedPredicate<T> are(DescribedPredicate<? super T> predicate) {
        return predicate.as("are " + predicate.getDescription()).forSubType();
    }

    /**
     * This method is just syntactic sugar, e.g. to write method.that(has(type(..))
     *
     * @param predicate The original predicate
     * @param <T>       The type of the object to decide on
     * @return The original predicate with adjusted description
     */
    @PublicAPI(usage = ACCESS)
    public static <T> DescribedPredicate<T> has(DescribedPredicate<? super T> predicate) {
        return predicate.as("has " + predicate.getDescription()).forSubType();
    }

    /**
     * This method is just syntactic sugar, e.g. to write classes.that(have(type(..))
     *
     * @param predicate The original predicate
     * @param <T>       The type of the object to decide on
     * @return The original predicate with adjusted description
     */
    @PublicAPI(usage = ACCESS)
    public static <T> DescribedPredicate<T> have(DescribedPredicate<? super T> predicate) {
        return predicate.as("have " + predicate.getDescription()).forSubType();
    }

    /**
     * This method is just syntactic sugar, e.g. to write classes.should(be(public()))
     *
     * @param predicate The original predicate
     * @param <T>       The type of the object to decide on
     * @return The original predicate with adjusted description
     */
    @PublicAPI(usage = ACCESS)
    public static <T> DescribedPredicate<T> be(DescribedPredicate<? super T> predicate) {
        return predicate.as("be " + predicate.getDescription()).forSubType();
    }
}
