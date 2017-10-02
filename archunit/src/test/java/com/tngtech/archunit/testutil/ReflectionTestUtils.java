package com.tngtech.archunit.testutil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class ReflectionTestUtils {
    public static Field field(Class<?> clazz, String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static Constructor<?> constructor(Class<?> clazz, Class<?>... parameterTypes) {
        try {
            return clazz.getDeclaredConstructor(parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static Method method(Class<?> clazz, String name, Class<?>... parameterTypes) {
        try {
            return clazz.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static Set<Class<?>> getHierarchy(Class<?> clazz) {
        Set<Class<?>> result = new HashSet<>();
        result.add(clazz);
        if (clazz.getSuperclass() != null) {
            result.addAll(getHierarchy(clazz.getSuperclass()));
        }
        for (Class<?> i : clazz.getInterfaces()) {
            result.addAll(getHierarchy(i));
        }
        return result;
    }
}
