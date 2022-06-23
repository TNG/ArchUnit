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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.tngtech.archunit.base.ArchUnitException.ReflectionException;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

public class ReflectionUtils {

    private static final com.google.common.base.Function<Object, Class<?>> EXTRACTING_CLASS = new com.google.common.base.Function<Object, Class<?>>() {
        @Override
        public Class<?> apply(Object input) {
            return input.getClass();
        }
    };

    private ReflectionUtils() {
    }

    static Set<Class<?>> getAllSupertypes(Class<?> type) {
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

    static <T> T newInstanceOf(Class<T> type) {
        return com.tngtech.archunit.base.ReflectionUtils.newInstanceOf(type);
    }

    @SuppressWarnings("unchecked") // callers must know, what they do here, we can't make this compile safe anyway
    private static <T> T getValue(Field field, Object owner) {
        try {
            field.setAccessible(true);
            return (T) field.get(owner);
        } catch (IllegalAccessException e) {
            throw new ReflectionException(e);
        }
    }

    public static <T> T getValueOrThrowException(Field field, Class<?> fieldOwner, Function<Throwable, ? extends RuntimeException> exceptionConverter) {
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

    static <T> T invokeMethod(Method method, Class<?> methodOwner, Object... args) {
        if (Modifier.isStatic(method.getModifiers())) {
            return invokeMethod(null, method, args);
        } else {
            return invokeMethod(newInstanceOf(methodOwner), method, args);
        }
    }

    @SuppressWarnings("unchecked") // callers must know, what they do here, we can't make this compile safe anyway
    public static <T> T invokeMethod(Object owner, Method method, Object... args) {
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

    public static <T> T invokeMethod(Object owner, String methodName, Object... args) {
        return invokeMethod(owner, Objects.requireNonNull(findMethod(owner, methodName, args)), args);
    }

    private static Method findMethod(Object owner, String methodName, Object[] args) {
        try {
            return owner.getClass().getMethod(
                    methodName,
                    Iterables.toArray(Iterables.transform(Arrays.asList(args), EXTRACTING_CLASS), Class.class)
            );
        } catch (NoSuchMethodException e) {
            ReflectionUtils.rethrowUnchecked(e);
            return null; // will never be reached
        }
    }

    // Certified Hack(TM) to rethrow any exception unchecked. Uses a hole in the JLS with respect to Generics.
    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void rethrowUnchecked(Throwable throwable) throws T {
        throw (T) throwable;
    }

    static Predicate<AnnotatedElement> withAnnotation(final Class<? extends Annotation> annotationType) {
        return input -> input.isAnnotationPresent(annotationType);
    }
}
