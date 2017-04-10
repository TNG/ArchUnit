package com.tngtech.archunit.core.domain.properties;

import java.lang.annotation.Annotation;
import java.util.Collection;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.core.domain.JavaAnnotation;

import static com.tngtech.archunit.core.domain.Formatters.ensureSimpleName;

public interface CanBeAnnotated {
    boolean isAnnotatedWith(Class<? extends Annotation> annotationType);

    boolean isAnnotatedWith(String annotationTypeName);

    boolean isAnnotatedWith(DescribedPredicate<? super JavaAnnotation> predicate);

    class Predicates {
        public static DescribedPredicate<CanBeAnnotated> annotatedWith(final Class<? extends Annotation> annotationType) {
            return annotatedWith(annotationType.getName());
        }

        public static DescribedPredicate<CanBeAnnotated> annotatedWith(final String annotationTypeName) {
            return new DescribedPredicate<CanBeAnnotated>("annotated with @" + ensureSimpleName(annotationTypeName)) {
                @Override
                public boolean apply(CanBeAnnotated input) {
                    return input.isAnnotatedWith(annotationTypeName);
                }
            };
        }

        public static DescribedPredicate<CanBeAnnotated> annotatedWith(final DescribedPredicate<? super JavaAnnotation> predicate) {
            return new DescribedPredicate<CanBeAnnotated>("annotated with " + predicate.getDescription()) {
                @Override
                public boolean apply(CanBeAnnotated input) {
                    return input.isAnnotatedWith(predicate);
                }
            };
        }
    }

    class Utils {
        public static boolean isAnnotatedWith(Collection<JavaAnnotation> annotations,
                                              DescribedPredicate<? super JavaAnnotation> predicate) {
            for (JavaAnnotation annotation : annotations) {
                if (predicate.apply(annotation)) {
                    return true;
                }
            }
            return false;
        }

        public static <A extends Annotation> Function<JavaAnnotation, A> toAnnotationOfType(final Class<A> type) {
            return new Function<JavaAnnotation, A>() {
                @Override
                public A apply(JavaAnnotation input) {
                    return input.as(type);
                }
            };
        }
    }
}
