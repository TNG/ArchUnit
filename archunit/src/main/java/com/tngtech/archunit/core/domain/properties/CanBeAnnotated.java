/*
 * Copyright 2017 TNG Technology Consulting GmbH
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
import java.util.Collection;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.core.domain.JavaAnnotation;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.core.domain.Formatters.ensureSimpleName;

public interface CanBeAnnotated {
    @PublicAPI(usage = ACCESS)
    boolean isAnnotatedWith(Class<? extends Annotation> annotationType);

    @PublicAPI(usage = ACCESS)
    boolean isAnnotatedWith(String annotationTypeName);

    @PublicAPI(usage = ACCESS)
    boolean isAnnotatedWith(DescribedPredicate<? super JavaAnnotation> predicate);

    final class Predicates {
        private Predicates() {
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<CanBeAnnotated> annotatedWith(final Class<? extends Annotation> annotationType) {
            return annotatedWith(annotationType.getName());
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<CanBeAnnotated> annotatedWith(final String annotationTypeName) {
            return new DescribedPredicate<CanBeAnnotated>("annotated with @" + ensureSimpleName(annotationTypeName)) {
                @Override
                public boolean apply(CanBeAnnotated input) {
                    return input.isAnnotatedWith(annotationTypeName);
                }
            };
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<CanBeAnnotated> annotatedWith(final DescribedPredicate<? super JavaAnnotation> predicate) {
            return new DescribedPredicate<CanBeAnnotated>("annotated with " + predicate.getDescription()) {
                @Override
                public boolean apply(CanBeAnnotated input) {
                    return input.isAnnotatedWith(predicate);
                }
            };
        }
    }

    final class Utils {
        private Utils() {
        }

        @PublicAPI(usage = ACCESS)
        public static boolean isAnnotatedWith(Collection<JavaAnnotation> annotations,
                DescribedPredicate<? super JavaAnnotation> predicate) {
            for (JavaAnnotation annotation : annotations) {
                if (predicate.apply(annotation)) {
                    return true;
                }
            }
            return false;
        }

        @PublicAPI(usage = ACCESS)
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
