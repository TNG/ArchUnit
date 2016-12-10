package com.tngtech.archunit.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

class ImportedClasses {
    private final ImmutableMap<String, JavaClass> directlyImported;
    private final Map<String, JavaClass> additionalClasses = new HashMap<>();
    private final ClassResolver resolver;

    ImportedClasses(Collection<JavaClass> directlyImported, ClassResolver resolver) {
        ImmutableMap.Builder<String, JavaClass> directlyImportedByTypeName = ImmutableMap.builder();
        for (JavaClass javaClass : directlyImported) {
            directlyImportedByTypeName.put(javaClass.getName(), javaClass);
        }
        this.directlyImported = directlyImportedByTypeName.build();
        this.resolver = resolver;
    }

    Map<String, JavaClass> getDirectlyImported() {
        return directlyImported;
    }

    void add(Map<String, JavaClass> additionalClasses) {
        this.additionalClasses.putAll(additionalClasses);
    }

    boolean contain(String name) {
        return directlyImported.containsKey(name) || additionalClasses.containsKey(name);
    }

    JavaClass get(String typeName) {
        if (directlyImported.containsKey(typeName)) {
            return directlyImported.get(typeName);
        }
        if (!additionalClasses.containsKey(typeName)) {
            additionalClasses.put(typeName, resolver.resolve(typeName));
        }
        return additionalClasses.get(typeName);
    }

    Map<String, JavaClass> getAll() {
        return ImmutableMap.<String, JavaClass>builder()
                .putAll(directlyImported)
                .putAll(additionalClasses)
                .build();
    }
}
