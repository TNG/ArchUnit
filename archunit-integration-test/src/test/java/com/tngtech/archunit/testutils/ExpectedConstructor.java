package com.tngtech.archunit.testutils;

import java.lang.annotation.Annotation;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaConstructor;

import static com.tngtech.archunit.core.domain.Formatters.formatMethod;

public class ExpectedConstructor {
    public static ExpectedConstructor.Creator of(Class<?> owner, Class<?>... params) {
        return new ExpectedConstructor.Creator(owner, params);
    }

    public static class Creator {
        private final Class<?> clazz;
        private final Class<?>[] params;

        private Creator(Class<?> clazz, Class<?>[] params) {
            this.clazz = clazz;
            this.params = params;
        }

        public ExpectedMessage beingAnnotatedWith(Class<? extends Annotation> annotationType) {
            return new ExpectedMessage(String.format("Constructor <%s> is annotated with @%s in (%s.java:0)",
                    formatMethod(clazz.getName(), JavaConstructor.CONSTRUCTOR_NAME, JavaClass.namesOf(params)),
                    annotationType.getSimpleName(),
                    clazz.getSimpleName()));
        }
    }
}
