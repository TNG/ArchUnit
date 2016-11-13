package com.tngtech.archunit.core;

import java.util.Collection;
import java.util.Map;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.tngtech.archunit.core.AccessTarget.ConstructorCallTarget;
import com.tngtech.archunit.core.AccessTarget.FieldAccessTarget;
import com.tngtech.archunit.core.AccessTarget.MethodCallTarget;

class AccessCompletion {
    final Multimap<FieldAccessTarget, JavaFieldAccess> accessesToFields = HashMultimap.create();
    final Multimap<MethodCallTarget, JavaMethodCall> accessesToMethods = HashMultimap.create();
    final Multimap<ConstructorCallTarget, JavaConstructorCall> accessesToConstructors = HashMultimap.create();

    private AccessCompletion() {
    }

    void mergeWith(AccessCompletion subprocess) {
        accessesToFields.putAll(subprocess.accessesToFields);
        accessesToMethods.putAll(subprocess.accessesToMethods);
        accessesToConstructors.putAll(subprocess.accessesToConstructors);
    }

    static class SubProcess extends AccessCompletion {
        SubProcess() {
        }

        SubProcess(JavaCodeUnit<?, ?> codeUnit) {
            for (JavaFieldAccess access : codeUnit.getFieldAccesses()) {
                accessesToFields.put(access.getTarget(), access);
            }
            for (JavaMethodCall call : codeUnit.getMethodCallsFromSelf()) {
                accessesToMethods.put(call.getTarget(), call);
            }
            for (JavaConstructorCall call : codeUnit.getConstructorCallsFromSelf()) {
                accessesToConstructors.put(call.getTarget(), call);
            }
        }
    }

    static class TopProcess extends AccessCompletion {
        void finish() {
            for (Map.Entry<FieldAccessTarget, Collection<JavaFieldAccess>> entry : accessesToFields.asMap().entrySet()) {
                entry.getKey().getJavaField().registerAccesses(entry.getValue());
            }
            for (Map.Entry<MethodCallTarget, Collection<JavaMethodCall>> entry : accessesToMethods.asMap().entrySet()) {
                entry.getKey().getMethod().registerCalls(entry.getValue());
            }
            for (Map.Entry<ConstructorCallTarget, Collection<JavaConstructorCall>> entry : accessesToConstructors.asMap().entrySet()) {
                entry.getKey().getConstructor().registerCalls(entry.getValue());
            }
        }
    }
}
