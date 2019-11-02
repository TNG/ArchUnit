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
package com.tngtech.archunit.core.domain;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Supplier;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.core.domain.properties.HasAnnotations;

import static com.google.common.base.Suppliers.memoize;
import static com.google.common.collect.Sets.union;
import static com.tngtech.archunit.core.domain.properties.HasReturnType.Functions.GET_RAW_RETURN_TYPE;
import static com.tngtech.archunit.core.domain.properties.HasType.Functions.GET_RAW_TYPE;

class JavaClassDependencies {
    private final JavaClass javaClass;
    private final Set<JavaField> fieldsWithTypeOfClass;
    private final Set<JavaMethod> methodsWithParameterTypeOfClass;
    private final Set<JavaMethod> methodsWithReturnTypeOfClass;
    private final Set<ThrowsDeclaration<JavaMethod>> methodsWithThrowsDeclarationTypeOfClass;
    private final Set<JavaConstructor> constructorsWithParameterTypeOfClass;
    private final Set<ThrowsDeclaration<JavaConstructor>> constructorsWithThrowsDeclarationTypeOfClass;
    private final Set<JavaAnnotation<?>> annotationsWithTypeOfClass;
    private final Set<JavaAnnotation<?>> annotationsWithParameterTypeOfClass;
    private final Set<JavaMember> membersWithAnnotationTypeOfClass;
    private final Set<JavaMember> membersWithAnnotationParameterTypeOfClass;
    private final Supplier<Set<Dependency>> directDependenciesFromClass;
    private final Supplier<Set<Dependency>> directDependenciesToClass;

    JavaClassDependencies(JavaClass javaClass, ImportContext context) {
        this.javaClass = javaClass;
        this.fieldsWithTypeOfClass = context.getFieldsOfType(javaClass);
        this.methodsWithParameterTypeOfClass = context.getMethodsWithParameterOfType(javaClass);
        this.methodsWithReturnTypeOfClass = context.getMethodsWithReturnType(javaClass);
        this.methodsWithThrowsDeclarationTypeOfClass = context.getMethodThrowsDeclarationsOfType(javaClass);
        this.constructorsWithParameterTypeOfClass = context.getConstructorsWithParameterOfType(javaClass);
        this.constructorsWithThrowsDeclarationTypeOfClass = context.getConstructorThrowsDeclarationsOfType(javaClass);
        this.annotationsWithTypeOfClass = context.getAnnotationsOfType(javaClass);
        this.annotationsWithParameterTypeOfClass = context.getAnnotationsWithParameterOfType(javaClass);
        this.membersWithAnnotationTypeOfClass = context.getMembersAnnotatedWithType(javaClass);
        this.membersWithAnnotationParameterTypeOfClass = context.getMembersWithParametersOfType(javaClass);
        this.directDependenciesFromClass = getDirectDependenciesFromClassSupplier();
        this.directDependenciesToClass = getDirectDependenciesToClassSupplier();
    }

    private Supplier<Set<Dependency>> getDirectDependenciesFromClassSupplier() {
        return memoize(new Supplier<Set<Dependency>>() {
            @Override
            public Set<Dependency> get() {
                ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
                result.addAll(dependenciesFromAccesses(javaClass.getAccessesFromSelf()));
                result.addAll(inheritanceDependenciesFromSelf());
                result.addAll(fieldDependenciesFromSelf());
                result.addAll(returnTypeDependenciesFromSelf());
                result.addAll(methodParameterDependenciesFromSelf());
                result.addAll(throwsDeclarationDependenciesFromSelf());
                result.addAll(constructorParameterDependenciesFromSelf());
                result.addAll(annotationDependenciesFromSelf());
                return result.build();
            }
        });
    }

    private Supplier<Set<Dependency>> getDirectDependenciesToClassSupplier() {
        return memoize(new Supplier<Set<Dependency>>() {
            @Override
            public Set<Dependency> get() {
                ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
                result.addAll(dependenciesFromAccesses(javaClass.getAccessesToSelf()));
                result.addAll(inheritanceDependenciesToSelf());
                result.addAll(fieldDependenciesToSelf());
                result.addAll(returnTypeDependenciesToSelf());
                result.addAll(methodParameterDependenciesToSelf());
                result.addAll(throwsDeclarationDependenciesToSelf());
                result.addAll(constructorParameterDependenciesToSelf());
                result.addAll(annotationDependenciesToSelf());
                return result.build();
            }
        });
    }

    Set<Dependency> getDirectDependenciesFromClass() {
        return directDependenciesFromClass.get();
    }

    Set<Dependency> getDirectDependenciesToClass() {
        return directDependenciesToClass.get();
    }

    Set<JavaField> getFieldsWithTypeOfClass() {
        return fieldsWithTypeOfClass;
    }

    Set<JavaMethod> getMethodsWithParameterTypeOfClass() {
        return methodsWithParameterTypeOfClass;
    }

