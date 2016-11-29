package com.tngtech.archunit.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.objectweb.asm.Type;

import static com.google.common.base.Preconditions.checkArgument;

public interface MemberDescription<T extends Member> {
    String getName();

    int getModifiers();

    Set<JavaAnnotation> getAnnotationsFor(JavaMember<?, ?> owner);

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
        public Set<JavaAnnotation> getAnnotationsFor(JavaMember<?, ?> owner) {
            return convert(owner, member.getAnnotations());
        }

        private static Set<JavaAnnotation> convert(JavaMember<?, ?> owner, Annotation[] reflectionAnnotations) {
            ImmutableSet.Builder<JavaAnnotation> result = ImmutableSet.builder();
            for (Annotation annotation : reflectionAnnotations) {
                result.add(new JavaAnnotation.Builder()
                        .withType(TypeDetails.of(annotation.annotationType()))
                        .withValues(mapOf(annotation))
                        .build(owner));
            }
            return result.build();
        }

        private static Map<String, Object> mapOf(Annotation annotation) {
            ImmutableMap.Builder<String, Object> result = ImmutableMap.builder();
            for (Method method : annotation.annotationType().getDeclaredMethods()) {
                result.put(method.getName(), get(annotation, method.getName()));
            }
            return result.build();
        }

        private static Object get(Annotation annotation, String methodName) {
            try {
                Object result = annotation.annotationType().getMethod(methodName).invoke(annotation);
                if (result instanceof Class) {
                    return TypeDetails.of((Class<?>) result);
                }
                if (result instanceof Class[]) {
                    return TypeDetails.allOf((Class<?>[]) result);
                }
                if (result instanceof Enum<?>) {
                    return enumConstant((Enum) result);
                }
                if (result instanceof Enum[]) {
                    return enumConstants((Enum[]) result);
                }
                return result;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private static List<JavaEnumConstant> enumConstants(Enum[] enums) {
            ImmutableList.Builder<JavaEnumConstant> result = ImmutableList.builder();
            for (Enum e : enums) {
                result.add(enumConstant(e));
            }
            return result.build();
        }

        private static JavaEnumConstant enumConstant(Enum result) {
            return new JavaEnumConstant(TypeDetails.of(result.getDeclaringClass()), result.name());
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

        public List<TypeDetails> getParameterTypes() {
            return TypeDetails.allOf(member.getParameterTypes());
        }

        @Override
        public String getName() {
            return JavaConstructor.CONSTRUCTOR_NAME;
        }

        @Override
        public String getDescriptor() {
            return Type.getConstructorDescriptor(member);
        }
    }

    interface ForMethod extends MemberDescription<Method> {
        List<TypeDetails> getParameterTypes();

        TypeDetails getReturnType();
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
        public List<TypeDetails> getParameterTypes() {
            return TypeDetails.allOf(member.getParameterTypes());
        }

        @Override
        public TypeDetails getReturnType() {
            return TypeDetails.of(member.getReturnType());
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
