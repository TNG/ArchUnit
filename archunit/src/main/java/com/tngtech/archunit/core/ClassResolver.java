package com.tngtech.archunit.core;

import java.util.Map;

interface ClassResolver {
    Optional<JavaClass> resolve(String typeName, ImportedClasses.ByTypeName importedClasses);

    Map<String, Optional<JavaClass>> getAllSuperClasses(String className, ImportedClasses.ByTypeName importedClasses);
}