    Set<JavaMethod> getMethodsWithReturnTypeOfClass() {
        return methodsWithReturnTypeOfClass;
    }

    Set<ThrowsDeclaration<JavaMethod>> getMethodThrowsDeclarationsWithTypeOfClass() {
        return methodsWithThrowsDeclarationTypeOfClass;
    }

    Set<JavaConstructor> getConstructorsWithParameterTypeOfClass() {
        return constructorsWithParameterTypeOfClass;
    }

    Set<ThrowsDeclaration<JavaConstructor>> getConstructorsWithThrowsDeclarationTypeOfClass() {
        return constructorsWithThrowsDeclarationTypeOfClass;
    }

    private Set<ThrowsDeclaration<? extends JavaCodeUnit>> getThrowsDeclarationsWithTypeOfClass() {
        return union(methodsWithThrowsDeclarationTypeOfClass, constructorsWithThrowsDeclarationTypeOfClass);
    }

    Set<JavaAnnotation<?>> getAnnotationsWithTypeOfClass() {
        return annotationsWithTypeOfClass;
    }

    private Set<JavaAnnotation<?>> getAnnotationsWithParameterTypeOfClass() {
        return annotationsWithParameterTypeOfClass;
    }

    private Set<JavaMember> getMembersWithAnnotationTypeOfClass() {
        return membersWithAnnotationTypeOfClass;
    }

    private Set<JavaMember> getMembersWithAnnotationParameterTypeOfClass() {
        return membersWithAnnotationParameterTypeOfClass;
    }

    private Set<Dependency> dependenciesFromAccesses(Set<JavaAccess<?>> accesses) {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (JavaAccess<?> access : filterNoSelfAccess(accesses)) {
            result.add(Dependency.from(access));
        }
        return result.build();
    }

    private Set<Dependency> inheritanceDependenciesFromSelf() {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (JavaClass superType : FluentIterable.from(javaClass.getInterfaces()).append(javaClass.getSuperClass().asSet())) {
            result.add(Dependency.fromInheritance(javaClass, superType));
        }
        return result.build();
    }

    private Set<Dependency> fieldDependenciesFromSelf() {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (JavaField field : nonPrimitive(javaClass.getFields(), GET_RAW_TYPE)) {
            result.add(Dependency.fromField(field));
        }
        return result.build();
    }

    private Set<Dependency> returnTypeDependenciesFromSelf() {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (JavaMethod method : nonPrimitive(javaClass.getMethods(), GET_RAW_RETURN_TYPE)) {
            result.add(Dependency.fromReturnType(method));
        }
        return result.build();
    }

    private Set<Dependency> methodParameterDependenciesFromSelf() {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (JavaMethod method : javaClass.getMethods()) {
            for (JavaClass parameter : nonPrimitive(method.getRawParameterTypes())) {
                result.add(Dependency.fromParameter(method, parameter));
            }
        }
        return result.build();
    }

    private Set<Dependency> throwsDeclarationDependenciesFromSelf() {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (JavaCodeUnit codeUnit : javaClass.getCodeUnits()) {
            for (ThrowsDeclaration<? extends JavaCodeUnit> throwsDeclaration : codeUnit.getThrowsClause()) {
                result.add(Dependency.fromThrowsDeclaration(throwsDeclaration));
            }
        }
        return result.build();
    }

    private Set<Dependency> constructorParameterDependenciesFromSelf() {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (JavaConstructor constructor : javaClass.getConstructors()) {
            for (JavaClass parameter : nonPrimitive(constructor.getRawParameterTypes())) {
                result.add(Dependency.fromParameter(constructor, parameter));
            }
        }
        return result.build();
    }

    private Set<Dependency> annotationDependenciesFromSelf() {
        return new ImmutableSet.Builder<Dependency>()
                .addAll(annotationDependencies(javaClass))
                .addAll(annotationDependencies(javaClass.getFields()))
                .addAll(annotationDependencies(javaClass.getMethods()))
                .addAll(annotationDependencies(javaClass.getConstructors()))
                .build();
    }

    private <T extends HasDescription & HasAnnotations<?>> Set<Dependency> annotationDependencies(Set<T> annotatedObjects) {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (T annotated : annotatedObjects) {
            result.addAll(annotationDependencies(annotated));
        }
        return result.build();
    }

    private <T extends HasDescription & HasAnnotations<?>> Set<Dependency> annotationDependencies(T annotated) {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (JavaAnnotation<?> annotation : annotated.getAnnotations()) {
            result.add(Dependency.fromAnnotation(annotation));
            result.addAll(annotationParametersDependencies(annotation));
        }
        return result.build();
    }

