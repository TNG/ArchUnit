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
import java.util.HashSet;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

@PublicAPI(usage = ACCESS)
public final class AllDependenciesCondition extends AllAttributesMatchCondition<Dependency> {
    private final DescribedPredicate<? super Dependency> conditionPredicate;
    private final Function<JavaClass, ? extends Collection<Dependency>> javaClassToRelevantDependencies;
    private final DescribedPredicate<Dependency> ignorePredicate;

    AllDependenciesCondition(
            String description,
            final DescribedPredicate<? super Dependency> predicate,
            Function<JavaClass, ? extends Collection<Dependency>> javaClassToRelevantDependencies) {

        this(description, predicate, javaClassToRelevantDependencies, DescribedPredicate.<Dependency>alwaysFalse());
    }

    private AllDependenciesCondition(
            String description,
            final DescribedPredicate<? super Dependency> conditionPredicate,
            Function<JavaClass, ? extends Collection<Dependency>> javaClassToRelevantDependencies,
            DescribedPredicate<Dependency> ignorePredicate) {

        super(description, new DependencyCondition(conditionPredicate));
        this.conditionPredicate = checkNotNull(conditionPredicate);
        this.javaClassToRelevantDependencies = checkNotNull(javaClassToRelevantDependencies);
        this.ignorePredicate = checkNotNull(ignorePredicate);
    }

    @PublicAPI(usage = ACCESS)
    public AllDependenciesCondition ignoreDependency(DescribedPredicate<? super Dependency> ignorePredicate) {
        return new AllDependenciesCondition(getDescription(),
                conditionPredicate,
                javaClassToRelevantDependencies,
                this.ignorePredicate.or(ignorePredicate));
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public AllDependenciesCondition as(String description, Object... args) {
        return new AllDependenciesCondition(
                String.format(description, args),
                conditionPredicate,
                javaClassToRelevantDependencies,
                ignorePredicate);
    }

    @Override
    Collection<Dependency> relevantAttributes(JavaClass javaClass) {
        Collection<Dependency> result = new HashSet<>();
        for (Dependency dependency : javaClassToRelevantDependencies.apply(javaClass)) {
            if (!ignorePredicate.apply(dependency)) {
                result.add(dependency);
            }
        }
        return result;
    }
}
