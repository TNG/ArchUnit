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
package com.tngtech.archunit.library.dependencies;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ArchRule.Transformation.As;
import com.tngtech.archunit.lang.ArchRule.Transformation.Because;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.Priority;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.Dependency.Predicates.dependency;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.priority;

public final class SliceRule implements ArchRule {
    private final Slices.Transformer inputTransformer;
    private final Priority priority;
    private final List<Transformation> transformations;
    private final DescribedPredicate<Dependency> ignoreDependency;
    private final ConditionFactory conditionFactory;

    SliceRule(Slices.Transformer inputTransformer, Priority priority, ConditionFactory conditionFactory) {
        this(inputTransformer, priority, Collections.<Transformation>emptyList(), DescribedPredicate.<Dependency>alwaysFalse(), conditionFactory);
    }

    private SliceRule(Slices.Transformer inputTransformer, Priority priority, List<Transformation> transformations,
            DescribedPredicate<Dependency> ignoreDependency, ConditionFactory conditionFactory) {
        this.inputTransformer = inputTransformer;
        this.priority = priority;
        this.transformations = transformations;
        this.ignoreDependency = ignoreDependency;
        this.conditionFactory = conditionFactory;
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public void check(JavaClasses classes) {
        getArchRule().check(classes);
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public SliceRule because(String reason) {
        return copyWithTransformation(new Because(reason));
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public EvaluationResult evaluate(JavaClasses classes) {
        return getArchRule().evaluate(classes);
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public String getDescription() {
        return getArchRule().getDescription();
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public SliceRule as(String newDescription) {
        return copyWithTransformation(new As(newDescription));
    }

    @PublicAPI(usage = ACCESS)
    public SliceRule ignoreDependency(Class<?> origin, Class<?> target) {
        return ignoreDependency(equivalentTo(origin), equivalentTo(target));
    }

    @PublicAPI(usage = ACCESS)
    public SliceRule ignoreDependency(String origin, String target) {
        return ignoreDependency(name(origin), name(target));
    }

    @PublicAPI(usage = ACCESS)
    public SliceRule ignoreDependency(DescribedPredicate<? super JavaClass> origin, DescribedPredicate<? super JavaClass> target) {
        return new SliceRule(inputTransformer, priority, transformations, ignoreDependency.or(dependency(origin, target)), conditionFactory);
    }

    private SliceRule copyWithTransformation(Transformation transformation) {
        List<Transformation> newTransformations =
                ImmutableList.<Transformation>builder().addAll(transformations).add(transformation).build();
        return new SliceRule(inputTransformer, priority, newTransformations, ignoreDependency, conditionFactory);
    }

    private ArchRule getArchRule() {
        ArchRule rule = priority(priority).all(inputTransformer).should(conditionFactory.create(inputTransformer, not(ignoreDependency)));
        for (Transformation transformation : transformations) {
            rule = transformation.apply(rule);
        }
        return rule;
    }

    @Override
    public String toString() {
        return getDescription();
    }

    interface ConditionFactory {
        ArchCondition<Slice> create(Slices.Transformer transformer, DescribedPredicate<Dependency> predicate);
    }
}
