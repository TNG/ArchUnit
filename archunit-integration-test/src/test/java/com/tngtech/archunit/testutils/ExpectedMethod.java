package com.tngtech.archunit.testutils;

import java.lang.annotation.Annotation;

import static com.tngtech.archunit.core.domain.Formatters.formatMethod;
import static com.tngtech.archunit.core.domain.Formatters.formatNamesOf;
import static java.lang.String.format;

public class ExpectedMethod {
    public static ExpectedMethod.Creator of(Class<?> owner, String methodName, Class<?>... params) {
        return new ExpectedMethod.Creator(owner, methodName, params);
    }

    public static class Creator {
        private final Class<?> clazz;
        private final String methodName;
        private final Class<?>[] params;
        private int lineNumber;

        private Creator(Class<?> clazz, String methodName, Class<?>[] params) {
            this.clazz = clazz;
            this.methodName = methodName;
            this.params = params;
        }

        public Creator inLine(int lineNumber) {
            this.lineNumber = lineNumber;
            return this;
        }

        public ExpectedMessage toNotHaveRawReturnType(Class<?> type) {
            return method("does not have raw return type " + type.getName());
        }

        public ExpectedMessage throwsException(Class<?> type) {
            return method("does declare throwable of type " + type.getName());
        }

        public ExpectedMessage beingAnnotatedWith(Class<? extends Annotation> annotationType) {
            return method("is annotated with @" + annotationType.getSimpleName());
        }

        private ExpectedMessage method(String message) {
            String methodDescription = format("Method <%s>", formatMethod(clazz.getName(), methodName, formatNamesOf(params)));
            String sourceCodeLocation = format("(%s.java:%d)", clazz.getSimpleName(), lineNumber);
            return new ExpectedMessage(format("%s %s in %s", methodDescription, message, sourceCodeLocation));
        }
    }
}
