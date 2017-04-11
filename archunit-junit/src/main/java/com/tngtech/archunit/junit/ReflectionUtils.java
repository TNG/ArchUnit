package com.tngtech.archunit.junit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

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

    private abstract static class Collector<T> {
        private final List<T> collected = new ArrayList<>();

        void collectFrom(Class<?> type) {
            collected.addAll(extractFrom(type));
        }

        protected abstract Collection<? extends T> extractFrom(Class<?> type);
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
