package com.tngtech.archunit.core;

import java.util.Map;
import java.util.Set;

interface ClassResolver {
    JavaClass resolve(String typeName);

    Set<JavaClass> getAllSuperClasses(String className, Map<String, JavaClass> importedClasses);
}
