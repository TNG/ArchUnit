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
package com.tngtech.archunit.junit;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import com.tngtech.archunit.junit.internal.AdapterFor;
import org.junit.platform.commons.support.AnnotationSupport;

abstract class AnnotationUtils {
    private AnnotationUtils() {}

    /**
     * Creates a stream of all annotations present on a given element, including repeatable annotations.
     *
     * Also resolves targets for {@link AdapterFor} by creating annotation proxies.
     *
     * @param element The annotated element
     * @param annotations The annotation types to look for
     * @return a stream
     */
    @SafeVarargs
    public static Stream<? extends Annotation> streamRepeatableAnnotations(AnnotatedElementComposite element, Class<? extends Annotation>... annotations) {
        return Arrays.stream(annotations)
                .flatMap(annotationType -> streamRepeatableAnnotations(element, annotationType));
    }

    /**
     * Creates a stream of all annotations present on a given element, treating them as non-repeatable annotations.
     *
     * Also resolves targets for {@link AdapterFor} by creating annotation proxies.
     *
     * @param element The annotated element
     * @param annotations The annotation types to look for
     * @return a stream
     */
    @SafeVarargs
    public static Stream<? extends Annotation> streamAnnotations(AnnotatedElementComposite element, Class<? extends Annotation>... annotations) {
        return Arrays.stream(annotations)
                .flatMap(annotationType -> streamAnnotations(element, annotationType));
    }

    public static Annotation dereferenceAnnotation(Annotation source) {
        return getAdapterTarget(source)
                .map(targetType -> (Annotation) proxyAnnotation(source, targetType))
                .orElse(source);
    }

    public static AnnotatedElement dereferenced(final AnnotatedElement target) {
        return new DereferencedAnnotatedElementWrapper(target);
    }

    private static Stream<? extends Annotation> streamAnnotations(AnnotatedElementComposite element, Class<? extends Annotation> annotationType) {
        return element.getChildren().stream()
                .map(child -> AnnotationSupport.findAnnotation(child, annotationType))
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    private static Stream<? extends Annotation> streamRepeatableAnnotations(AnnotatedElementComposite element,
            Class<? extends Annotation> annotationType) {
        return element.getChildren().stream()
                .map(child -> AnnotationSupport.findRepeatableAnnotations(child, annotationType))
                .flatMap(Collection::stream);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Annotation> T proxyAnnotation(Annotation annotation, Class<T> target) {
        return (T) Proxy.newProxyInstance(AnnotationUtils.class.getClassLoader(), new Class<?>[]{target}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if ("annotationType".equals(method.getName()) || "getClass".equals(method.getName())) {
                    return target;
                }
                return annotation.annotationType().getMethod(method.getName(), method.getParameterTypes()).invoke(annotation, args);
            }
        });
    }

    private static <T> Optional<Class<? extends Annotation>> getAdapterTarget(Annotation annotation) {
        return Optional.ofNullable(annotation.annotationType().getAnnotation(AdapterFor.class))
                .map(AdapterFor::value);
    }

}
