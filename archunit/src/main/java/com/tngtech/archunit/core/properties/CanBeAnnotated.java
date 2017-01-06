package com.tngtech.archunit.core.properties;

import java.lang.annotation.Annotation;

import com.tngtech.archunit.base.DescribedPredicate;

public interface CanBeAnnotated {
    boolean isAnnotatedWith(Class<? extends Annotation> annotation);

    class Predicates {
        public static DescribedPredicate<CanBeAnnotated> annotatedWith(final Class<? extends Annotation> annotationType) {
            return new DescribedPredicate<CanBeAnnotated>("annotated with @" + annotationType.getSimpleName()) {
                @Override
                public boolean apply(CanBeAnnotated input) {
                    return input.isAnnotatedWith(annotationType);
                }
            };
        }
    }
}
