/*
 * Copyright 2018 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.junit.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.ArchUnitException.ReflectionException;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

class ReflectionUtils {
    private ReflectionUtils() {
    }

    static Collection<Field> getAllFields(Class<?> owner, Predicate<? super Field> predicate) {
        return getAll(owner, Class::getDeclaredFields).filter(predicate).collect(toList());
    }

    static Collection<Method> getAllMethods(Class<?> owner, Predicate<? super Method> predicate) {
        return getAll(owner, Class::getDeclaredMethods).filter(predicate).collect(toList());
    }

    private static <T> Stream<T> getAll(Class<?> type, Function<Class<?>, T[]> collector) {
        Stream.Builder<T> result = Stream.builder();
        for (Class<?> t : getAllSupertypes(type)) {
            stream(collector.apply(t)).forEach(result);
        }
        return result.build();
    }

    private static Set<Class<?>> getAllSupertypes(Class<?> type) {
        if (type == null) {
            return Collections.emptySet();
        }

        ImmutableSet.Builder<Class<?>> result = ImmutableSet.<Class<?>>builder()
                .add(type)
                .addAll(getAllSupertypes(type.getSuperclass()));
        for (Class<?> c : type.getInterfaces()) {
            result.addAll(getAllSupertypes(c));
        }
        return result.build();
    }

    static <T> T newInstanceOf(Class<T> type) {
        return com.tngtech.archunit.base.ReflectionUtils.newInstanceOf(type);
    }

    static <T> T getValueOrThrowException(Field field, Class<?> fieldOwner, Function<Throwable, ? extends RuntimeException> exceptionConverter) {
        try {
            if (Modifier.isStatic(field.getModifiers())) {
                return getValue(field, null);
            } else {
                return getValue(field, newInstanceOf(fieldOwner));
            }
        } catch (ReflectionException e) {
            throw exceptionConverter.apply(e.getCause());
        }
    }

    @SuppressWarnings("unchecked") // callers must know what they do here, we can't make this compile safe anyway
    private static <T> T getValue(Field field, Object owner) {
        try {
            field.setAccessible(true);
            return (T) field.get(owner);
        } catch (IllegalAccessException e) {
            throw new ReflectionException(e);
        }
    }

    static <T> T invokeMethod(Method method, Class<?> methodOwner, Object... args) {
        if (Modifier.isStatic(method.getModifiers())) {
            return invoke(null, method, args);
        } else {
            return invoke(newInstanceOf(methodOwner), method, args);
        }
    }

    @SuppressWarnings("unchecked") // callers must know what they do here, we can't make this compile safe anyway
    private static <T> T invoke(Object owner, Method method, Object... args) {
        method.setAccessible(true);
        try {
            return (T) method.invoke(owner, args);
        } catch (IllegalAccessException e) {
            throw new ReflectionException(e);
        } catch (InvocationTargetException e) {
            ReflectionUtils.rethrowUnchecked(e.getTargetException());
            return null; // will never be reached
        }
    }

    // Certified Hack(TM) to rethrow any exception unchecked. Uses a hole in the JLS with respect to Generics.
    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void rethrowUnchecked(Throwable throwable) throws T {
        throw (T) throwable;
    }

    static Predicate<AnnotatedElement> withAnnotation(Class<? extends Annotation> annotationType) {
        return input -> input.isAnnotationPresent(annotationType);
    }

    /**
     * Same as {@link #tryFindAnnotation(Class, Class)}, but throws an exception if no annotation can be found.
     */
    static <T extends Annotation> T findAnnotation(final Class<?> clazz, Class<T> annotationType) {
        return tryFindAnnotation(clazz, annotationType).orElseThrow(() ->
                new ArchTestInitializationException("Class %s is not (meta-)annotated with @%s", clazz.getName(), annotationType.getSimpleName()));
    }

    /**
     * Recursively searches for an annotation of type {@link T} on the given {@code clazz}.
     * Returns the first matching annotation that is found.
     * Any further matching annotation possibly present within the meta-annotation hierarchy will be ignored.
     * If no matching annotation can be found {@link Optional#empty()} will be returned.
     *
     * @param clazz The {@link Class} from which to retrieve the annotation
     * @return The first found annotation instance reachable in the meta-annotation hierarchy or {@link Optional#empty()} if none can be found
     */
    static <T extends Annotation> Optional<T> tryFindAnnotation(final Class<?> clazz, Class<T> annotationType) {
        return tryFindAnnotation(clazz.getAnnotations(), annotationType, new HashSet<>());
    }

    private static <T extends Annotation> Optional<T> tryFindAnnotation(final Annotation[] annotations, Class<T> annotationType, Set<Annotation> visited) {
        for (Annotation annotation : annotations) {
            if (!visited.add(annotation)) {
                continue;
            }

            Optional<T> result = annotationType.isInstance(annotation)
                    ? Optional.of(annotationType.cast(annotation))
                    : tryFindAnnotation(annotation.annotationType().getAnnotations(), annotationType, visited);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }
}
