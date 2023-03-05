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
package com.tngtech.archunit.library.modules.syntax;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.cycle_detection.rules.CycleArchCondition;
import com.tngtech.archunit.library.modules.ArchModule;
import com.tngtech.archunit.library.modules.ModuleDependency;

import static com.tngtech.archunit.core.domain.Dependency.Predicates.dependency;
import static com.tngtech.archunit.lang.SimpleConditionEvent.violated;

class ModulesShouldInternal<DESCRIPTOR extends ArchModule.Descriptor> implements ModulesShould<DESCRIPTOR> {
    private final Function<ArchCondition<ArchModule<DESCRIPTOR>>, ArchRule> createRule;

    ModulesShouldInternal(Function<ArchCondition<ArchModule<DESCRIPTOR>>, ArchRule> createRule) {
        this.createRule = createRule;
    }

    @Override
    public ModulesRule respectTheirAllowedDependencies(DescribedPredicate<? super ModuleDependency<DESCRIPTOR>> allowedDependencyPredicate, ModuleDependencyScope dependencyScope) {
        return new ModulesRuleInternal<>(
                createRule,
                relevantClassDependencyPredicate -> new RespectTheirAllowedDependenciesCondition<>(allowedDependencyPredicate.forSubtype(), dependencyScope, relevantClassDependencyPredicate)
        );
    }

    @Override
    public ModulesRule respectTheirAllowedDependencies(AllowedModuleDependencies allowedDependencies, ModuleDependencyScope dependencyScope) {
        return respectTheirAllowedDependencies(
                allowedDependencies.asPredicate(),
                dependencyScope
        );
    }

    @Override
    public ModulesRule onlyDependOnEachOtherThroughClassesThat(DescribedPredicate<? super JavaClass> predicate) {
        return new ModulesRuleInternal<>(
                createRule,
                relevantClassDependencyPredicate -> new ArchCondition<ArchModule<DESCRIPTOR>>("only depend on each other through classes that " + predicate.getDescription()) {
                    @Override
                    public void check(ArchModule<DESCRIPTOR> module, ConditionEvents events) {
                        module.getModuleDependenciesFromSelf().stream()
                                .flatMap(moduleDependency -> moduleDependency.toClassDependencies().stream())
                                .filter(relevantClassDependencyPredicate)
                                .filter(classDependency -> !predicate.test(classDependency.getTargetClass()))
                                .forEach(classDependency -> events.add(SimpleConditionEvent.violated(classDependency, classDependency.getDescription())));
                    }
                }
        );
    }

    @Override
    public ModulesRule beFreeOfCycles() {
        return new ModulesRuleInternal<>(
                createRule,
                relevantClassDependencyPredicate -> CycleArchCondition.<ArchModule<DESCRIPTOR>>builder()
                        .retrieveClassesBy(Function.identity())
                        .retrieveDescriptionBy(ArchModule::getName)
                        .retrieveOutgoingDependenciesBy(ArchModule::getClassDependenciesFromSelf)
                        .onlyConsiderDependencies(relevantClassDependencyPredicate)
                        .build()
        );
    }

    private static class RespectTheirAllowedDependenciesCondition<DESCRIPTOR extends ArchModule.Descriptor> extends ArchCondition<ArchModule<DESCRIPTOR>> {
        private final DescribedPredicate<ModuleDependency<DESCRIPTOR>> allowedModuleDependencyPredicate;
        private final ModuleDependencyScope dependencyScope;
        private final Predicate<Dependency> relevantClassDependencyPredicate;
        private Collection<ArchModule<DESCRIPTOR>> allModules;

        RespectTheirAllowedDependenciesCondition(
                DescribedPredicate<ModuleDependency<DESCRIPTOR>> allowedModuleDependencyPredicate,
                ModuleDependencyScope dependencyScope,
                Predicate<Dependency> relevantClassDependencyPredicate
        ) {
            super("respect their allowed dependencies %s %s", allowedModuleDependencyPredicate.getDescription(), dependencyScope.getDescription());
            this.allowedModuleDependencyPredicate = allowedModuleDependencyPredicate;
            this.dependencyScope = dependencyScope;
            this.relevantClassDependencyPredicate = relevantClassDependencyPredicate;
        }

        @Override
        public void init(Collection<ArchModule<DESCRIPTOR>> allModules) {
            this.allModules = allModules;
        }

