package com.tngtech.archunit.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.google.common.collect.ImmutableList;

public class TypeDetails {
    private Class<?> type;

    private TypeDetails(Class<?> type) {
        this.type = type;
    }

    public Field[] getDeclaredFields() {
        return type.getDeclaredFields();
    }

    public Method[] getDeclaredMethods() {
        return type.getDeclaredMethods();
    }

    public Constructor<?>[] getDeclaredConstructors() {
        return type.getDeclaredConstructors();
    }

    public String getName() {
        return type.getName();
    }

    public Class<?> getEnclosingClass() {
        return type.getEnclosingClass();
    }

    public Class<?>[] getInterfaces() {
        return type.getInterfaces();
    }

    public Class<?> getSuperclass() {
        return type.getSuperclass();
    }

    public String getSimpleName() {
        return type.getSimpleName();
    }

    public String getPackage() {
        return type.getPackage() != null ? type.getPackage().getName() : "";
    }

    public boolean isInterface() {
        return type.isInterface();
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final TypeDetails other = (TypeDetails) obj;
        return Objects.equals(this.type, other.type);
    }

    public static List<TypeDetails> allOf(Class<?>... types) {
        return allOf(ImmutableList.copyOf(types));
    }

    public static List<TypeDetails> allOf(Collection<Class<?>> types) {
        ImmutableList.Builder<TypeDetails> result = ImmutableList.builder();
        for (Class<?> type : types) {
            result.add(TypeDetails.of(type));
        }
        return result.build();
    }

    public static TypeDetails of(Class<?> type) {
        return new TypeDetails(type);
    }
}
