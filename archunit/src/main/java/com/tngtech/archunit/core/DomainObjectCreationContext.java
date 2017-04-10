package com.tngtech.archunit.core;

import java.util.Map;

import com.tngtech.archunit.Internal;

@Internal
public class DomainObjectCreationContext {
    public static void completeClassHierarchy(JavaClass javaClass, ImportContext importContext) {
        javaClass.completeClassHierarchyFrom(importContext);
    }

    public static JavaClasses createJavaClasses(Map<String, JavaClass> classes, ImportContext importContext) {
        return JavaClasses.of(classes, importContext);
    }

    public static void completeMembers(JavaClass javaClass, ImportContext importContext) {
        javaClass.completeMembers(importContext);
    }
}
