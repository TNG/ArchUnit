package com.tngtech.archunit.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.properties.CanBeAnnotated;
import com.tngtech.archunit.core.properties.HasAnnotations;
import com.tngtech.archunit.core.properties.HasDescriptor;
import com.tngtech.archunit.core.properties.HasModifiers;
import com.tngtech.archunit.core.properties.HasName;
import com.tngtech.archunit.core.properties.HasOwner;
import com.tngtech.archunit.core.properties.HasOwner.Functions.Get;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.base.DescribedPredicate.equalTo;
import static com.tngtech.archunit.core.properties.CanBeAnnotated.Utils.toAnnotationOfType;
import static com.tngtech.archunit.core.properties.HasName.Functions.GET_NAME;

public abstract class JavaMember implements
        HasName.AndFullName, HasDescriptor, HasAnnotations, HasModifiers, HasOwner<JavaClass> {
    private final String name;
    private final String descriptor;
    private final Supplier<Map<String, JavaAnnotation>> annotations;
    private final JavaClass owner;
    private final Set<JavaModifier> modifiers;

    JavaMember(JavaMemberBuilder<?, ?> builder) {
        this.name = checkNotNull(builder.getName());
        this.descriptor = checkNotNull(builder.getDescriptor());
        this.annotations = builder.getAnnotations();
        this.owner = checkNotNull(builder.getOwner());
        this.modifiers = checkNotNull(builder.getModifiers());
    }

    @Override
    public Set<JavaAnnotation> getAnnotations() {
        return ImmutableSet.copyOf(annotations.get().values());
    }

    /**
     * Returns the {@link Annotation} of this member of the given {@link Annotation} type.
     *
     * @throws IllegalArgumentException if there is no annotation of the respective reflection type
     */
    @Override
    public <A extends Annotation> A getAnnotationOfType(Class<A> type) {
        return getAnnotationOfType(type.getName()).as(type);
    }

    @Override
    public JavaAnnotation getAnnotationOfType(String typeName) {
        return tryGetAnnotationOfType(typeName).getOrThrow(new IllegalArgumentException(String.format(
                "Member %s is not annotated with @%s",
                getFullName(), Formatters.ensureSimpleName(typeName))));
    }

    @Override
    public <A extends Annotation> Optional<A> tryGetAnnotationOfType(Class<A> type) {
        return tryGetAnnotationOfType(type.getName()).transform(toAnnotationOfType(type));
    }

    @Override
    public Optional<JavaAnnotation> tryGetAnnotationOfType(String typeName) {
        return Optional.fromNullable(annotations.get().get(typeName));
    }

    @Override
    public boolean isAnnotatedWith(Class<? extends Annotation> type) {
        return isAnnotatedWith(type.getName());
    }

    @Override
    public boolean isAnnotatedWith(String typeName) {
        return annotations.get().containsKey(typeName);
    }

    @Override
    public boolean isAnnotatedWith(DescribedPredicate<? super JavaAnnotation> predicate) {
        return CanBeAnnotated.Utils.isAnnotatedWith(annotations.get().values(), predicate);
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

    public static class Predicates {
        public static DescribedPredicate<JavaMember> declaredIn(Class<?> clazz) {
            return declaredIn(clazz.getName());
        }

        public static DescribedPredicate<JavaMember> declaredIn(String className) {
            return declaredIn(GET_NAME.is(equalTo(className)).as(className));
        }

        public static DescribedPredicate<JavaMember> declaredIn(DescribedPredicate<? super JavaClass> predicate) {
            return Get.<JavaClass>owner().is(predicate)
                    .as("declared in %s", predicate.getDescription())
                    .forSubType();
        }
    }

}
