package com.tngtech.archunit.core;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;

class AccessContext {
    final SetMultimap<JavaClass, JavaFieldAccess> fieldAccessesByTarget = HashMultimap.create();
    final SetMultimap<JavaClass, JavaMethodCall> methodCallsByTarget = HashMultimap.create();
    final SetMultimap<String, JavaConstructorCall> constructorCallsByTarget = HashMultimap.create();

    private AccessContext() {
    }

    void mergeWith(AccessContext other) {
        fieldAccessesByTarget.putAll(other.fieldAccessesByTarget);
        methodCallsByTarget.putAll(other.methodCallsByTarget);
        constructorCallsByTarget.putAll(other.constructorCallsByTarget);
    }

    static class Part extends AccessContext {
        Part() {
        }

        Part(JavaCodeUnit<?, ?> codeUnit) {
            for (JavaFieldAccess access : codeUnit.getFieldAccesses()) {
                fieldAccessesByTarget.put(access.getTarget().getOwner(), access);
            }
            for (JavaMethodCall call : codeUnit.getMethodCallsFromSelf()) {
                methodCallsByTarget.put(call.getTarget().getOwner(), call);
            }
            for (JavaConstructorCall call : codeUnit.getConstructorCallsFromSelf()) {
                constructorCallsByTarget.put(call.getTarget().getFullName(), call);
            }
        }
    }

    static class TopProcess extends AccessContext {
        private final Collection<JavaClass> classes;

        TopProcess(Collection<JavaClass> classes) {
            this.classes = classes;
        }

        void finish() {
            for (JavaClass clazz : classes) {
                for (JavaField field : clazz.getFields()) {
                    field.registerAccessesToField(getFieldAccessesTo(field));
                }
                for (JavaMethod method : clazz.getMethods()) {
                    method.registerCallsToMethod(getMethodCallsOf(method));
                }
                for (final JavaConstructor constructor : clazz.getConstructors()) {
                    constructor.registerCallsToConstructor(constructorCallsByTarget.get(constructor.getFullName()));
                }
            }
        }

        private Supplier<Set<JavaFieldAccess>> getFieldAccessesTo(final JavaField field) {
            return newAccessSupplier(field.getOwner(), fieldAccessTargetResolvesTo(field));
        }

        private Function<JavaClass, Set<JavaFieldAccess>> fieldAccessTargetResolvesTo(final JavaField field) {
            return new Function<JavaClass, Set<JavaFieldAccess>>() {
                @Override
                public Set<JavaFieldAccess> apply(JavaClass input) {
                    Set<JavaFieldAccess> result = new HashSet<>();
                    for (JavaFieldAccess access : fieldAccessesByTarget.get(input)) {
                        if (access.getTarget().resolve().asSet().contains(field)) {
                            result.add(access);
                        }
                    }
                    return result;
                }
            };
        }

        private Supplier<Set<JavaMethodCall>> getMethodCallsOf(final JavaMethod method) {
            return newAccessSupplier(method.getOwner(), methodCallTargetResolvesTo(method));
        }

        private Function<JavaClass, Set<JavaMethodCall>> methodCallTargetResolvesTo(final JavaMethod method) {
            return new Function<JavaClass, Set<JavaMethodCall>>() {
                @Override
                public Set<JavaMethodCall> apply(JavaClass input) {
                    Set<JavaMethodCall> result = new HashSet<>();
                    for (JavaMethodCall call : methodCallsByTarget.get(input)) {
                        if (call.getTarget().resolve().contains(method)) {
                            result.add(call);
                        }
                    }
                    return result;
                }
            };
        }

        private <T> Supplier<Set<T>> newAccessSupplier(final JavaClass owner, final Function<JavaClass, Set<T>> doWithEachClass) {

            return Suppliers.memoize(new Supplier<Set<T>>() {
                @Override
                public Set<T> get() {
                    ImmutableSet.Builder<T> result = ImmutableSet.builder();
                    for (final JavaClass javaClass : getPossibleTargetClassesForAccess()) {
                        result.addAll(doWithEachClass.apply(javaClass));
                    }
                    return result.build();
                }

                private Set<JavaClass> getPossibleTargetClassesForAccess() {
                    return ImmutableSet.<JavaClass>builder()
                            .add(owner)
                            .addAll(owner.getAllSubClasses())
                            .build();
                }
            });
        }
    }
}
