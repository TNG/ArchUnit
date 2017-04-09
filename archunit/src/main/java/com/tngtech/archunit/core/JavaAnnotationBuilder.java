package com.tngtech.archunit.core;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.tngtech.archunit.base.Optional;

class JavaAnnotationBuilder {
    private JavaType type;
    private Map<String, ValueBuilder> values = new HashMap<>();
    private ImportedClasses.ByTypeName importedClasses;

    JavaAnnotationBuilder withType(JavaType type) {
        this.type = type;
        return this;
    }

    JavaType getJavaType() {
        return type;
    }

    JavaAnnotationBuilder addProperty(String key, ValueBuilder valueBuilder) {
        values.put(key, valueBuilder);
        return this;
    }

    JavaAnnotation build(ImportedClasses.ByTypeName importedClasses) {
        this.importedClasses = importedClasses;
        return new JavaAnnotation(this);
    }

    public JavaClass getType() {
        return importedClasses.get(type.getName());
    }

    public Map<String, Object> getValues() {
        ImmutableMap.Builder<String, Object> result = ImmutableMap.builder();
        for (Map.Entry<String, ValueBuilder> entry : values.entrySet()) {
            Optional<Object> value = entry.getValue().build(importedClasses);
            if (value.isPresent()) {
                result.put(entry.getKey(), value.get());
            }
        }
        addDefaultValues(result, importedClasses);
        return result.build();
    }

    private void addDefaultValues(ImmutableMap.Builder<String, Object> result, ImportedClasses.ByTypeName importedClasses) {
        for (JavaMethod method : importedClasses.get(type.getName()).getMethods()) {
            if (!values.containsKey(method.getName()) && method.getDefaultValue().isPresent()) {
                result.put(method.getName(), method.getDefaultValue().get());
            }
        }
    }

    abstract static class ValueBuilder {
        abstract Optional<Object> build(ImportedClasses.ByTypeName importedClasses);

        static ValueBuilder ofFinished(final Object value) {
            return new ValueBuilder() {
                @Override
                Optional<Object> build(ImportedClasses.ByTypeName importedClasses) {
                    return Optional.of(value);
                }
            };
        }

        static ValueBuilder from(final JavaAnnotationBuilder builder) {
            return new ValueBuilder() {
                @Override
                Optional<Object> build(ImportedClasses.ByTypeName importedClasses) {
                    return Optional.<Object>of(builder.build(importedClasses));
                }
            };
        }
    }
}
