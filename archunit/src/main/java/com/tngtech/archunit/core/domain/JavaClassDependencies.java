/*
 * Copyright 2014-2021 TNG Technology Consulting GmbH
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

import java.util.List;
import java.util.Set;

import com.google.common.base.Supplier;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.core.domain.JavaAnnotation.DefaultParameterVisitor;
import com.tngtech.archunit.core.domain.properties.HasAnnotations;

import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.base.Suppliers.memoize;
import static com.google.common.collect.Iterables.concat;
import static java.util.Collections.emptySet;

class JavaClassDependencies {
    private final JavaClass javaClass;
    private final Supplier<Set<Dependency>> directDependenciesFromClass;

    JavaClassDependencies(JavaClass javaClass) {
        this.javaClass = javaClass;
        this.directDependenciesFromClass = createDirectDependenciesFromClassSupplier();
    }

    private Supplier<Set<Dependency>> createDirectDependenciesFromClassSupplier() {
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
                result.addAll(instanceofCheckDependenciesFromSelf());
                result.addAll(referencedClassObjectDependenciesFromSelf());
                result.addAll(typeParameterDependenciesFromSelf());
                return result.build();
            }
        });
    }

    Set<Dependency> getDirectDependenciesFromClass() {
        return directDependenciesFromClass.get();
    }

    private Set<Dependency> dependenciesFromAccesses(Set<JavaAccess<?>> accesses) {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (JavaAccess<?> access : accesses) {
            result.addAll(Dependency.tryCreateFromAccess(access));
        }
        return result.build();
    }

    private Set<Dependency> inheritanceDependenciesFromSelf() {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (JavaClass supertype : FluentIterable.from(javaClass.getRawInterfaces()).append(javaClass.getRawSuperclass().asSet())) {
            result.add(Dependency.fromInheritance(javaClass, supertype));
        }
        result.addAll(genericSuperclassTypeArgumentDependencies());
        result.addAll(genericInterfaceTypeArgumentDependencies());
        return result.build();
    }

    private Set<Dependency> genericSuperclassTypeArgumentDependencies() {
        if (!javaClass.getSuperclass().isPresent() || !(javaClass.getSuperclass().get() instanceof JavaParameterizedType)) {
            return emptySet();
        }
        JavaParameterizedType genericSuperclass = (JavaParameterizedType) javaClass.getSuperclass().get();

        List<JavaType> actualTypeArguments = genericSuperclass.getActualTypeArguments();
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (JavaClass superclassTypeArgumentDependency : dependenciesOfTypes(actualTypeArguments)) {
            result.addAll(Dependency.tryCreateFromGenericSuperclassTypeArguments(javaClass, genericSuperclass, superclassTypeArgumentDependency));
        }
        return result.build();
    }

    private Set<Dependency> genericInterfaceTypeArgumentDependencies() {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (JavaParameterizedType genericInterface : getGenericInterfacesOf(javaClass)) {
            List<JavaType> actualTypeArguments = genericInterface.getActualTypeArguments();
            for (JavaClass interfaceTypeArgumentDependency : dependenciesOfTypes(actualTypeArguments)) {
                result.addAll(Dependency.tryCreateFromGenericInterfaceTypeArgument(javaClass, genericInterface, interfaceTypeArgumentDependency));
            }
        }
        return result.build();
    }

    // the cast is safe since we are explicitly filtering instanceOf(..)
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Iterable<JavaParameterizedType> getGenericInterfacesOf(JavaClass javaClass) {
        return (Iterable) FluentIterable.from(javaClass.getInterfaces()).filter(instanceOf(JavaParameterizedType.class));
    }

    private Set<Dependency> fieldDependenciesFromSelf() {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (JavaField field : javaClass.getFields()) {
            result.addAll(Dependency.tryCreateFromField(field));
        }
        return result.build();
    }

    private Set<Dependency> returnTypeDependenciesFromSelf() {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (JavaMethod method : javaClass.getMethods()) {
            result.addAll(Dependency.tryCreateFromReturnType(method));
        }
        return result.build();
    }

    private Set<Dependency> methodParameterDependenciesFromSelf() {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (JavaMethod method : javaClass.getMethods()) {
            for (JavaClass parameter : method.getRawParameterTypes()) {
                result.addAll(Dependency.tryCreateFromParameter(method, parameter));
            }
        }
        return result.build();
    }

    private Set<Dependency> throwsDeclarationDependenciesFromSelf() {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (JavaCodeUnit codeUnit : javaClass.getCodeUnits()) {
            for (ThrowsDeclaration<? extends JavaCodeUnit> throwsDeclaration : codeUnit.getThrowsClause()) {
                result.addAll(Dependency.tryCreateFromThrowsDeclaration(throwsDeclaration));
            }
        }
        return result.build();
    }

    private Set<Dependency> constructorParameterDependenciesFromSelf() {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (JavaConstructor constructor : javaClass.getConstructors()) {
            for (JavaClass parameter : constructor.getRawParameterTypes()) {
                result.addAll(Dependency.tryCreateFromParameter(constructor, parameter));
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

    private Set<Dependency> instanceofCheckDependenciesFromSelf() {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (JavaCodeUnit codeUnit : javaClass.getCodeUnits()) {
            for (InstanceofCheck instanceofCheck : codeUnit.getInstanceofChecks()) {
                result.addAll(Dependency.tryCreateFromInstanceofCheck(instanceofCheck));
            }
        }
        return result.build();
    }

    private Set<Dependency> referencedClassObjectDependenciesFromSelf() {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (ReferencedClassObject referencedClassObject : javaClass.getReferencedClassObjects()) {
            result.addAll(Dependency.tryCreateFromReferencedClassObject(referencedClassObject));
        }
        return result.build();
    }

    private Set<Dependency> typeParameterDependenciesFromSelf() {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (JavaTypeVariable<?> typeVariable : javaClass.getTypeParameters()) {
            result.addAll(getDependenciesFromTypeParameter(typeVariable));
        }
        return result.build();
    }

    private Set<Dependency> getDependenciesFromTypeParameter(JavaTypeVariable<?> typeVariable) {
        ImmutableSet.Builder<Dependency> dependenciesBuilder = ImmutableSet.builder();
        for (JavaClass typeParameterDependency : dependenciesOfTypes(typeVariable.getUpperBounds())) {
            dependenciesBuilder.addAll(Dependency.tryCreateFromTypeParameter(typeVariable, typeParameterDependency));
        }
        return dependenciesBuilder.build();
    }

    private Set<JavaClass> dependenciesOfTypes(Iterable<JavaType> types) {
        ImmutableSet.Builder<JavaClass> result = ImmutableSet.builder();
        for (JavaType type : types) {
            for (JavaClass typeParameterDependency : dependenciesOfType(type)) {
                result.add(typeParameterDependency);
            }
        }
        return result.build();
    }

    private static Iterable<JavaClass> dependenciesOfType(JavaType javaType) {
        ImmutableSet.Builder<JavaClass> result = ImmutableSet.builder();
        if (javaType instanceof JavaClass) {
            result.add((JavaClass) javaType);
        } else if (javaType instanceof JavaParameterizedType) {
            result.addAll(dependenciesOfParameterizedType((JavaParameterizedType) javaType));
        } else if (javaType instanceof JavaWildcardType) {
            result.addAll(dependenciesOfWildcardType((JavaWildcardType) javaType));
        }
        return result.build();
    }

    private static Set<JavaClass> dependenciesOfParameterizedType(JavaParameterizedType parameterizedType) {
        ImmutableSet.Builder<JavaClass> result = ImmutableSet.<JavaClass>builder()
                .add(parameterizedType.toErasure());
        for (JavaType typeArgument : parameterizedType.getActualTypeArguments()) {
            result.addAll(dependenciesOfType(typeArgument));
        }
        return result.build();
    }

    private static Set<JavaClass> dependenciesOfWildcardType(JavaWildcardType javaType) {
        ImmutableSet.Builder<JavaClass> result = ImmutableSet.builder();
        for (JavaType bound : concat(javaType.getUpperBounds(), javaType.getLowerBounds())) {
            result.addAll(dependenciesOfType(bound));
        }
        return result.build();
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
            result.addAll(Dependency.tryCreateFromAnnotation(annotation));
            annotation.accept(new DefaultParameterVisitor() {
                @Override
                public void visitClass(String propertyName, JavaClass javaClass) {
                    result.addAll(Dependency.tryCreateFromAnnotationMember(annotation, javaClass));
                }

                @Override
                public void visitEnumConstant(String propertyName, JavaEnumConstant enumConstant) {
                    result.addAll(Dependency.tryCreateFromAnnotationMember(annotation, enumConstant.getDeclaringClass()));
                }

                @Override
                public void visitAnnotation(String propertyName, JavaAnnotation<?> memberAnnotation) {
                    result.addAll(Dependency.tryCreateFromAnnotationMember(annotation, memberAnnotation.getRawType()));
                    memberAnnotation.accept(this);
                }
            });
        }
        return result.build();
    }
}
