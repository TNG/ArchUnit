package com.tngtech.archunit.testutils;

import java.lang.annotation.Annotation;

import com.tngtech.archunit.core.domain.JavaClass;

import static com.tngtech.archunit.core.domain.Formatters.formatMethod;

public class ExpectedMethod {
    public static ExpectedMethod.Creator of(Class<?> owner, String methodName, Class<?>... params) {
        return new ExpectedMethod.Creator(owner, methodName, params);
    }

    public static class Creator {
        private final Class<?> clazz;
        private final String methodName;
        private final Class<?>[] params;

        private Creator(Class<?> clazz, String methodName, Class<?>[] params) {
            this.clazz = clazz;
            this.methodName = methodName;
            this.params = params;
        }

        public ExpectedMessage toNotHaveRawReturnType(Class<?> type) {
            return new ExpectedMessage(String.format("Method <%s> does not have raw return type %s in (%s.java:0)",
                    formatMethod(clazz.getName(), methodName, JavaClass.namesOf(params)),
                    type.getName(),
                    clazz.getSimpleName()));
        }

        public ExpectedMessage throwsException(Class<?> type) {
            return new ExpectedMessage(String.format("Method <%s> does declare throwable of type %s in (%s.java:0)",
                    formatMethod(clazz.getName(), methodName, JavaClass.namesOf(params)),
                    type.getName(),
                    clazz.getSimpleName()));
        }

        public ExpectedMessage beingAnnotatedWith(Class<? extends Annotation> annotationType) {
            return new ExpectedMessage(String.format("Method <%s> is annotated with @%s in (%s.java:0)",
                    formatMethod(clazz.getName(), methodName, JavaClass.namesOf(params)),
                    annotationType.getSimpleName(),
                    clazz.getSimpleName()));
        }
    }
}