    private Set<Dependency> annotationParametersDependencies(JavaAnnotation<?> annotation) {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (Map.Entry<String, Object> entry : annotation.getProperties().entrySet()) {
            Object value = entry.getValue();
            if (value.getClass().isArray()) {
                if (!value.getClass().getComponentType().isPrimitive()) {
                    Object[] values = (Object[]) value;
                    for (Object o : values) {
                        result.addAll(annotationParameterDependencies(annotation, o));
                    }
                }
            } else {
                result.addAll(annotationParameterDependencies(annotation, value));
            }
        }
        return result.build();
    }

    private Set<Dependency> annotationParameterDependencies(JavaAnnotation<?> origin, Object value) {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        if (value instanceof JavaClass) {
            JavaClass annotationMember = (JavaClass) value;
            result.add(Dependency.fromAnnotationMember(origin, annotationMember));
        } else if (value instanceof JavaAnnotation<?>) {
            JavaAnnotation<?> nestedAnnotation = (JavaAnnotation<?>) value;
            result.add(Dependency.fromAnnotationMember(origin, nestedAnnotation.getRawType()));
            result.addAll(annotationParametersDependencies(nestedAnnotation));
        }
        return result.build();
    }

    private Set<Dependency> inheritanceDependenciesToSelf() {
        Set<Dependency> result = new HashSet<>();
        for (JavaClass subClass : javaClass.getSubClasses()) {
            result.add(Dependency.fromInheritance(subClass, javaClass));
        }
        return result;
    }

    private Set<Dependency> fieldDependenciesToSelf() {
        Set<Dependency> result = new HashSet<>();
        for (JavaField field : javaClass.getFieldsWithTypeOfSelf()) {
            result.add(Dependency.fromField(field));
        }
        return result;
    }

    private Set<Dependency> returnTypeDependenciesToSelf() {
        Set<Dependency> result = new HashSet<>();
        for (JavaMethod method : javaClass.getMethodsWithReturnTypeOfSelf()) {
            result.add(Dependency.fromReturnType(method));
        }
        return result;
    }

    private Set<Dependency> methodParameterDependenciesToSelf() {
        Set<Dependency> result = new HashSet<>();
        for (JavaMethod method : javaClass.getMethodsWithParameterTypeOfSelf()) {
            result.add(Dependency.fromParameter(method, javaClass));
        }
        return result;
    }

    private Set<Dependency> throwsDeclarationDependenciesToSelf() {
        Set<Dependency> result = new HashSet<>();
        for (ThrowsDeclaration<? extends JavaCodeUnit> throwsDeclaration : getThrowsDeclarationsWithTypeOfClass()) {
            result.add(Dependency.fromThrowsDeclaration(throwsDeclaration));
        }
        return result;
    }

    private Set<Dependency> constructorParameterDependenciesToSelf() {
        Set<Dependency> result = new HashSet<>();
        for (JavaConstructor constructor : javaClass.getConstructorsWithParameterTypeOfSelf()) {
            result.add(Dependency.fromParameter(constructor, javaClass));
        }
        return result;
    }

    private Iterable<? extends Dependency> annotationDependenciesToSelf() {
        Set<Dependency> result = new HashSet<>();
        for (JavaAnnotation<?> annotation : javaClass.getAnnotationsWithTypeOfSelf()) {
            result.add(Dependency.fromAnnotation(annotation));
        }
        for (JavaAnnotation<?> annotation : getAnnotationsWithParameterTypeOfClass()) {
            result.add(Dependency.fromAnnotationMember(annotation, javaClass));
        }
        for (JavaMember member : getMembersWithAnnotationTypeOfClass()) {
            JavaAnnotation<?> annotation = member.getAnnotationOfType(javaClass.getName());
            result.add(Dependency.fromAnnotation(annotation));
        }
        for (JavaMember member : getMembersWithAnnotationParameterTypeOfClass()) {
            JavaAnnotation<?> annotation = member.getAnnotationOfType(javaClass.getName());
            result.add(Dependency.fromAnnotationMember(annotation, javaClass));
        }
        return result;
    }

    private Set<JavaAccess<?>> filterNoSelfAccess(Set<? extends JavaAccess<?>> accesses) {
        Set<JavaAccess<?>> result = new HashSet<>();
        for (JavaAccess<?> access : accesses) {
            if (!access.getTargetOwner().equals(access.getOriginOwner())) {
                result.add(access);
            }
        }
        return result;
    }

    private Set<JavaClass> nonPrimitive(Collection<JavaClass> classes) {
        return nonPrimitive(classes, com.tngtech.archunit.base.Function.Functions.<JavaClass>identity());
    }

    private <T> Set<T> nonPrimitive(Collection<T> members, Function<? super T, JavaClass> getRelevantType) {
        ImmutableSet.Builder<T> result = ImmutableSet.builder();
        for (T member : members) {
            if (!getRelevantType.apply(member).isPrimitive()) {
                result.add(member);
            }
        }
        return result.build();
    }
}
