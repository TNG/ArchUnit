package com.tngtech.archunit.core;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

import static com.tngtech.archunit.core.ReflectionUtils.classForName;
import static com.tngtech.archunit.core.ReflectionUtils.getAllSuperTypes;

/**
 * Temporary class to resolve type names to {@link JavaClass}, since we want to get rid of all the use
 * of reflection in the import process.
 * This class will be removed in a future commit and be replaced by some way of resolving a {@link JavaClass}
 * without using reflection.
 */
class ImportWorkaround {
    static Set<JavaClass> getAllSuperClasses(String typeName) {
        ImmutableSet.Builder<JavaClass> result = ImmutableSet.builder();
        for (Class<?> type : getAllSuperTypes(classForName(typeName))) {
            result.add(new JavaClass.Builder().withType(TypeDetails.of(type)).build());
        }
        return result.build();
    }

    static JavaClass resolveClass(String typeName) {
        return new JavaClass.Builder().withType(TypeDetails.of(classForName(typeName))).build();
    }
}
