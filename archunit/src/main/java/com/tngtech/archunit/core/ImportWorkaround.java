package com.tngtech.archunit.core;

import java.util.Collections;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.tngtech.archunit.core.ReflectionUtils.classForName;
import static com.tngtech.archunit.core.ReflectionUtils.getAllSuperTypes;

/**
 * Temporary class to resolve type names to {@link JavaClass}, since we want to get rid of all the use
 * of reflection in the import process.
 * This class will be removed in a future commit and be replaced by some way of resolving a {@link JavaClass}
 * without using reflection.
 */
class ImportWorkaround {
    private static final Logger LOG = LoggerFactory.getLogger(ImportWorkaround.class);

    static Set<JavaClass> getAllSuperClasses(String typeName) {
        try {
            return tryGetAllSuperClasses(typeName);
        } catch (NoClassDefFoundError e) {
            LOG.warn("Can't analyse related type of '{}' because of missing dependency '{}'",
                    typeName, e.getMessage());
        } catch (ReflectionException e) {
            LOG.warn("Can't analyse related type of '{}' because of missing dependency. Error was: '{}'",
                    typeName, e.getMessage());
        }
        return Collections.emptySet();
    }

    private static Set<JavaClass> tryGetAllSuperClasses(String typeName) {
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
