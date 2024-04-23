/*
 * Copyright 2014-2024 TNG Technology Consulting GmbH
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
import java.util.function.Function;

import com.google.common.base.Joiner;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaClass;

class AllAccessesCondition extends AllAttributesMatchCondition<JavaAccess<?>, JavaClass> {
    private final Function<JavaClass, ? extends Collection<JavaAccess<?>>> getRelevantAccesses;

    AllAccessesCondition(String prefix, DescribedPredicate<JavaAccess<?>> predicate,
            Function<JavaClass, ? extends Collection<JavaAccess<?>>> getRelevantAccesses) {
        super(Joiner.on(" ").join(prefix, predicate.getDescription()), new JavaAccessCondition<>(predicate));
        this.getRelevantAccesses = getRelevantAccesses;
    }

    @Override
    Collection<JavaAccess<?>> relevantAttributes(JavaClass item) {
        return getRelevantAccesses.apply(item);
    }
}
