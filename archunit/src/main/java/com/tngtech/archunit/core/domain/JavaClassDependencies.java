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

import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Supplier;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.core.domain.JavaAnnotation.DefaultParameterVisitor;
import com.tngtech.archunit.core.domain.properties.HasAnnotations;

import static com.google.common.base.Suppliers.memoize;
import static com.google.common.collect.Sets.union;

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

    private Set<Dependency> dependenciesFromAccesses(Set<JavaAccess<?>> accesses) {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (JavaAccess<?> access : accesses) {
            result.addAll(Dependency.tryCreateFromAccess(access).asSet());
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
        for (JavaField field : javaClass.getFields()) {
            result.addAll(Dependency.tryCreateFromField(field).asSet());
        }
        return result.build();
    }

    private Set<Dependency> returnTypeDependenciesFromSelf() {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (JavaMethod method : javaClass.getMethods()) {
            result.addAll(Dependency.tryCreateFromReturnType(method).asSet());
        }
        return result.build();
    }

    private Set<Dependency> methodParameterDependenciesFromSelf() {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (JavaMethod method : javaClass.getMethods()) {
            for (JavaClass parameter : method.getRawParameterTypes()) {
                result.addAll(Dependency.tryCreateFromParameter(method, parameter).asSet());
            }
        }
        return result.build();
    }

    private Set<Dependency> throwsDeclarationDependenciesFromSelf() {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (JavaCodeUnit codeUnit : javaClass.getCodeUnits()) {
            for (ThrowsDeclaration<? extends JavaCodeUnit> throwsDeclaration : codeUnit.getThrowsClause()) {
                result.addAll(Dependency.tryCreateFromThrowsDeclaration(throwsDeclaration).asSet());
            }
        }
        return result.build();
    }

    private Set<Dependency> constructorParameterDependenciesFromSelf() {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (JavaConstructor constructor : javaClass.getConstructors()) {
            for (JavaClass parameter : constructor.getRawParameterTypes()) {
                result.addAll(Dependency.tryCreateFromParameter(constructor, parameter).asSet());
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
        final ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (final JavaAnnotation<?> annotation : annotated.getAnnotations()) {
            result.addAll(Dependency.tryCreateFromAnnotation(annotation).asSet());
            annotation.accept(new DefaultParameterVisitor() {
                @Override
                public void visitClass(String propertyName, JavaClass javaClass) {
                    result.addAll(Dependency.tryCreateFromAnnotationMember(annotation, javaClass).asSet());
                }

                @Override
                public void visitEnumConstant(String propertyName, JavaEnumConstant enumConstant) {
                    result.addAll(Dependency.tryCreateFromAnnotationMember(annotation, enumConstant.getDeclaringClass()).asSet());
                }

                @Override
                public void visitAnnotation(String propertyName, JavaAnnotation<?> memberAnnotation) {
                    result.addAll(Dependency.tryCreateFromAnnotationMember(annotation, memberAnnotation.getRawType()).asSet());
                    memberAnnotation.accept(this);
                }
            });
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
            result.addAll(Dependency.tryCreateFromField(field).asSet());
        }
        return result;
    }

    private Set<Dependency> returnTypeDependenciesToSelf() {
        Set<Dependency> result = new HashSet<>();
        for (JavaMethod method : javaClass.getMethodsWithReturnTypeOfSelf()) {
            result.addAll(Dependency.tryCreateFromReturnType(method).asSet());
        }
        return result;
    }

    private Set<Dependency> methodParameterDependenciesToSelf() {
        Set<Dependency> result = new HashSet<>();
        for (JavaMethod method : javaClass.getMethodsWithParameterTypeOfSelf()) {
            result.addAll(Dependency.tryCreateFromParameter(method, javaClass).asSet());
        }
        return result;
    }

    private Set<Dependency> throwsDeclarationDependenciesToSelf() {
        Set<Dependency> result = new HashSet<>();
        for (ThrowsDeclaration<? extends JavaCodeUnit> throwsDeclaration : getThrowsDeclarationsWithTypeOfClass()) {
            result.addAll(Dependency.tryCreateFromThrowsDeclaration(throwsDeclaration).asSet());
        }
        return result;
    }

    private Set<Dependency> constructorParameterDependenciesToSelf() {
        Set<Dependency> result = new HashSet<>();
        for (JavaConstructor constructor : javaClass.getConstructorsWithParameterTypeOfSelf()) {
            result.addAll(Dependency.tryCreateFromParameter(constructor, javaClass).asSet());
        }
        return result;
    }

    private Iterable<? extends Dependency> annotationDependenciesToSelf() {
        Set<Dependency> result = new HashSet<>();
        for (JavaAnnotation<?> annotation : annotationsWithTypeOfClass) {
            result.addAll(Dependency.tryCreateFromAnnotation(annotation).asSet());
        }
        for (JavaAnnotation<?> annotation : annotationsWithParameterTypeOfClass) {
            result.addAll(Dependency.tryCreateFromAnnotationMember(annotation, javaClass).asSet());
        }
        return result;
    }
}
