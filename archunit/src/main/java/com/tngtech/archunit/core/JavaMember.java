package com.tngtech.archunit.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class JavaMember<M extends Member, T extends MemberDescription<M>>
        implements HasName.AndFullName, HasOwner.IsOwnedByClass, HasDescriptor {
    final T memberDescription;
    private final Set<JavaAnnotation<?>> annotations;
    private final JavaClass owner;
    private final Set<JavaModifier> modifiers;
    private final int hashCode;

    JavaMember(T memberDescription, JavaClass owner) {
        this.memberDescription = checkNotNull(memberDescription);
        annotations = convert(memberDescription.getAnnotations());
        this.owner = checkNotNull(owner);
        modifiers = JavaModifier.getModifiersFor(memberDescription.getModifiers());

        memberDescription.checkCompatibility(owner);
        hashCode = Objects.hash(memberDescription);
    }

    private Set<JavaAnnotation<?>> convert(Annotation[] reflectionAnnotations) {
        ImmutableSet.Builder<JavaAnnotation<?>> result = ImmutableSet.builder();
        for (Annotation annotation : reflectionAnnotations) {
            result.add(new JavaAnnotation.Builder().withAnnotation(annotation).build(this));
        }
        return result.build();
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

    public static DescribedPredicate<JavaMember<?, ?>> modifier(final JavaModifier modifier) {
        return new DescribedPredicate<JavaMember<?, ?>>("modifier " + modifier) {
            @Override
            public boolean apply(JavaMember<?, ?> input) {
                return input.getModifiers().contains(modifier);
            }
        };
    }

    public static final ChainableFunction<JavaMember<?, ?>, JavaClass> GET_OWNER =
            new ChainableFunction<JavaMember<?, ?>, JavaClass>() {
                @Override
                public JavaClass apply(JavaMember<?, ?> input) {
                    return input.getOwner();
                }
            };

    static abstract class Builder<RAW extends MemberDescription<?>, OUTPUT> implements BuilderWithBuildParameter<JavaClass, OUTPUT> {
        RAW member;
        JavaClass owner;

        Builder<RAW, OUTPUT> withMember(RAW member) {
            this.member = member;
            return this;
        }
    }
}
