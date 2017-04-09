package com.tngtech.archunit.core;

final class JavaFieldBuilder extends JavaMemberBuilder<JavaField, JavaFieldBuilder> {
    private JavaType type;

    JavaFieldBuilder withType(JavaType type) {
        this.type = type;
        return self();
    }

    public JavaClass getType() {
        return get(type.getName());
    }

    @Override
    JavaField construct(JavaFieldBuilder builder, ImportedClasses.ByTypeName importedClasses) {
        return new JavaField(builder);
    }
}
