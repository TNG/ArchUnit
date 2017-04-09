package com.tngtech.archunit.core;

import java.util.Map;
import java.util.Set;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

import static com.tngtech.archunit.core.JavaAnnotation.buildAnnotations;

abstract class JavaMemberBuilder<OUTPUT, SELF extends JavaMemberBuilder<OUTPUT, SELF>> implements BuilderWithBuildParameter<JavaClass, OUTPUT> {
    private String name;
    private String descriptor;
    private Set<JavaAnnotationBuilder> annotations;
    private Set<JavaModifier> modifiers;
    private JavaClass owner;
    private ImportedClasses.ByTypeName importedClasses;

    SELF withName(String name) {
        this.name = name;
        return self();
    }

    String getName() {
        return name;
    }

    SELF withDescriptor(String descriptor) {
        this.descriptor = descriptor;
        return self();
    }

    SELF withAnnotations(Set<JavaAnnotationBuilder> annotations) {
        this.annotations = annotations;
        return self();
    }

    SELF withModifiers(Set<JavaModifier> modifiers) {
        this.modifiers = modifiers;
        return self();
    }

    @SuppressWarnings("unchecked")
    SELF self() {
        return (SELF) this;
    }

    JavaClass get(String typeName) {
        return importedClasses.get(typeName);
    }

    abstract OUTPUT construct(SELF self, ImportedClasses.ByTypeName importedClasses);

    Supplier<Map<String, JavaAnnotation>> getAnnotations() {
        return Suppliers.memoize(new Supplier<Map<String, JavaAnnotation>>() {
            @Override
            public Map<String, JavaAnnotation> get() {
                return buildAnnotations(annotations, importedClasses);
            }
        });
    }

    public String getDescriptor() {
        return descriptor;
    }

    public Set<JavaModifier> getModifiers() {
        return modifiers;
    }

    public JavaClass getOwner() {
        return owner;
    }

    @Override
    public final OUTPUT build(JavaClass owner, ImportedClasses.ByTypeName importedClasses) {
        this.owner = owner;
        this.importedClasses = importedClasses;
        return construct(self(), importedClasses);
    }
}
