package com.tngtech.archunit.core;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.tngtech.archunit.base.Optional;

class JavaMethodBuilder extends JavaCodeUnitBuilder<JavaMethod, JavaMethodBuilder> {
    private Optional<JavaAnnotationBuilder.ValueBuilder> annotationDefaultValueBuilder = Optional.absent();
    private Supplier<Optional<Object>> annotationDefaultValue = Suppliers.ofInstance(Optional.absent());

    JavaMethodBuilder withAnnotationDefaultValue(JavaAnnotationBuilder.ValueBuilder defaultValue) {
        annotationDefaultValueBuilder = Optional.of(defaultValue);
        return this;
    }

    Supplier<Optional<Object>> getAnnotationDefaultValue() {
        return annotationDefaultValue;
    }

    @Override
    JavaMethod construct(JavaMethodBuilder builder, final ImportedClasses.ByTypeName importedClasses) {
        if (annotationDefaultValueBuilder.isPresent()) {
            annotationDefaultValue = Suppliers.memoize(new Supplier<Optional<Object>>() {
                @Override
                public Optional<Object> get() {
                    return annotationDefaultValueBuilder.get().build(importedClasses);
                }
            });
        }
        return new JavaMethod(builder);
    }
}
