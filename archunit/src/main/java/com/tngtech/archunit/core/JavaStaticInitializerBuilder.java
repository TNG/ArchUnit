package com.tngtech.archunit.core;

import java.util.Collections;

class JavaStaticInitializerBuilder extends JavaCodeUnitBuilder<JavaStaticInitializer, JavaStaticInitializerBuilder> {
    public JavaStaticInitializerBuilder() {
        withReturnType(JavaType.From.name(void.class.getName()));
        withParameters(Collections.<JavaType>emptyList());
        withName(JavaStaticInitializer.STATIC_INITIALIZER_NAME);
        withDescriptor("()V");
        withAnnotations(Collections.<JavaAnnotationBuilder>emptySet());
        withModifiers(Collections.<JavaModifier>emptySet());
    }

    @Override
    JavaStaticInitializer construct(JavaStaticInitializerBuilder builder, ImportedClasses.ByTypeName importedClasses) {
        return new JavaStaticInitializer(builder);
    }
}
