package com.tngtech.archunit.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

import com.google.common.collect.ImmutableList;
import org.objectweb.asm.Type;

import static com.google.common.base.Preconditions.checkArgument;

public interface MemberDescription<T extends Member> {
    String getName();

    int getModifiers();

    Annotation[] getAnnotations();

    String getDescriptor();

    T reflect();

    void checkCompatibility(JavaClass owner);

    abstract class ForDeterminedMember<T extends AnnotatedElement & Member> implements MemberDescription<T> {
        final T member;
        private int hashCode;

        public ForDeterminedMember(T member) {
            this.member = member;
            hashCode = Objects.hash(member);
        }

        @Override
        public String getName() {
            return member.getName();
        }

        @Override
        public int getModifiers() {
            return member.getModifiers();
        }

        @Override
        public Annotation[] getAnnotations() {
            return member.getAnnotations();
        }

        @Override
        public T reflect() {
            return member;
        }

        @Override
        public void checkCompatibility(JavaClass owner) {
            checkArgument(member.getDeclaringClass().isAssignableFrom(owner.reflect()),
                    "Member <" + member.getName() + "> is declared in " + member.getDeclaringClass().getName() +
                            " but the reflection type of the owner <" + owner + "> is not assignable to this");
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
            final ForDeterminedMember<?> other = (ForDeterminedMember<?>) obj;
            return Objects.equals(this.member, other.member);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" + member + '}';
        }
    }

    class ForConstructor extends ForDeterminedMember<Constructor<?>> {
        public ForConstructor(Constructor<?> constructor) {
            super(constructor);
        }

        public List<? extends Class<?>> getParameterTypes() {
            return ImmutableList.copyOf(member.getParameterTypes());
        }

        @Override
        public String getDescriptor() {
            return Type.getConstructorDescriptor(member);
        }
    }

    interface ForMethod extends MemberDescription<Method> {
        List<Class<?>> getParameterTypes();

        Class<?> getReturnType();
    }

    class ForDeterminedMethod extends ForDeterminedMember<Method> implements ForMethod {
        public ForDeterminedMethod(Method method) {
            super(method);
        }

        @Override
        public String getDescriptor() {
            return Type.getMethodDescriptor(member);
        }

        @Override
        public List<Class<?>> getParameterTypes() {
            return ImmutableList.copyOf(member.getParameterTypes());
        }

        @Override
        public Class<?> getReturnType() {
            return member.getReturnType();
        }
    }

    interface ForField extends MemberDescription<Field> {
        Class<?> getType();
    }

    class ForDeterminedField extends ForDeterminedMember<Field> implements ForField {
        public ForDeterminedField(Field field) {
            super(field);
        }

        @Override
        public String getDescriptor() {
            return Type.getDescriptor(member.getType());
        }

        @Override
        public Class<?> getType() {
            return member.getType();
        }
    }
}
