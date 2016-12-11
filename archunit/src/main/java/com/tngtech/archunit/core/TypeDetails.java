package com.tngtech.archunit.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import org.objectweb.asm.Type;

import static com.tngtech.archunit.core.ReflectionUtils.classForName;

public class TypeDetails {
    private Class<?> type;

    private TypeDetails(Class<?> type) {
        this.type = type;
    }

    Set<JavaAnnotation> getAnnotations() {
        return JavaAnnotation.allOf(type.getAnnotations());
    }

    Field[] getDeclaredFields() {
        return type.getDeclaredFields();
    }

    Method[] getDeclaredMethods() {
        return type.getDeclaredMethods();
    }

    Constructor<?>[] getDeclaredConstructors() {
        return type.getDeclaredConstructors();
    }

    public String getName() {
        return type.getName();
    }

    Optional<TypeDetails> getEnclosingClass() {
        return type.getEnclosingClass() != null ?
                Optional.of(TypeDetails.of(type.getEnclosingClass())) :
                Optional.<TypeDetails>absent();
    }

    List<TypeDetails> getInterfaces() {
        return TypeDetails.allOf(type.getInterfaces());
    }

    Optional<TypeDetails> getSuperclass() {
        return type.getSuperclass() != null ?
                Optional.of(TypeDetails.of(type.getSuperclass())) :
                Optional.<TypeDetails>absent();
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

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + type.getName() + "}";
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

    public static List<TypeDetails> allOf(Type[] types) {
        ImmutableList.Builder<TypeDetails> result = ImmutableList.builder();
        for (Type type : types) {
            result.add(TypeDetails.of(type));
        }
        return result.build();
    }

    public static TypeDetails of(Class<?> type) {
        return new TypeDetails(type);
    }

    public static TypeDetails of(Type type) {
        return new TypeDetails(classForName(type.getClassName()));
    }
}
