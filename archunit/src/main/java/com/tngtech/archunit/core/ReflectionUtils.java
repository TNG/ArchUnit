package com.tngtech.archunit.core;

public class ReflectionUtils {
    public static <T> T newInstanceOf(Class<T> type) {
        try {
            return type.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
