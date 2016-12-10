package com.tngtech.archunit.core;

import java.util.Collection;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

class AccessContext {
    final SetMultimap<String, JavaFieldAccess> fieldAccessesByTarget = HashMultimap.create();
    final SetMultimap<String, JavaMethodCall> methodCallsByTarget = HashMultimap.create();
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
                fieldAccessesByTarget.put(access.getTarget().getFullName(), access);
            }
            for (JavaMethodCall call : codeUnit.getMethodCallsFromSelf()) {
                methodCallsByTarget.put(call.getTarget().getFullName(), call);
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
                    field.registerAccessesToField(fieldAccessesByTarget.get(field.getFullName()));
                }
                for (JavaMethod method : clazz.getMethods()) {
                    method.registerCallsToMethod(methodCallsByTarget.get(method.getFullName()));
                }
                for (JavaConstructor constructor : clazz.getConstructors()) {
                    constructor.registerCallsToConstructor(constructorCallsByTarget.get(constructor.getFullName()));
                }
            }
        }
    }
}
