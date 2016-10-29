package com.tngtech.archunit.core;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.tngtech.archunit.core.AccessRecord.FieldAccessRecord;
import com.tngtech.archunit.core.JavaFieldAccess.AccessType;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.core.ReflectionUtils.classForName;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestUtils {
    public static JavaMethod javaMethod(Class<?> owner, String name, Class<?>... args) {
        return javaMethod(javaClass(owner), name, args);
    }

    public static JavaMethod javaMethod(JavaClass clazz, String name, Class<?>... args) {
        try {
            return javaMethod(clazz, clazz.reflect().getDeclaredMethod(name, args));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static JavaMethod javaMethod(JavaClass clazz, Method method) {
        return new JavaMethod.Builder().withMethod(method).build(clazz);
    }

    public static JavaClass javaClass(Class<?> owner) {
        JavaClass javaClass = new JavaClass.Builder().withType(new TypeDetails(owner)).build();
        ClassFileImportContext context = mock(ClassFileImportContext.class);
        when(context.tryGetJavaClassWithType(anyString())).thenAnswer(new Answer<Optional<JavaClass>>() {
            @Override
            public Optional<JavaClass> answer(InvocationOnMock invocation) throws Throwable {
                return Optional.of(javaClass(classForName((String) invocation.getArguments()[0])));
            }
        });
        javaClass.completeClassHierarchyFrom(context);
        javaClass.completeFrom(context);
        for (JavaCodeUnit<?, ?> unit : javaClass.getCodeUnits()) {
            unit.completeFrom(context);
        }
        return javaClass;
    }

    public static JavaField javaField(Class<?> ownerClass, String name) {
        try {
            return new JavaField.Builder()
                    .withField(ownerClass.getDeclaredField(name))
                    .build(javaClass(ownerClass));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static JavaClasses javaClasses(Class<?>... classes) {
        Map<String, JavaClass> result = new HashMap<>();
        for (Class<?> c : classes) {
            result.put(c.getName(), javaClass(c));
        }
        return new JavaClasses(result);
    }

    public static AccessesSimulator simulateCall() {
        return new AccessesSimulator();
    }

    public static DescribedPredicate<Object> predicateWithDescription(String description) {
        return DescribedPredicate.alwaysTrue().as(description);
    }

    public static class AccessesSimulator {
        private final Set<AccessRecord<JavaMethod>> targets = new HashSet<>();

        public AccessSimulator from(JavaMethod method, int lineNumber) {
            return new AccessSimulator(targets, method, lineNumber);
        }

        public AccessSimulator from(Class<?> clazz, String methodName, Class<?>... params) {
            return new AccessSimulator(targets, javaMethod(clazz, methodName, params), 0);
        }

        public AccessSimulator from(JavaClass clazz, String methodName, Class<?>... params) {
            return from(clazz.getMethod(methodName, params), 0);
        }
    }

    public static class AccessSimulator {
        private final Set<AccessRecord<JavaMethod>> targets;
        private final JavaMethod method;
        private final int lineNumber;

        private AccessSimulator(Set<AccessRecord<JavaMethod>> targets, JavaMethod method, int lineNumber) {
            this.targets = targets;
            this.method = method;
            this.lineNumber = lineNumber;
        }

        public JavaMethodCall to(JavaMethod target) {
            targets.add(new TestAccessRecord<>(target));
            ClassFileImportContext context = mock(ClassFileImportContext.class);
            when(context.getMethodCallRecordsFor(method)).thenReturn(targets);
            method.completeFrom(context);
            return getCallToTarget(target);
        }

        public JavaCall<?> to(Class<?> clazz, String methodName, Class<?>... params) {
            return to(javaMethod(clazz, methodName, params));
        }

        private JavaMethodCall getCallToTarget(JavaMethod target) {
            Set<JavaMethodCall> matchingCalls = new HashSet<>();
            for (JavaMethodCall call : method.getMethodCallsFromSelf()) {
                if (call.getTarget().equals(target)) {
                    matchingCalls.add(call);
                }
            }
            return getOnlyElement(matchingCalls);
        }

        public void to(JavaField target, AccessType accessType) {
            ClassFileImportContext context = mock(ClassFileImportContext.class);
            when(context.getFieldAccessRecordsFor(method))
                    .thenReturn(Collections.<FieldAccessRecord>singleton(new TestFieldAccessRecord(target, accessType)));
            method.completeFrom(context);
        }

        private class TestFieldAccessRecord extends TestAccessRecord<JavaField> implements FieldAccessRecord {
            private final JavaField target;
            private final AccessType accessType;

            private TestFieldAccessRecord(JavaField target, AccessType accessType) {
                super(target);
                this.target = target;
                this.accessType = accessType;
            }

            @Override
            public AccessType getAccessType() {
                return accessType;
            }
        }

        private class TestAccessRecord<T> implements AccessRecord<T> {
            private final T target;

            public TestAccessRecord(T target) {
                this.target = target;
            }

            @Override
            public JavaCodeUnit<?, ?> getCaller() {
                return method;
            }

            @Override
            public T getTarget() {
                return target;
            }

            @Override
            public int getLineNumber() {
                return lineNumber;
            }
        }
    }

    static class ClassWithMethodNamedMethod {
        String method() {
            return null;
        }
    }

    static class AnotherClassWithMethodNamedMethod {
        String method() {
            return null;
        }
    }

    static class ClassWithFieldNamedValue {
        String value;
    }

    static class AnotherClassWithFieldNamedValue {
        String value;
    }
}
