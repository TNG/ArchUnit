package com.tngtech.archunit.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.ImmutableList;

public class ReflectionUtils {
    public static List<Field> getAllFieldsSortedFromChildToParent(Class<?> type) {
        return getAll(type, new Collector<Field>() {
            @Override
            protected Collection<? extends Field> extractFrom(Class<?> type) {
                return ImmutableList.copyOf(type.getDeclaredFields());
            }
        });
    }

    public static List<Constructor<?>> getAllConstructorsSortedFromChildToParent(Class<?> type) {
        return getAll(type, new Collector<Constructor<?>>() {
            @Override
            protected Collection<? extends Constructor<?>> extractFrom(Class<?> type) {
                return ImmutableList.copyOf(type.getDeclaredConstructors());
            }
        });
    }

    public static List<Method> getAllMethodsSortedFromChildToParent(Class<?> type) {
        return getAll(type, new Collector<Method>() {
            @Override
            protected Collection<? extends Method> extractFrom(Class<?> type) {
                return ImmutableList.copyOf(type.getDeclaredMethods());
            }
        });
    }

    public static <T> T newInstanceOf(Class<T> type) {
        try {
            return type.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> List<T> getAll(Class<?> type, Collector<T> collector) {
        Class<?> current = type;
        while (current != null) {
            collector.collectFrom(current);
            current = current.getSuperclass();
        }

        return collector.collected;
    }

    private static abstract class Collector<T> {
        private final List<T> collected = new ArrayList<>();

        void collectFrom(Class<?> type) {
            collected.addAll(extractFrom(type));
        }

        protected abstract Collection<? extends T> extractFrom(Class<?> type);
    }
}
