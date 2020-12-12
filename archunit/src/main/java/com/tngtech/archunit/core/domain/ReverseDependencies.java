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

import java.util.Set;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;

final class ReverseDependencies {

    private final LoadingCache<JavaField, Set<JavaFieldAccess>> accessToFieldCache;
    private final LoadingCache<JavaMethod, Set<JavaMethodCall>> callToMethodCache;
    private final LoadingCache<JavaConstructor, Set<JavaConstructorCall>> callToConstructorCache;

    private ReverseDependencies(
            ImmutableSetMultimap<JavaClass, JavaFieldAccess> fieldAccessDependencies,
            ImmutableSetMultimap<JavaClass, JavaMethodCall> methodCallDependencies,
            ImmutableSetMultimap<String, JavaConstructorCall> constructorCallDependencies) {

        accessToFieldCache = CacheBuilder.newBuilder().build(new ResolvingAccessLoader<>(fieldAccessDependencies));
        callToMethodCache = CacheBuilder.newBuilder().build(new ResolvingAccessLoader<>(methodCallDependencies));
        callToConstructorCache = CacheBuilder.newBuilder().build(new ConstructorCallLoader(constructorCallDependencies));
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

    static final ReverseDependencies EMPTY = new ReverseDependencies(
            ImmutableSetMultimap.<JavaClass, JavaFieldAccess>of(),
            ImmutableSetMultimap.<JavaClass, JavaMethodCall>of(),
            ImmutableSetMultimap.<String, JavaConstructorCall>of());

    static class Creation {
        private final ImmutableSetMultimap.Builder<JavaClass, JavaFieldAccess> fieldAccessDependencies = ImmutableSetMultimap.builder();
        private final ImmutableSetMultimap.Builder<JavaClass, JavaMethodCall> methodCallDependencies = ImmutableSetMultimap.builder();
        private final ImmutableSetMultimap.Builder<String, JavaConstructorCall> constructorCallDependencies = ImmutableSetMultimap.builder();

        public void registerDependenciesOf(JavaClass clazz) {
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

        void finish(Iterable<JavaClass> classes) {
            ReverseDependencies reverseDependencies = new ReverseDependencies(
                    fieldAccessDependencies.build(),
                    methodCallDependencies.build(),
                    constructorCallDependencies.build());
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
                    if (access.getTarget().resolve().contains(member)) {
                        result.add(access);
                    }
                }
            }
            return result.build();
        }

        private Set<JavaClass> getPossibleTargetClassesForAccess(JavaClass owner) {
            return ImmutableSet.<JavaClass>builder()
                    .add(owner)
                    .addAll(owner.getAllSubClasses())
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
