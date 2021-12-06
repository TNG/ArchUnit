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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.tngtech.archunit.base.Optional;

final class ReverseDependencies {

    private final LoadingCache<JavaField, Set<JavaFieldAccess>> accessToFieldCache;
    private final LoadingCache<JavaMethod, Set<JavaMethodCall>> callToMethodCache;
    private final LoadingCache<JavaConstructor, Set<JavaConstructorCall>> callToConstructorCache;
    private final SetMultimap<JavaClass, JavaField> fieldTypeDependencies;
    private final SetMultimap<JavaClass, JavaMethod> methodParameterTypeDependencies;
    private final SetMultimap<JavaClass, JavaMethod> methodReturnTypeDependencies;
    private final SetMultimap<JavaClass, ThrowsDeclaration<JavaMethod>> methodsThrowsDeclarationDependencies;
    private final SetMultimap<JavaClass, JavaConstructor> constructorParameterTypeDependencies;
    private final SetMultimap<JavaClass, ThrowsDeclaration<JavaConstructor>> constructorThrowsDeclarationDependencies;
    private final SetMultimap<JavaClass, JavaAnnotation<?>> annotationTypeDependencies;
    private final SetMultimap<JavaClass, JavaAnnotation<?>> annotationParameterTypeDependencies;
    private final SetMultimap<JavaClass, InstanceofCheck> instanceofCheckDependencies;
    private final Supplier<SetMultimap<JavaClass, Dependency>> directDependenciesToClass;

    private ReverseDependencies(ReverseDependencies.Creation creation) {
        accessToFieldCache = CacheBuilder.newBuilder().build(new ResolvingAccessLoader<>(creation.fieldAccessDependencies.build()));
        callToMethodCache = CacheBuilder.newBuilder().build(new ResolvingAccessLoader<>(creation.methodCallDependencies.build()));
        callToConstructorCache = CacheBuilder.newBuilder().build(new ConstructorCallLoader(creation.constructorCallDependencies.build()));
        this.fieldTypeDependencies = creation.fieldTypeDependencies.build();
        this.methodParameterTypeDependencies = creation.methodParameterTypeDependencies.build();
        this.methodReturnTypeDependencies = creation.methodReturnTypeDependencies.build();
        this.methodsThrowsDeclarationDependencies = creation.methodsThrowsDeclarationDependencies.build();
        this.constructorParameterTypeDependencies = creation.constructorParameterTypeDependencies.build();
        this.constructorThrowsDeclarationDependencies = creation.constructorThrowsDeclarationDependencies.build();
        this.annotationTypeDependencies = creation.annotationTypeDependencies.build();
        this.annotationParameterTypeDependencies = creation.annotationParameterTypeDependencies.build();
        this.instanceofCheckDependencies = creation.instanceofCheckDependencies.build();
        this.directDependenciesToClass = createDirectDependenciesToClassSupplier(creation.allDependencies);
    }

    private static Supplier<SetMultimap<JavaClass, Dependency>> createDirectDependenciesToClassSupplier(final List<JavaClassDependencies> allDependencies) {
        return Suppliers.memoize(new Supplier<SetMultimap<JavaClass, Dependency>>() {
            @Override
            public SetMultimap<JavaClass, Dependency> get() {
                ImmutableSetMultimap.Builder<JavaClass, Dependency> result = ImmutableSetMultimap.builder();
                for (JavaClassDependencies dependencies : allDependencies) {
                    for (Dependency dependency : dependencies.getDirectDependenciesFromClass()) {
                        result.put(dependency.getTargetClass(), dependency);
                    }
                }
                return result.build();
            }
        });
    }

    Set<JavaFieldAccess> getAccessesTo(JavaField field) {
        return accessToFieldCache.getUnchecked(field);
    }

    Set<JavaMethodCall> getCallsTo(JavaMethod method) {
        return callToMethodCache.getUnchecked(method);
    }

    Set<JavaConstructorCall> getCallsTo(JavaConstructor constructor) {
        return callToConstructorCache.getUnchecked(constructor);
    }

    Set<JavaField> getFieldsWithTypeOf(JavaClass clazz) {
        return fieldTypeDependencies.get(clazz);
    }

    Set<JavaMethod> getMethodsWithParameterTypeOf(JavaClass clazz) {
        return methodParameterTypeDependencies.get(clazz);
    }

    Set<JavaMethod> getMethodsWithReturnTypeOf(JavaClass clazz) {
        return methodReturnTypeDependencies.get(clazz);
    }

    Set<ThrowsDeclaration<JavaMethod>> getMethodThrowsDeclarationsWithTypeOf(JavaClass clazz) {
        return methodsThrowsDeclarationDependencies.get(clazz);
    }

