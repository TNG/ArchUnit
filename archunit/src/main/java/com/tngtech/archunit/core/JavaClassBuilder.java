package com.tngtech.archunit.core;

import java.util.HashSet;
import java.util.Set;

import com.tngtech.archunit.base.Optional;

final class JavaClassBuilder {
    private Optional<Source> source = Optional.absent();
    private JavaType javaType;
    private boolean isInterface;
    private Set<JavaModifier> modifiers = new HashSet<>();

    JavaClassBuilder withSource(Source source) {
        this.source = Optional.of(source);
        return this;
    }

    @SuppressWarnings("unchecked")
    JavaClassBuilder withType(JavaType javaType) {
        this.javaType = javaType;
        return this;
    }

    JavaClassBuilder withInterface(boolean isInterface) {
        this.isInterface = isInterface;
        return this;
    }

    JavaClassBuilder withModifiers(Set<JavaModifier> modifiers) {
        this.modifiers = modifiers;
        return this;
    }

    JavaClass build() {
        return new JavaClass(this);
    }

    public Optional<Source> getSource() {
        return source;
    }

    public JavaType getJavaType() {
        return javaType;
    }

    public boolean isInterface() {
        return isInterface;
    }

    public Set<JavaModifier> getModifiers() {
        return modifiers;
    }
}
