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

    boolean contain(String name) {
        return directlyImported.containsKey(name) || additionalClasses.containsKey(name);
    }

    void ensurePresent(String typeName) {
        if (!contain(typeName)) {
            Optional<JavaClass> resolved = resolver.resolve(typeName, byType());
            JavaClass newClass = resolved.isPresent() ? resolved.get() : simpleClassOf(typeName);
            additionalClasses.put(typeName, newClass);
        }
    }

    static JavaClass simpleClassOf(String typeName) {
        return new JavaClass.Builder().withType(TypeDetails.of(typeName)).build();
    }

    void add(JavaClass clazz) {
        additionalClasses.put(clazz.getName(), clazz);
    }

    void add(Map<String, JavaClass> additionalClasses) {
        this.additionalClasses.putAll(additionalClasses);
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
