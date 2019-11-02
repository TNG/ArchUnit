package com.tngtech.archunit.core.importer;

import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaAnnotationBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaAnnotationBuilder.ValueBuilder;

public class JavaAnnotationTestBuilder {
    private final JavaAnnotationBuilder delegate = new JavaAnnotationBuilder();

    public JavaAnnotationTestBuilder withType(JavaType type) {
        delegate.withType(type);
        return this;
    }

    public JavaAnnotationTestBuilder addProperty(String key, Object value) {
        delegate.addProperty(key, ValueBuilder.ofFinished(value));
        return this;
    }

    public JavaAnnotation<?> build(JavaClass owner, ClassesByTypeName byTypeName) {
        return delegate.build(owner, byTypeName);
    }
}