    Set<JavaConstructor> getConstructorsWithParameterTypeOf(JavaClass clazz) {
        return constructorParameterTypeDependencies.get(clazz);
    }

    Set<ThrowsDeclaration<JavaConstructor>> getConstructorsWithThrowsDeclarationTypeOf(JavaClass clazz) {
        return constructorThrowsDeclarationDependencies.get(clazz);
    }

    Set<JavaAnnotation<?>> getAnnotationsWithTypeOf(JavaClass clazz) {
        return annotationTypeDependencies.get(clazz);
    }

    Set<JavaAnnotation<?>> getAnnotationsWithParameterTypeOf(JavaClass clazz) {
        return annotationParameterTypeDependencies.get(clazz);
    }

    Set<InstanceofCheck> getInstanceofChecksWithTypeOf(JavaClass clazz) {
        return instanceofCheckDependencies.get(clazz);
    }

    Set<Dependency> getDirectDependenciesTo(JavaClass clazz) {
        return directDependenciesToClass.get().get(clazz);
    }

    static final ReverseDependencies EMPTY = new ReverseDependencies(new Creation());

    static class Creation {
        private final ImmutableSetMultimap.Builder<JavaClass, JavaFieldAccess> fieldAccessDependencies = ImmutableSetMultimap.builder();
        private final ImmutableSetMultimap.Builder<JavaClass, JavaMethodCall> methodCallDependencies = ImmutableSetMultimap.builder();
        private final ImmutableSetMultimap.Builder<String, JavaConstructorCall> constructorCallDependencies = ImmutableSetMultimap.builder();
        private final ImmutableSetMultimap.Builder<JavaClass, JavaField> fieldTypeDependencies = ImmutableSetMultimap.builder();
        private final ImmutableSetMultimap.Builder<JavaClass, JavaMethod> methodParameterTypeDependencies = ImmutableSetMultimap.builder();
        private final ImmutableSetMultimap.Builder<JavaClass, JavaMethod> methodReturnTypeDependencies = ImmutableSetMultimap.builder();
        private final ImmutableSetMultimap.Builder<JavaClass, ThrowsDeclaration<JavaMethod>> methodsThrowsDeclarationDependencies = ImmutableSetMultimap.builder();
        private final ImmutableSetMultimap.Builder<JavaClass, JavaConstructor> constructorParameterTypeDependencies = ImmutableSetMultimap.builder();
        private final ImmutableSetMultimap.Builder<JavaClass, ThrowsDeclaration<JavaConstructor>> constructorThrowsDeclarationDependencies = ImmutableSetMultimap.builder();
        private final ImmutableSetMultimap.Builder<JavaClass, JavaAnnotation<?>> annotationTypeDependencies = ImmutableSetMultimap.builder();
        private final ImmutableSetMultimap.Builder<JavaClass, JavaAnnotation<?>> annotationParameterTypeDependencies = ImmutableSetMultimap.builder();
        private final ImmutableSetMultimap.Builder<JavaClass, InstanceofCheck> instanceofCheckDependencies = ImmutableSetMultimap.builder();
        private final List<JavaClassDependencies> allDependencies = new ArrayList<>();

        public void registerDependenciesOf(JavaClass clazz, JavaClassDependencies classDependencies) {
            registerAccesses(clazz);
            registerFields(clazz);
            registerMethods(clazz);
            registerConstructors(clazz);
            registerAnnotations(clazz);
            registerStaticInitializer(clazz);
            allDependencies.add(classDependencies);
        }

        private void registerAccesses(JavaClass clazz) {
            for (JavaFieldAccess access : clazz.getFieldAccessesFromSelf()) {
                fieldAccessDependencies.put(access.getTargetOwner(), access);
            }
            for (JavaMethodCall call : clazz.getMethodCallsFromSelf()) {
                methodCallDependencies.put(call.getTargetOwner(), call);
            }
            for (JavaConstructorCall call : clazz.getConstructorCallsFromSelf()) {
                constructorCallDependencies.put(call.getTarget().getFullName(), call);
            }
        }

        private void registerFields(JavaClass clazz) {
            for (JavaField field : clazz.getFields()) {
                fieldTypeDependencies.put(field.getRawType(), field);
            }
        }

        private void registerMethods(JavaClass clazz) {
            for (JavaMethod method : clazz.getMethods()) {
                for (JavaClass parameter : method.getRawParameterTypes()) {
                    methodParameterTypeDependencies.put(parameter, method);
                }
                methodReturnTypeDependencies.put(method.getRawReturnType(), method);
                for (ThrowsDeclaration<JavaMethod> throwsDeclaration : method.getThrowsClause()) {
                    methodsThrowsDeclarationDependencies.put(throwsDeclaration.getRawType(), throwsDeclaration);
                }
                for (InstanceofCheck instanceofCheck : method.getInstanceofChecks()) {
                    instanceofCheckDependencies.put(instanceofCheck.getRawType(), instanceofCheck);
                }
            }
        }

