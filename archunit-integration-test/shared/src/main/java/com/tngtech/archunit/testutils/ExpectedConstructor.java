package com.tngtech.archunit.testutils;

import java.lang.annotation.Annotation;

import static com.tngtech.archunit.core.domain.Formatters.formatMethod;
import static com.tngtech.archunit.core.domain.Formatters.formatNamesOf;
import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;

public class ExpectedConstructor {
    public static ExpectedConstructor.Creator of(Class<?> owner, Class<?>... params) {
        return new ExpectedConstructor.Creator(owner, params);
    }

    public static class Creator {
        private final Class<?> clazz;
        private final Class<?>[] params;
        private int lineNumber;

        private Creator(Class<?> clazz, Class<?>[] params) {
            this.clazz = clazz;
            this.params = params;
        }

        public  Creator inLine(int lineNumber) {
            this.lineNumber = lineNumber;
            return this;
        }

        public ExpectedMessage beingAnnotatedWith(Class<? extends Annotation> annotationType) {
            return new ExpectedMessage(String.format("Constructor <%s> is annotated with @%s in (%s.java:%d)",
                    formatMethod(clazz.getName(), CONSTRUCTOR_NAME, formatNamesOf(params)),
                    annotationType.getSimpleName(),
                    clazz.getSimpleName(), lineNumber));
        }
    }
}
