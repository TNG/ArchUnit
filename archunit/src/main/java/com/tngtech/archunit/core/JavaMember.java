package com.tngtech.archunit.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.properties.HasAnnotations;
import com.tngtech.archunit.core.properties.HasDescriptor;
import com.tngtech.archunit.core.properties.HasModifiers;
import com.tngtech.archunit.core.properties.HasName;
import com.tngtech.archunit.core.properties.HasOwner;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.core.JavaAnnotation.buildAnnotations;

public abstract class JavaMember implements
        HasName.AndFullName, HasDescriptor, HasAnnotations, HasModifiers, HasOwner<JavaClass> {
    private final String name;
    private final String descriptor;
    private final Supplier<Map<String, JavaAnnotation>> annotations;
    private final JavaClass owner;
    private final Set<JavaModifier> modifiers;

    JavaMember(Builder<?, ?> builder) {
        this.name = checkNotNull(builder.name);
        this.descriptor = checkNotNull(builder.descriptor);
        this.annotations = builder.getAnnotations();
        this.owner = checkNotNull(builder.owner);
        this.modifiers = checkNotNull(builder.modifiers);
    }

    @Override
    public Set<JavaAnnotation> getAnnotations() {
        return ImmutableSet.copyOf(annotations.get().values());
    }

    /**
     * Returns the {@link JavaAnnotation} of this field for the given {@link java.lang.annotation.Annotation} type.
     *
     * @throws IllegalArgumentException if there is no annotation of the respective reflection type
     */
    @Override
    public JavaAnnotation getAnnotationOfType(Class<? extends Annotation> type) {
        return getAnnotationOfType(type.getName());
    }

    @Override
    public JavaAnnotation getAnnotationOfType(String typeName) {
        return tryGetAnnotationOfType(typeName).getOrThrow(new IllegalArgumentException(String.format(
                "Member %s is not annotated with @%s",
                getFullName(), Formatters.ensureSimpleName(typeName))));
    }

    @Override
    public Optional<JavaAnnotation> tryGetAnnotationOfType(Class<? extends Annotation> type) {
        return tryGetAnnotationOfType(type.getName());
    }

    @Override
    public Optional<JavaAnnotation> tryGetAnnotationOfType(String typeName) {
        return Optional.fromNullable(annotations.get().get(typeName));
    }

    @Override
    public boolean isAnnotatedWith(Class<? extends Annotation> type) {
        return annotations.get().containsKey(type.getName());
    }

    @Override
    public JavaClass getOwner() {
        return owner;
    }

    @Override
    public Set<JavaModifier> getModifiers() {
        return modifiers;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescriptor() {
        return descriptor;
    }

    public abstract Set<? extends JavaAccess<?>> getAccessesToSelf();

    @Override
    public String toString() {
        return getClass().getSimpleName() + '{' + getFullName() + '}';
    }


    /**
     * Resolves the respective {@link Member} from the classpath.<br/>
     * NOTE: This method will throw an exception, if the owning {@link Class} or any of its dependencies
     * can't be found on the classpath.
     *
     * @return The {@link Member} equivalent to this {@link JavaMember}
     */
    public abstract Member reflect();

    abstract static class Builder<OUTPUT, SELF extends Builder<OUTPUT, SELF>> implements BuilderWithBuildParameter<JavaClass, OUTPUT> {
        private String name;
        private String descriptor;
        private Set<JavaAnnotation.Builder> annotations;
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

        SELF withAnnotations(Set<JavaAnnotation.Builder> annotations) {
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

        @Override
        public final OUTPUT build(JavaClass owner, ImportedClasses.ByTypeName importedClasses) {
            this.owner = owner;
            this.importedClasses = importedClasses;
            return construct(self(), importedClasses);
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
    }
}
