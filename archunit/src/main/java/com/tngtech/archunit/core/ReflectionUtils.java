package com.tngtech.archunit.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.tngtech.archunit.core.ArchUnitException.ReflectionException;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.collect.Collections2.filter;
import static com.google.common.primitives.Primitives.allPrimitiveTypes;

public class ReflectionUtils {
    private static final Logger LOG = LoggerFactory.getLogger(ReflectionUtils.class);

    private static final Map<String, Class<?>> primitiveClassesByName =
            Maps.uniqueIndex(allPrimitiveTypes(), new Function<Class<?>, String>() {
                @Override
                public String apply(Class<?> input) {
                    return input.getName();
                }
            });

    private ReflectionUtils() {
    }

    static Class<?> classForName(String name) {
        return classForName(name, ReflectionUtils.class.getClassLoader());
    }

    static Optional<Class<?>> tryGetClassForName(String name) {
        try {
            return Optional.<Class<?>>of(classForName(name));
        } catch (NoClassDefFoundError e) {
            LOG.warn("Can't analyse related type of '{}' because of missing dependency '{}'",
                    name, e.getMessage());
        } catch (ReflectionException e) {
            LOG.warn("Can't analyse related type of '{}' because of missing dependency. Error was: '{}'",
                    name, e.getMessage());
        }
        return Optional.absent();
    }

    static Class<?> classForName(String name, ClassLoader classLoader) {
        if (primitiveClassesByName.containsKey(name)) {
            return primitiveClassesByName.get(name);
        }
        name = ensureCorrectArrayTypeName(name);
        try {
            return Class.forName(name, false, classLoader);
        } catch (Throwable e) {
            throw new ReflectionException(e);
        }
    }

    static String ensureCorrectArrayTypeName(String name) {
        return name.endsWith("[]") ? fixArrayTypeName(name) : name;
    }

    private static String fixArrayTypeName(String name) {
        String componentTypeName = name.replaceAll("(\\[\\])*$", "");

        componentTypeName = primitiveClassesByName.containsKey(componentTypeName) ?
                Type.getDescriptor(primitiveClassesByName.get(componentTypeName)) :
                Type.getObjectType(componentTypeName.replaceAll("/", ".")).getDescriptor();

        String arrayDesignator = Strings.repeat("[", CharMatcher.is('[').countIn(name));

        return arrayDesignator + componentTypeName;
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

    public static Collection<Constructor<?>> getAllConstructors(Class<?> owner, Predicate<? super Constructor<?>> predicate) {
        return filter(getAll(owner, new Collector<Constructor<?>>() {
            @Override
            protected Collection<? extends Constructor<?>> extractFrom(Class<?> type) {
                return ImmutableList.copyOf(type.getDeclaredConstructors());
            }
        }), toGuava(predicate));
    }

    public static Collection<Field> getAllFields(Class<?> owner, Predicate<? super Field> predicate) {
        return filter(getAll(owner, new Collector<Field>() {
            @Override
            protected Collection<? extends Field> extractFrom(Class<?> type) {
                return ImmutableList.copyOf(type.getDeclaredFields());
            }
        }), toGuava(predicate));
    }

    public static Collection<Method> getAllMethods(Class<?> owner, Predicate<? super Method> predicate) {
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

    public static List<String> namesOf(Class<?>... paramTypes) {
        return namesOf(ImmutableList.copyOf(paramTypes));
    }

    public static List<String> namesOf(List<Class<?>> paramTypes) {
        ArrayList<String> result = new ArrayList<>();
        for (Class<?> paramType : paramTypes) {
            result.add(paramType.getName());
        }
        return result;
    }

    private static abstract class Collector<T> {
        private final List<T> collected = new ArrayList<>();

        void collectFrom(Class<?> type) {
            collected.addAll(extractFrom(type));
        }

        protected abstract Collection<? extends T> extractFrom(Class<?> type);
    }

    // NOTE: Don't use Guava classes in public API
    public interface Predicate<T> {
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
