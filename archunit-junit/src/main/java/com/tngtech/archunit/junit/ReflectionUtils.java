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
package com.tngtech.archunit.junit;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.ArchUnitException.ReflectionException;
import com.tngtech.archunit.base.Function;

import static com.google.common.collect.Collections2.filter;

class ReflectionUtils {
    private ReflectionUtils() {
    }

    static Set<Class<?>> getAllSuperTypes(Class<?> type) {
        if (type == null) {
            return Collections.emptySet();
        }

        ImmutableSet.Builder<Class<?>> result = ImmutableSet.<Class<?>>builder()
                .add(type)
                .addAll(getAllSuperTypes(type.getSuperclass()));
        for (Class<?> c : type.getInterfaces()) {
            result.addAll(getAllSuperTypes(c));
        }
        return result.build();
    }

    static Collection<Field> getAllFields(Class<?> owner, Predicate<? super Field> predicate) {
        return filter(getAll(owner, new Collector<Field>() {
            @Override
            protected Collection<? extends Field> extractFrom(Class<?> type) {
                return ImmutableList.copyOf(type.getDeclaredFields());
            }
        }), toGuava(predicate));
    }

    static Collection<Method> getAllMethods(Class<?> owner, Predicate<? super Method> predicate) {
        return filter(getAll(owner, new Collector<Method>() {
            @Override
            protected Collection<? extends Method> extractFrom(Class<?> type) {
                return ImmutableList.copyOf(type.getDeclaredMethods());
            }
        }), toGuava(predicate));
    }

    private static <T> List<T> getAll(Class<?> type, Collector<T> collector) {
        for (Class<?> t : getAllSuperTypes(type)) {
            collector.collectFrom(t);
        }
        return collector.collected;
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

    static <T> T invokeMethod(Method method, Class<?> methodOwner, Object... args) {
        if (Modifier.isStatic(method.getModifiers())) {
            return invoke(null, method, args);
        } else {
            return invoke(newInstanceOf(methodOwner), method, args);
        }
    }

    @SuppressWarnings("unchecked") // callers must know, what they do here, we can't make this compile safe anyway
    private static <T> T invoke(Object owner, Method method, Object... args) {
        method.setAccessible(true);
        try {
            return (T) method.invoke(owner, args);
        } catch (IllegalAccessException e) {
            throw new ReflectionException(e);
        } catch (InvocationTargetException e) {
            ReflectionUtils.<RuntimeException>rethrowUnchecked(e.getTargetException());
            return null; // will never be reached
        }
    }

    // Certified Hack(TM) to rethrow any exception unchecked. Uses a hole in the JLS with respect to Generics.
    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void rethrowUnchecked(Throwable throwable) throws T {
        throw (T) throwable;
    }

    private abstract static class Collector<T> {
        private final List<T> collected = new ArrayList<>();

        void collectFrom(Class<?> type) {
            collected.addAll(extractFrom(type));
        }

        protected abstract Collection<? extends T> extractFrom(Class<?> type);
    }

    static Predicate<AnnotatedElement> withAnnotation(final Class<? extends Annotation> annotationType) {
        return new Predicate<AnnotatedElement>() {
            @Override
            public boolean apply(AnnotatedElement input) {
                return input.getAnnotation(annotationType) != null;
            }
        };
    }

    interface Predicate<T> {
        boolean apply(T input);
    }

    private static <T> com.google.common.base.Predicate<T> toGuava(final Predicate<T> predicate) {
        return new com.google.common.base.Predicate<T>() {
            @Override
            public boolean apply(T input) {
                return predicate.apply(input);
            }
        };
    }
}
