package com.tngtech.archunit.core;

import java.util.Set;

interface ClassResolver {
    JavaClass resolve(String typeName);

    Set<JavaClass> getAllSuperClasses(String className);
}
