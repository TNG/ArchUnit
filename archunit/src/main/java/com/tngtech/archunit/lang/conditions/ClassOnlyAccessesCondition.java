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

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaClass;

class ClassOnlyAccessesCondition<T extends JavaAccess<?>> extends AllAttributesMatchCondition<T> {
    private final Function<JavaClass, ? extends Collection<T>> getRelevantAccesses;

    ClassOnlyAccessesCondition(DescribedPredicate<? super T> predicate, Function<JavaClass, ? extends Collection<T>> getRelevantAccesses) {
        super("only access targets where " + predicate.getDescription(), new JavaAccessCondition<>(predicate));
        this.getRelevantAccesses = getRelevantAccesses;
    }

    @Override
    Collection<T> relevantAttributes(JavaClass item) {
        return getRelevantAccesses.apply(item);
    }
}
