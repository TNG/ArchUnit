package com.tngtech.archunit.core;

interface ClassResolver {
    Optional<JavaClass> tryResolve(String typeName, ImportedClasses.ByTypeName importedClasses);
}
