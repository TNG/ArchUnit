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
package com.tngtech.archunit.library.modules.syntax;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.syntax.ClassesThatInternal;
import com.tngtech.archunit.lang.syntax.elements.ClassesThat;
import com.tngtech.archunit.library.cycle_detection.rules.CycleArchCondition;
import com.tngtech.archunit.library.modules.AnnotationDescriptor;
import com.tngtech.archunit.library.modules.ArchModule;
import com.tngtech.archunit.library.modules.ModuleDependency;

import static com.tngtech.archunit.core.domain.Dependency.Predicates.dependency;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.SimpleConditionEvent.violated;

class ModulesShouldInternal<DESCRIPTOR extends ArchModule.Descriptor> implements ModulesShould<DESCRIPTOR> {
    final Function<ArchCondition<ArchModule<DESCRIPTOR>>, ArchRule> createRule;

    ModulesShouldInternal(Function<ArchCondition<ArchModule<DESCRIPTOR>>, ArchRule> createRule) {
        this.createRule = createRule;
    }

    @Override
    public ModulesRule<DESCRIPTOR> respectTheirAllowedDependencies(DescribedPredicate<? super ModuleDependency<DESCRIPTOR>> allowedDependencyPredicate, ModuleDependencyScope dependencyScope) {
        return new ModulesRuleInternal<>(
                createRule,
                relevantClassDependencyPredicate -> new RespectTheirAllowedDependenciesCondition<>(allowedDependencyPredicate.forSubtype(), dependencyScope, relevantClassDependencyPredicate)
        );
    }

    @Override
    public ModulesRule<DESCRIPTOR> respectTheirAllowedDependencies(AllowedModuleDependencies allowedDependencies, ModuleDependencyScope dependencyScope) {
        return respectTheirAllowedDependencies(
                allowedDependencies.asPredicate(),
                dependencyScope
        );
    }

