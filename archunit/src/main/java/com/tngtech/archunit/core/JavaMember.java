package com.tngtech.archunit.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class JavaMember<M extends Member, T extends MemberDescription<M>>
        implements HasName.AndFullName, HasOwner.IsOwnedByClass, HasDescriptor {
    final T memberDescription;
    private final Set<JavaAnnotation<?>> annotations;
    private final JavaClass owner;
    private final Set<JavaModifier> modifiers;
    private int hashCode;

    JavaMember(T memberDescription, JavaClass owner) {
        this.memberDescription = checkNotNull(memberDescription);
        annotations = convert(memberDescription.getAnnotations());
        this.owner = checkNotNull(owner);
        modifiers = JavaModifier.getModifiersFor(memberDescription.getModifiers());

        memberDescription.checkCompatibility(owner);
        hashCode = Objects.hash(memberDescription);
    }

    public static FluentPredicate<AnnotatedElement> withAnnotation(final Class<? extends Annotation> annotationType) {
        return new FluentPredicate<AnnotatedElement>() {
            @Override
            public boolean apply(AnnotatedElement input) {
                return input.getAnnotation(annotationType) != null;
            }
        };
    }

    private Set<JavaAnnotation<?>> convert(Annotation[] reflectionAnnotations) {
        Set<JavaAnnotation<?>> result = new HashSet<>();
        for (Annotation annotation : reflectionAnnotations) {
            result.add(new JavaAnnotation.Builder().withAnnotation(annotation).build(this));
        }
        return result;
    }

    public Set<JavaAnnotation<?>> getAnnotations() {
        return annotations;
    }

    /**
     * Returns the reflection value (compare {@link java.lang.annotation.Annotation}) of the respective
     * {@link JavaAnnotation} of this field.
     *
     * @throws IllegalArgumentException if there is no annotation of the respective reflection type
     */
    public <A extends Annotation> A getReflectionAnnotationOfType(Class<A> type) {
        return tryGetAnnotationOfType(type).get().reflect();
    }

    public <A extends Annotation> Optional<A> tryGetReflectionAnnotationOfType(Class<A> type) {
        return tryGetAnnotationOfType(type).transform(new Function<JavaAnnotation<A>, A>() {
            @Override
            public A apply(JavaAnnotation<A> input) {
                return input.reflect();
            }
        });
    }

    /**
     * Returns the {@link JavaAnnotation} for the given reflection type
     * (compare {@link java.lang.annotation.Annotation}) of this field.
     *
     * @throws IllegalArgumentException if there is no annotation of the respective reflection type
     */
    public <A extends Annotation> JavaAnnotation<A> getAnnotationOfType(Class<A> type) {
        return tryGetAnnotationOfType(type).get();
    }

    public boolean isAnnotationPresent(Class<? extends Annotation> type) {
        return tryGetAnnotationOfType(type).isPresent();
    }

    @SuppressWarnings("unchecked") // Type parameter always matches the type of the reflection annotation inside
    public <A extends Annotation> Optional<JavaAnnotation<A>> tryGetAnnotationOfType(Class<A> type) {
        for (JavaAnnotation<?> annotation : annotations) {
            if (type == annotation.getType()) {
                return Optional.of((JavaAnnotation<A>) annotation);
            }
        }
        return Optional.absent();
    }

    @Override
    public JavaClass getOwner() {
        return owner;
    }

    public Set<JavaModifier> getModifiers() {
        return modifiers;
    }

    @Override
    public String getName() {
        return memberDescription.getName();
    }

    @Override
    public String getDescriptor() {
        return memberDescription.getDescriptor();
    }

    public abstract Set<? extends JavaAccess<?>> getAccessesToSelf();

    public M reflect() {
        return memberDescription.reflect();
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final JavaMember<?, ?> other = (JavaMember<?, ?>) obj;
        return Objects.equals(this.memberDescription, other.memberDescription);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{member=" + memberDescription + ", owner=" + getOwner() + '}';
    }

    static abstract class Builder<RAW extends MemberDescription<?>, OUTPUT> implements BuilderWithBuildParameter<JavaClass, OUTPUT> {
        RAW member;
        JavaClass owner;

        Builder<RAW, OUTPUT> withMember(RAW member) {
            this.member = member;
            return this;
        }
    }
}
