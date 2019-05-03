package com.tngtech.archunit.base;

import java.lang.reflect.Constructor;

import com.tngtech.archunit.Internal;

@Internal
public class ReflectionUtils {
    public static <T> T newInstanceOf(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new ArchUnitException.ReflectionException(e);
        }
    }
}
