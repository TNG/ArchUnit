package com.tngtech.archunit.core;

final class JavaConstructorBuilder extends JavaCodeUnitBuilder<JavaConstructor, JavaConstructorBuilder> {
    @Override
    JavaConstructor construct(JavaConstructorBuilder builder, ImportedClasses.ByTypeName importedClasses) {
        return new JavaConstructor(builder);
    }
}
