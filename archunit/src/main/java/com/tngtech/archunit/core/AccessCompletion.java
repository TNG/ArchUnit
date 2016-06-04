package com.tngtech.archunit.core;

import java.util.Collection;
import java.util.Map;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

class AccessCompletion {
    final Multimap<JavaField, JavaFieldAccess> accessesToFields = HashMultimap.create();
    final Multimap<JavaMethod, JavaMethodCall> accessesToMethods = HashMultimap.create();
    final Multimap<JavaConstructor, JavaConstructorCall> accessesToConstructors = HashMultimap.create();

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
            for (Map.Entry<JavaField, Collection<JavaFieldAccess>> entry : accessesToFields.asMap().entrySet()) {
                entry.getKey().registerAccesses(entry.getValue());
            }
            for (Map.Entry<JavaMethod, Collection<JavaMethodCall>> entry : accessesToMethods.asMap().entrySet()) {
                entry.getKey().registerCalls(entry.getValue());
            }
            for (Map.Entry<JavaConstructor, Collection<JavaConstructorCall>> entry : accessesToConstructors.asMap().entrySet()) {
                entry.getKey().registerCalls(entry.getValue());
            }
        }
    }
}