    @Override
    public ModulesRule<DESCRIPTOR> onlyDependOnEachOtherThroughClassesThat(DescribedPredicate<? super JavaClass> predicate) {
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
    public ClassesThat<ModulesRule<DESCRIPTOR>> onlyDependOnEachOtherThroughClassesThat() {
        return new ClassesThatInternal<>(this::onlyDependOnEachOtherThroughClassesThat);
    }

    @Override
    public ModulesRule<DESCRIPTOR> beFreeOfCycles() {
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

    static class ModulesByAnnotationShouldInternal<ANNOTATION extends Annotation> extends ModulesShouldInternal<AnnotationDescriptor<ANNOTATION>> implements ModulesByAnnotationShould<ANNOTATION> {

        ModulesByAnnotationShouldInternal(Function<ArchCondition<ArchModule<AnnotationDescriptor<ANNOTATION>>>, ArchRule> createRule) {
            super(createRule);
        }

        @Override
        public ModulesByAnnotationRule<ANNOTATION> respectTheirAllowedDependenciesDeclaredIn(String annotationPropertyName, ModuleDependencyScope dependencyScope) {
            return new ModulesByAnnotationRuleInternal<>(
                    respectTheirAllowedDependencies(
                            DescribedPredicate.describe(
                                    "declared in '" + annotationPropertyName + "'",
                                    moduleDependency -> {
                                        Set<String> allowedDependencies = getAllowedDependencies(moduleDependency.getOrigin().getDescriptor().getAnnotation(), annotationPropertyName);
                                        return allowedDependencies.contains(moduleDependency.getTarget().getName());
                                    }),
                            dependencyScope)
            );
        }

        @Override
        public ModulesByAnnotationRule<ANNOTATION> onlyDependOnEachOtherThroughPackagesDeclaredIn(String annotationPropertyName) {
            return new ModulesByAnnotationRuleInternal<>(new ModulesRuleInternal<>(
                    createRule,
                    relevantClassDependencyPredicate -> new ArchCondition<ArchModule<AnnotationDescriptor<ANNOTATION>>>(
                            String.format("only depend on each other through packages declared in '%s'", annotationPropertyName)
                    ) {
                        @Override
                        public void check(ArchModule<AnnotationDescriptor<ANNOTATION>> module, ConditionEvents events) {
                            // note that while this would be simpler to write via getClassDependenciesToSelf() we don't go this way because resolving
                            // reverse dependencies is more expensive. So as a library function it makes sense to choose the more performant way instead.
                            module.getModuleDependenciesFromSelf().forEach(moduleDependency -> {
                                ANNOTATION descriptor = moduleDependency.getTarget().getDescriptor().getAnnotation();
                                String[] apiPackageIdentifiers = getStringArrayAnnotationProperty(descriptor, annotationPropertyName);
                                Predicate<JavaClass> predicate = resideInAnyPackage(apiPackageIdentifiers);

                                moduleDependency.toClassDependencies().stream()
                                        .filter(relevantClassDependencyPredicate)
                                        .filter(classDependency -> !predicate.test(classDependency.getTargetClass()))
                                        .forEach(classDependency -> events.add(SimpleConditionEvent.violated(classDependency, classDependency.getDescription())));
                            });
                        }
                    }
            ));
        }

        private Set<String> getAllowedDependencies(Annotation annotation, String annotationPropertyName) {
            String[] allowedDependencies = getStringArrayAnnotationProperty(annotation, annotationPropertyName);
            return ImmutableSet.copyOf(allowedDependencies);
        }

        private String[] getStringArrayAnnotationProperty(Annotation annotation, String annotationPropertyName) {
            Object value = getAnnotationProperty(annotation, annotationPropertyName);
            try {
                return (String[]) value;
            } catch (ClassCastException e) {
                String message = String.format("Property @%s.%s() must be of type String[]", annotation.annotationType().getSimpleName(), annotationPropertyName);
                throw new IllegalArgumentException(message, e);
            }
        }

        private static Object getAnnotationProperty(Annotation annotation, String annotationPropertyName) {
            try {
                return annotation.annotationType().getMethod(annotationPropertyName).invoke(annotation);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                String message = String.format("Could not invoke @%s.%s()", annotation.annotationType().getSimpleName(), annotationPropertyName);
                throw new IllegalArgumentException(message, e);
            }
        }

        private static class ModulesByAnnotationRuleInternal<ANNOTATION extends Annotation> implements ModulesByAnnotationRule<ANNOTATION> {
            private final ModulesRule<AnnotationDescriptor<ANNOTATION>> delegate;

            ModulesByAnnotationRuleInternal(ModulesRule<AnnotationDescriptor<ANNOTATION>> delegate) {
                this.delegate = delegate;
            }

            @Override
            public ModulesByAnnotationShouldInternal<ANNOTATION> andShould() {
                return new ModulesByAnnotationShouldInternal<>(this::andShould);
            }

            @Override
            public ArchRule andShould(ArchCondition<? super ArchModule<AnnotationDescriptor<ANNOTATION>>> condition) {
                return delegate.andShould(condition);
            }

            @Override
            public String getDescription() {
                return delegate.getDescription();
            }

            @Override
            public void check(JavaClasses classes) {
                delegate.check(classes);
            }

            @Override
            public EvaluationResult evaluate(JavaClasses classes) {
                return delegate.evaluate(classes);
            }

            @Override
            public ModulesByAnnotationRule<ANNOTATION> as(String newDescription) {
                return new ModulesByAnnotationRuleInternal<>(delegate.as(newDescription));
            }

            @Override
            public ModulesByAnnotationRule<ANNOTATION> because(String reason) {
                return new ModulesByAnnotationRuleInternal<>(delegate.because(reason));
            }

            @Override
            public ModulesByAnnotationRule<ANNOTATION> allowEmptyShould(boolean allowEmptyShould) {
                return new ModulesByAnnotationRuleInternal<>(delegate.allowEmptyShould(allowEmptyShould));
            }

            @Override
            public ModulesByAnnotationRule<ANNOTATION> ignoreDependency(Class<?> origin, Class<?> target) {
                return new ModulesByAnnotationRuleInternal<>(delegate.ignoreDependency(origin, target));
            }

            @Override
            public ModulesByAnnotationRule<ANNOTATION> ignoreDependency(String originFullyQualifiedClassName, String targetFullyQualifiedClassName) {
                return new ModulesByAnnotationRuleInternal<>(delegate.ignoreDependency(originFullyQualifiedClassName, targetFullyQualifiedClassName));
            }

            @Override
            public ModulesByAnnotationRule<ANNOTATION> ignoreDependency(Predicate<? super JavaClass> originPredicate, Predicate<? super JavaClass> targetPredicate) {
                return new ModulesByAnnotationRuleInternal<>(delegate.ignoreDependency(originPredicate, targetPredicate));
            }

            @Override
            public ModulesByAnnotationRule<ANNOTATION> ignoreDependency(Predicate<? super Dependency> dependencyPredicate) {
                return new ModulesByAnnotationRuleInternal<>(delegate.ignoreDependency(dependencyPredicate));
            }
        }
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

    private static class ModulesRuleInternal<DESCRIPTOR extends ArchModule.Descriptor> implements ModulesRule<DESCRIPTOR> {
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
        public ModulesShould<DESCRIPTOR> andShould() {
            return new ModulesShouldInternal<>(this::andShould);
        }

        @Override
        public ArchRule andShould(ArchCondition<? super ArchModule<DESCRIPTOR>> condition) {
            return createRule(createCondition().and(condition.as("should " + condition.getDescription())));
        }

        @Override
        public ModulesRule<DESCRIPTOR> as(String newDescription) {
            return new ModulesRuleInternal<>(
                    createRule,
                    createCondition,
                    rule -> modifyRule.apply(rule).as(newDescription),
                    relevantClassDependencyPredicate);
        }

        @Override
        public ModulesRule<DESCRIPTOR> because(String reason) {
            return new ModulesRuleInternal<>(
                    createRule,
                    createCondition,
                    rule -> modifyRule.apply(rule).because(reason),
                    relevantClassDependencyPredicate);
        }

        @Override
        public ModulesRule<DESCRIPTOR> allowEmptyShould(boolean allowEmptyShould) {
            return new ModulesRuleInternal<>(
                    createRule,
                    createCondition,
                    rule -> modifyRule.apply(rule).allowEmptyShould(allowEmptyShould),
                    relevantClassDependencyPredicate);
        }

        @Override
        public ModulesRule<DESCRIPTOR> ignoreDependency(Class<?> origin, Class<?> target) {
            return ignoreDependency(dependency(origin, target));
        }

        @Override
        public ModulesRule<DESCRIPTOR> ignoreDependency(String originFullyQualifiedClassName, String targetFullyQualifiedClassName) {
            return ignoreDependency(dependency(originFullyQualifiedClassName, targetFullyQualifiedClassName));
        }

        @Override
        public ModulesRule<DESCRIPTOR> ignoreDependency(Predicate<? super JavaClass> originPredicate, Predicate<? super JavaClass> targetPredicate) {
            return ignoreDependency(dependency -> originPredicate.test(dependency.getOriginClass()) && targetPredicate.test(dependency.getTargetClass()));
        }

        @Override
        public ModulesRule<DESCRIPTOR> ignoreDependency(Predicate<? super Dependency> dependencyPredicate) {
            return new ModulesRuleInternal<>(
                    createRule,
                    createCondition,
                    modifyRule,
                    dependency -> this.relevantClassDependencyPredicate.test(dependency) && !dependencyPredicate.test(dependency));
        }

        private ArchRule createRule() {
            return createRule(createCondition());
        }

        private ArchCondition<ArchModule<DESCRIPTOR>> createCondition() {
            return createCondition.apply(relevantClassDependencyPredicate);
        }

        private ArchRule createRule(ArchCondition<ArchModule<DESCRIPTOR>> condition) {
            return modifyRule.apply(createRule.apply(condition));
        }
    }
}
