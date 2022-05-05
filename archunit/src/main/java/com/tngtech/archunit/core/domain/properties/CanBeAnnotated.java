/*
 * Copyright 2014-2022 TNG Technology Consulting GmbH
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
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ArchUnitException.InvalidSyntaxUsageException;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAnnotation;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.base.DescribedPredicate.equalTo;
import static com.tngtech.archunit.core.domain.Formatters.ensureSimpleName;
import static com.tngtech.archunit.core.domain.properties.HasName.Functions.GET_NAME;
import static com.tngtech.archunit.core.domain.properties.HasType.Functions.GET_RAW_TYPE;

public interface CanBeAnnotated {

    /**
     * Returns {@code true}, if this element is annotated with the given annotation type.
     *
     * @param annotationType The type of the annotation to check for
     */
    @PublicAPI(usage = ACCESS)
    boolean isAnnotatedWith(Class<? extends Annotation> annotationType);

    /**
     * @param annotationTypeName Fully qualified class name of a specific type of {@link Annotation}
     * @see #isAnnotatedWith(Class)
     */
    @PublicAPI(usage = ACCESS)
    boolean isAnnotatedWith(String annotationTypeName);

    /**
     * Returns {@code true}, if this element is annotated with an annotation matching the given predicate.
     *
     * @param predicate Qualifies matching annotations
     */
    @PublicAPI(usage = ACCESS)
    boolean isAnnotatedWith(DescribedPredicate<? super JavaAnnotation<?>> predicate);

    /**
     * Returns {@code true}, if this element is meta-annotated with the given annotation type.
     * A meta-annotation is an annotation that is declared on another annotation.
     *
     * <p>
     * This method also returns {@code true} if this element is directly annotated with the given annotation type.
     * </p>
     *
     * @param annotationType The type of the annotation to check for
     */
    @PublicAPI(usage = ACCESS)
    boolean isMetaAnnotatedWith(Class<? extends Annotation> annotationType);

    /**
     * @param annotationTypeName Fully qualified class name of a specific type of {@link Annotation}
     * @see #isMetaAnnotatedWith(Class)
     */
    @PublicAPI(usage = ACCESS)
    boolean isMetaAnnotatedWith(String annotationTypeName);

    /**
     * Returns {@code true}, if this element is meta-annotated with an annotation matching the given predicate.
     * A meta-annotation is an annotation that is declared on another annotation.
     *
     * <p>
     * This method also returns {@code true} if this element is directly annotated with an annotation matching the given predicate.
     * </p>
     *
     * @param predicate Qualifies matching annotations
     */
    @PublicAPI(usage = ACCESS)
    boolean isMetaAnnotatedWith(DescribedPredicate<? super JavaAnnotation<?>> predicate);

    final class Predicates {
        private Predicates() {
        }

        /**
         * Returns a predicate that matches elements that are annotated with the given annotation type.
         *
         * @param annotationType The type of the annotation to check for
         */
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

        /**
         * @param annotationTypeName Fully qualified class name of a specific type of {@link Annotation}
         * @see #annotatedWith(Class)
         */
        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<CanBeAnnotated> annotatedWith(final String annotationTypeName) {
            DescribedPredicate<HasType> typeNameMatches = GET_RAW_TYPE.then(GET_NAME).is(equalTo(annotationTypeName));
            return annotatedWith(typeNameMatches.as("@" + ensureSimpleName(annotationTypeName)));
        }

        /**
         * Returns a predicate that matches elements that are annotated with an annotation matching the given predicate.
         *
         * @param predicate Qualifies matching annotations
         */
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
            public boolean test(CanBeAnnotated input) {
                return input.isAnnotatedWith(predicate);
            }
        }

        /**
         * Returns a predicate that matches elements that are meta-annotated with the given annotation type.
         * A meta-annotation is an annotation that is declared on another annotation.
         *
         * <p>
         * The returned predicate also matches elements that are directly annotated with the given annotation type.
         * </p>
         *
         * @param annotationType The type of the annotation to check for
         */
        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<CanBeAnnotated> metaAnnotatedWith(final Class<? extends Annotation> annotationType) {
            checkAnnotationHasReasonableRetention(annotationType);

            return metaAnnotatedWith(annotationType.getName());
        }

        /**
         * @see #metaAnnotatedWith(Class)
         */
        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<CanBeAnnotated> metaAnnotatedWith(final String annotationTypeName) {
            DescribedPredicate<HasType> typeNameMatches = GET_RAW_TYPE.then(GET_NAME).is(equalTo(annotationTypeName));
            return metaAnnotatedWith(typeNameMatches.as("@" + ensureSimpleName(annotationTypeName)));
        }

        /**
         * Returns a predicate that matches elements that are meta-annotated with an annotation matching the given predicate.
         * A meta-annotation is an annotation that is declared on another annotation.
         *
         * <p>
         * The returned predicate also matches elements that are directly annotated with the given annotation type.
         * </p>
         *
         * @param predicate Qualifies matching annotations
         */
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
            public boolean test(CanBeAnnotated input) {
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

            return annotations.stream().anyMatch(predicate);
        }

        @PublicAPI(usage = ACCESS)
        public static boolean isMetaAnnotatedWith(
                Collection<? extends JavaAnnotation<?>> annotations,
                DescribedPredicate<? super JavaAnnotation<?>> predicate) {

            return annotations.stream().anyMatch(annotation -> isMetaAnnotatedWith(annotation, predicate, new HashSet<>()));
        }

        private static boolean isMetaAnnotatedWith(
                JavaAnnotation<?> annotation,
                DescribedPredicate<? super JavaAnnotation<?>> predicate,
                Set<String> visitedAnnotations) {

            if (!visitedAnnotations.add(annotation.getRawType().getName())) {
                return false;
            }

            if (predicate.test(annotation)) {
                return true;
            }

            return annotation.getRawType().getAnnotations().stream()
                    .anyMatch(metaAnnotation -> isMetaAnnotatedWith(metaAnnotation, predicate, visitedAnnotations));
        }

        @PublicAPI(usage = ACCESS)
        public static <A extends Annotation> Function<JavaAnnotation<?>, A> toAnnotationOfType(final Class<A> type) {
            return input -> input.as(type);
        }
    }
}
