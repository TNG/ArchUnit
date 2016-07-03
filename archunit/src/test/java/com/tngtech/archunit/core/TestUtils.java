package com.tngtech.archunit.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestUtils {
    public static JavaMethod javaMethod(String name, Class<?> owner) {
        return javaMethod(name, javaClass(owner));
    }

    public static JavaMethod javaMethod(String name, JavaClass clazz) {
        try {
            return new JavaMethod.Builder().withMethod(clazz.reflect().getDeclaredMethod(name)).build(clazz);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static JavaClass javaClass(Class<?> owner) {
        JavaClass javaClass = new JavaClass.Builder().withType(owner).build();
        ClassFileImportContext context = mock(ClassFileImportContext.class);
        when(context.tryGetJavaClassWithType(any(Class.class))).thenAnswer(new Answer<Optional<JavaClass>>() {
            @Override
            public Optional<JavaClass> answer(InvocationOnMock invocation) throws Throwable {
                return Optional.of(javaClass((Class<?>) invocation.getArguments()[0]));
            }
        });
        javaClass.completeClassHierarchyFrom(context);
        for (JavaCodeUnit<?, ?> unit : javaClass.getCodeUnits()) {
            unit.completeFrom(context);
        }
        return javaClass;
    }

    public static JavaField javaField(String name, Class<?> ownerClass) throws NoSuchFieldException {
        return new JavaField.Builder()
                .withField(ownerClass.getDeclaredField(name))
                .build(javaClass(ownerClass));
    }

    public static JavaClasses javaClasses(Class<?>... classes) {
        Map<Class<?>, JavaClass> result = new HashMap<>();
        for (Class<?> c : classes) {
            result.put(c, javaClass(c));
        }
        return new JavaClasses(result);
    }

    public static AccessSimulator simulateCallFrom(JavaMethod method, int lineNumber) {
        return new AccessSimulator(method, lineNumber);
    }

    static class AccessSimulator {
        private final JavaMethod method;
        private final int lineNumber;

        public AccessSimulator(JavaMethod method, int lineNumber) {
            this.method = method;
            this.lineNumber = lineNumber;
        }

        public void to(JavaMethod target) {
            ClassFileImportContext context = mock(ClassFileImportContext.class);
            when(context.getMethodCallRecordsFor(method))
                    .thenReturn(Collections.<AccessRecord<JavaMethod>>singleton(new TestAccessRecord(target)));
            method.completeFrom(context);
        }

        private class TestAccessRecord implements AccessRecord<JavaMethod> {
            private final JavaMethod target;

            public TestAccessRecord(JavaMethod target) {
                this.target = target;
            }

            @Override
            public JavaCodeUnit<?, ?> getCaller() {
                return method;
            }

            @Override
            public JavaMethod getTarget() {
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
