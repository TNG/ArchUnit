/*
 * Copyright 2017 TNG Technology Consulting GmbH
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
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.base.Guava.Iterables.filter;
import static com.tngtech.archunit.core.domain.Dependency.Predicates.dependency;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.all;

public final class SliceRule implements ArchRule {
    private final Slices.Transformer inputTransformer;
    private final List<Transformation> transformations;
    private final DescribedPredicate<Dependency> ignoreDependency;

    SliceRule(Slices.Transformer inputTransformer) {
        this(inputTransformer, Collections.<Transformation>emptyList(), DescribedPredicate.<Dependency>alwaysFalse());
    }

    private SliceRule(Slices.Transformer inputTransformer, List<Transformation> transformations, DescribedPredicate<Dependency> ignoreDependency) {
        this.inputTransformer = inputTransformer;
        this.transformations = transformations;
        this.ignoreDependency = ignoreDependency;
    }

    @Override
    public void check(JavaClasses classes) {
        getArchRule().check(classes);
    }

    @Override
    public SliceRule because(String reason) {
        return copyWithTransformation(new Because(reason));
    }

    @Override
    public EvaluationResult evaluate(JavaClasses classes) {
        return getArchRule().evaluate(classes);
    }

    @Override
    public String getDescription() {
        return getArchRule().getDescription();
    }

    @Override
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
        return new SliceRule(inputTransformer, transformations, ignoreDependency.or(dependency(origin, target)));
    }

    private SliceRule copyWithTransformation(Transformation transformation) {
        List<Transformation> newTransformations =
                ImmutableList.<Transformation>builder().addAll(transformations).add(transformation).build();
        return new SliceRule(inputTransformer, newTransformations, ignoreDependency);
    }

    private ArchCondition<Slice> notDependOnEachOther(final Slices.Transformer inputTransformer) {
        return new ArchCondition<Slice>("not depend on each other") {
            @Override
            public void check(Slice slice, ConditionEvents events) {
                Iterable<Dependency> dependencies = filter(slice.getDependencies(), not(ignoreDependency));
                Slices dependencySlices = inputTransformer.transform(dependencies);
                for (Slice dependencySlice : dependencySlices) {
                    SliceDependency dependency = SliceDependency.of(slice, dependencySlice);
                    events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                }
            }
        };
    }

    private ArchRule getArchRule() {
        ArchRule rule = all(inputTransformer).should(notDependOnEachOther(inputTransformer));
        for (Transformation transformation : transformations) {
            rule = transformation.apply(rule);
        }
        return rule;
    }
}