        @Override
        public void check(ArchModule<DESCRIPTOR> module, ConditionEvents events) {
            Set<ModuleDependency<DESCRIPTOR>> actualDependencies = module.getModuleDependenciesFromSelf();

            actualDependencies.stream()
                    .filter(it -> !allowedModuleDependencyPredicate.test(it))
                    .filter(it -> it.toClassDependencies().stream().anyMatch(relevantClassDependencyPredicate))
                    .forEach(it -> events.add(violated(it, it.getDescription())));

            module.getUndefinedDependencies().stream()
                    .filter(dependencyScope.asPredicate(allModules))
                    .filter(relevantClassDependencyPredicate)
                    .forEach(it -> events.add(violated(it, "Dependency not contained in any module: " + it.getDescription())));
        }
    }

    private static class ModulesRuleInternal<DESCRIPTOR extends ArchModule.Descriptor> implements ModulesRule {
        private final Function<ArchCondition<ArchModule<DESCRIPTOR>>, ArchRule> createRule;
        private final Function<ArchRule, ArchRule> modifyRule;
        private final Function<Predicate<Dependency>, ArchCondition<ArchModule<DESCRIPTOR>>> createCondition;
        private final Predicate<Dependency> relevantClassDependencyPredicate;

        ModulesRuleInternal(
                Function<ArchCondition<ArchModule<DESCRIPTOR>>, ArchRule> createRule,
                Function<Predicate<Dependency>, ArchCondition<ArchModule<DESCRIPTOR>>> createCondition
        ) {
            this(createRule, createCondition, x -> x, __ -> true);
        }

        private ModulesRuleInternal(
                Function<ArchCondition<ArchModule<DESCRIPTOR>>, ArchRule> createRule,
                Function<Predicate<Dependency>, ArchCondition<ArchModule<DESCRIPTOR>>> createCondition,
                Function<ArchRule, ArchRule> modifyRule,
                Predicate<Dependency> relevantClassDependencyPredicate
        ) {
            this.createRule = createRule;
            this.createCondition = createCondition;
            this.modifyRule = modifyRule;
            this.relevantClassDependencyPredicate = relevantClassDependencyPredicate;
        }

        @Override
        public String getDescription() {
            return createRule().getDescription();
        }

        @Override
        public void check(JavaClasses classes) {
            createRule().check(classes);
        }

        @Override
        public EvaluationResult evaluate(JavaClasses classes) {
            return createRule().evaluate(classes);
        }

        @Override
        public ModulesRule as(String newDescription) {
            return new ModulesRuleInternal<>(
                    createRule,
                    createCondition,
                    rule -> modifyRule.apply(rule).as(newDescription),
                    relevantClassDependencyPredicate);
        }

        @Override
        public ModulesRule because(String reason) {
            return new ModulesRuleInternal<>(
                    createRule,
                    createCondition,
                    rule -> modifyRule.apply(rule).because(reason),
                    relevantClassDependencyPredicate);
        }

        @Override
        public ModulesRule allowEmptyShould(boolean allowEmptyShould) {
            return new ModulesRuleInternal<>(
                    createRule,
                    createCondition,
                    rule -> modifyRule.apply(rule).allowEmptyShould(allowEmptyShould),
                    relevantClassDependencyPredicate);
        }

        @Override
        public ModulesRule ignoreDependency(Class<?> origin, Class<?> target) {
            return ignoreDependency(dependency(origin, target));
        }

        @Override
        public ModulesRule ignoreDependency(String originFullyQualifiedClassName, String targetFullyQualifiedClassName) {
            return ignoreDependency(dependency(originFullyQualifiedClassName, targetFullyQualifiedClassName));
        }

        @Override
        public ModulesRule ignoreDependency(Predicate<? super JavaClass> originPredicate, Predicate<? super JavaClass> targetPredicate) {
            return ignoreDependency(dependency -> originPredicate.test(dependency.getOriginClass()) && targetPredicate.test(dependency.getTargetClass()));
        }

        @Override
        public ModulesRule ignoreDependency(Predicate<? super Dependency> dependencyPredicate) {
            return new ModulesRuleInternal<>(
                    createRule,
                    createCondition,
                    modifyRule,
                    dependency -> this.relevantClassDependencyPredicate.test(dependency) && !dependencyPredicate.test(dependency));
        }

        private ArchRule createRule() {
            return modifyRule.apply(createRule.apply(createCondition.apply(relevantClassDependencyPredicate)));
        }
    }
}