        private void registerConstructors(JavaClass clazz) {
            for (JavaConstructor constructor : clazz.getConstructors()) {
                for (JavaClass parameter : constructor.getRawParameterTypes()) {
                    constructorParameterTypeDependencies.put(parameter, constructor);
                }
                for (ThrowsDeclaration<JavaConstructor> throwsDeclaration : constructor.getThrowsClause()) {
                    constructorThrowsDeclarationDependencies.put(throwsDeclaration.getRawType(), throwsDeclaration);
                }
                for (InstanceofCheck instanceofCheck : constructor.getInstanceofChecks()) {
                    instanceofCheckDependencies.put(instanceofCheck.getRawType(), instanceofCheck);
                }
            }
        }

        private void registerAnnotations(JavaClass clazz) {
            for (final JavaAnnotation<?> annotation : findAnnotations(clazz)) {
                annotationTypeDependencies.put(annotation.getRawType(), annotation);
                annotation.accept(new JavaAnnotation.DefaultParameterVisitor() {
                    @Override
                    public void visitClass(String propertyName, JavaClass javaClass) {
                        annotationParameterTypeDependencies.put(javaClass, annotation);
                    }

                    @Override
                    public void visitEnumConstant(String propertyName, JavaEnumConstant enumConstant) {
                        annotationParameterTypeDependencies.put(enumConstant.getDeclaringClass(), annotation);
                    }

                    @Override
                    public void visitAnnotation(String propertyName, JavaAnnotation<?> memberAnnotation) {
                        annotationParameterTypeDependencies.put(memberAnnotation.getRawType(), annotation);
                        memberAnnotation.accept(this);
                    }
                });
            }
        }

        private Set<JavaAnnotation<?>> findAnnotations(JavaClass clazz) {
            Set<JavaAnnotation<?>> result = Sets.<JavaAnnotation<?>>newHashSet(clazz.getAnnotations());
            for (JavaMember member : clazz.getMembers()) {
                result.addAll(member.getAnnotations());
            }
            return result;
        }

        private void registerStaticInitializer(JavaClass clazz) {
            if (clazz.getStaticInitializer().isPresent()) {
                for (InstanceofCheck instanceofCheck : clazz.getStaticInitializer().get().getInstanceofChecks()) {
                    instanceofCheckDependencies.put(instanceofCheck.getRawType(), instanceofCheck);
                }
            }
        }

        void finish(Iterable<JavaClass> classes) {
            ReverseDependencies reverseDependencies = new ReverseDependencies(this);
            for (JavaClass clazz : classes) {
                clazz.setReverseDependencies(reverseDependencies);
            }
        }
    }

    private static class ResolvingAccessLoader<MEMBER extends JavaMember, ACCESS extends JavaAccess<?>> extends CacheLoader<MEMBER, Set<ACCESS>> {
        private final SetMultimap<JavaClass, ACCESS> accessesToSelf;

        private ResolvingAccessLoader(SetMultimap<JavaClass, ACCESS> accessesToSelf) {
            this.accessesToSelf = accessesToSelf;
        }

        @Override
        public Set<ACCESS> load(MEMBER member) {
            ImmutableSet.Builder<ACCESS> result = ImmutableSet.builder();
            for (final JavaClass javaClass : getPossibleTargetClassesForAccess(member.getOwner())) {
                for (ACCESS access : this.accessesToSelf.get(javaClass)) {
                    Optional<? extends JavaMember> target = access.getTarget().resolveMember();
                    if (target.isPresent() && target.get().equals(member)) {
                        result.add(access);
                    }
                }
            }
            return result.build();
        }

        private Set<JavaClass> getPossibleTargetClassesForAccess(JavaClass owner) {
            return ImmutableSet.<JavaClass>builder()
                    .add(owner)
                    .addAll(owner.getAllSubclasses())
                    .build();
        }
    }

    private static class ConstructorCallLoader extends CacheLoader<JavaConstructor, Set<JavaConstructorCall>> {
        private final SetMultimap<String, JavaConstructorCall> accessesToSelf;

        private ConstructorCallLoader(SetMultimap<String, JavaConstructorCall> accessesToSelf) {
            this.accessesToSelf = accessesToSelf;
        }

        @Override
        public Set<JavaConstructorCall> load(JavaConstructor member) {
            ImmutableSet.Builder<JavaConstructorCall> result = ImmutableSet.builder();
            result.addAll(accessesToSelf.get(member.getFullName()));
            return result.build();
        }
    }
}
