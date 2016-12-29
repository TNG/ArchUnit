package com.tngtech.archunit.core;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

class ImportedClasses {
    private final ImmutableMap<String, JavaClass> directlyImported;
    private final Map<String, JavaClass> additionalClasses = new HashMap<>();
    private final ClassResolver resolver;

    ImportedClasses(Map<String, JavaClass> directlyImported, ClassResolver resolver) {
        this.directlyImported = ImmutableMap.copyOf(directlyImported);
        this.resolver = resolver;
    }

    Map<String, JavaClass> getDirectlyImported() {
        return directlyImported;
    }

    boolean contain(String name) {
        return directlyImported.containsKey(name) || additionalClasses.containsKey(name);
    }

    void ensurePresent(String typeName) {
        if (!contain(typeName)) {
            Optional<JavaClass> resolved = resolver.tryResolve(typeName, byType());
            JavaClass newClass = resolved.isPresent() ? resolved.get() : simpleClassOf(typeName);
            additionalClasses.put(typeName, newClass);
        }
    }

    private static JavaClass simpleClassOf(String typeName) {
        return new JavaClass.Builder().withType(JavaType.From.name(typeName)).build();
    }

    void add(JavaClass clazz) {
        additionalClasses.put(clazz.getName(), clazz);
    }

    JavaClass get(String typeName) {
        ensurePresent(typeName);
        return directlyImported.containsKey(typeName) ?
                directlyImported.get(typeName) :
                additionalClasses.get(typeName);
    }

    Map<String, JavaClass> getAll() {
        return ImmutableMap.<String, JavaClass>builder()
                .putAll(directlyImported)
                .putAll(additionalClasses)
                .build();
    }

    ByTypeName byType() {
        return new ByTypeName() {
            @Override
            public boolean contain(String typeName) {
                return ImportedClasses.this.contain(typeName);
            }

            @Override
            public JavaClass get(String typeName) {
                return ImportedClasses.this.get(typeName);
            }
        };
    }

    interface ByTypeName {
        boolean contain(String typeName);

        JavaClass get(String typeName);
    }
}
