/*
 * Copyright 2014-2020 TNG Technology Consulting GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tngtech.archunit.core.domain.properties;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collection;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ArchUnitException.InvalidSyntaxUsageException;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.core.domain.JavaAnnotation;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.base.DescribedPredicate.equalTo;
import static com.tngtech.archunit.core.domain.Formatters.ensureSimpleName;
import static com.tngtech.archunit.core.domain.properties.HasName.Functions.GET_NAME;
import static com.tngtech.archunit.core.domain.properties.HasType.Functions.GET_RAW_TYPE;

public interface CanBeAnnotated {
    @PublicAPI(usage = ACCESS)
    boolean isAnnotatedWith(Class<? extends Annotation> annotationType);

    @PublicAPI(usage = ACCESS)
    boolean isAnnotatedWith(String annotationTypeName);

    @PublicAPI(usage = ACCESS)
    boolean isAnnotatedWith(DescribedPredicate<? super JavaAnnotation<?>> predicate);

    @PublicAPI(usage = ACCESS)
    boolean isMetaAnnotatedWith(Class<? extends Annotation> annotationType);

    @PublicAPI(usage = ACCESS)
    boolean isMetaAnnotatedWith(String annotationTypeName);

    @PublicAPI(usage = ACCESS)
    boolean isMetaAnnotatedWith(DescribedPredicate<? super JavaAnnotation<?>> predicate);

    final class Predicates {
        private Predicates() {
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<CanBeAnnotated> annotatedWith(final Class<? extends Annotation> annotationType) {
            checkAnnotationHasReasonableRetention(annotationType);

            return annotatedWith(annotationType.getName());
        }

        private static void checkAnnotationHasReasonableRetention(Class<? extends Annotation> annotationType) {
            if (isRetentionSource(annotationType)) {
                throw new InvalidSyntaxUsageException(String.format(
                        "Annotation type %s has @%s(%s), thus the information is gone after compile. "
                                + "So checking this with ArchUnit is useless.",
                        annotationType.getName(), Retention.class.getSimpleName(), RetentionPolicy.SOURCE));
            }
        }

        private static boolean isRetentionSource(Class<? extends Annotation> annotationType) {
            return annotationType.getAnnotation(Retention.class) != null
                    && (annotationType.getAnnotation(Retention.class).value() == RetentionPolicy.SOURCE);
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<CanBeAnnotated> annotatedWith(final String annotationTypeName) {
            DescribedPredicate<HasType> typeNameMatches = GET_RAW_TYPE.then(GET_NAME).is(equalTo(annotationTypeName));
            return annotatedWith(typeNameMatches.as("@" + ensureSimpleName(annotationTypeName)));
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<CanBeAnnotated> annotatedWith(final DescribedPredicate<? super JavaAnnotation<?>> predicate) {
            return new AnnotatedPredicate(predicate);
        }

        private static class AnnotatedPredicate extends DescribedPredicate<CanBeAnnotated> {
            private final DescribedPredicate<? super JavaAnnotation<?>> predicate;

            AnnotatedPredicate(DescribedPredicate<? super JavaAnnotation<?>> predicate) {
                super("annotated with " + predicate.getDescription());
                this.predicate = predicate;
            }

            @Override
            public boolean apply(CanBeAnnotated input) {
                return input.isAnnotatedWith(predicate);
            }
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<CanBeAnnotated> metaAnnotatedWith(final Class<? extends Annotation> annotationType) {
            checkAnnotationHasReasonableRetention(annotationType);

            return metaAnnotatedWith(annotationType.getName());
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<CanBeAnnotated> metaAnnotatedWith(final String annotationTypeName) {
            DescribedPredicate<HasType> typeNameMatches = GET_RAW_TYPE.then(GET_NAME).is(equalTo(annotationTypeName));
            return metaAnnotatedWith(typeNameMatches.as("@" + ensureSimpleName(annotationTypeName)));
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<CanBeAnnotated> metaAnnotatedWith(final DescribedPredicate<? super JavaAnnotation<?>> predicate) {
            return new MetaAnnotatedPredicate(predicate);
        }

        private static class MetaAnnotatedPredicate extends DescribedPredicate<CanBeAnnotated> {
            private final DescribedPredicate<? super JavaAnnotation<?>> predicate;

            MetaAnnotatedPredicate(DescribedPredicate<? super JavaAnnotation<?>> predicate) {
                super("meta-annotated with " + predicate.getDescription());
                this.predicate = predicate;
            }

            @Override
            public boolean apply(CanBeAnnotated input) {
                return input.isMetaAnnotatedWith(predicate);
            }
        }
    }

    final class Utils {
        private Utils() {
        }

        @PublicAPI(usage = ACCESS)
        public static boolean isAnnotatedWith(
                Collection<? extends JavaAnnotation<?>> annotations,
                DescribedPredicate<? super JavaAnnotation<?>> predicate) {

            for (JavaAnnotation<?> annotation : annotations) {
                if (predicate.apply(annotation)) {
                    return true;
                }
            }
            return false;
        }

        @PublicAPI(usage = ACCESS)
        public static boolean isMetaAnnotatedWith(
                Collection<? extends JavaAnnotation<?>> annotations,
                DescribedPredicate<? super JavaAnnotation<?>> predicate) {

            for (JavaAnnotation<?> annotation : annotations) {
                if (annotation.getRawType().isAnnotatedWith(predicate) || annotation.getRawType().isMetaAnnotatedWith(predicate)) {
                    return true;
                }
            }
            return false;
        }

        @PublicAPI(usage = ACCESS)
        public static <A extends Annotation> Function<JavaAnnotation<?>, A> toAnnotationOfType(final Class<A> type) {
            return new Function<JavaAnnotation<?>, A>() {
                @Override
                public A apply(JavaAnnotation<?> input) {
                    return input.as(type);
                }
            };
        }
    }
}
