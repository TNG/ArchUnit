package com.tngtech.archunit.testutil.assertion;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import com.tngtech.archunit.core.domain.JavaMember;
import org.assertj.core.api.Assertions;

import static com.tngtech.archunit.core.domain.Formatters.formatMethodParameterTypeNames;
import static com.tngtech.archunit.core.domain.JavaClass.namesOf;
import static com.tngtech.archunit.core.domain.JavaModifier.ABSTRACT;
import static com.tngtech.archunit.core.domain.JavaModifier.FINAL;
import static com.tngtech.archunit.core.domain.JavaModifier.NATIVE;
import static com.tngtech.archunit.core.domain.JavaModifier.PRIVATE;
import static com.tngtech.archunit.core.domain.JavaModifier.PROTECTED;
import static com.tngtech.archunit.core.domain.JavaModifier.PUBLIC;
import static com.tngtech.archunit.core.domain.JavaModifier.STATIC;
import static com.tngtech.archunit.core.domain.JavaModifier.SYNCHRONIZED;
import static com.tngtech.archunit.core.domain.JavaModifier.TRANSIENT;
import static com.tngtech.archunit.core.domain.JavaModifier.VOLATILE;

public class JavaMembersAssertion {
    public static <T extends Member & AnnotatedElement> void assertEquivalent(JavaMember javaMember, T member) {
        Assertions.assertThat(javaMember.getOwner().reflect()).isEqualTo(member.getDeclaringClass());
        assertModifiersMatch(javaMember, member);
        Assertions.assertThat(JavaAnnotationAssertion.propertiesOf(javaMember.getAnnotations()))
                .isEqualTo(JavaAnnotationAssertion.propertiesOf(member.getAnnotations()));
    }

    public static <T extends Member> void assertModifiersMatch(JavaMember javaMember, T member) {
        Assertions.assertThat(javaMember.getModifiers().contains(ABSTRACT))
                .as("member is abstract")
                .isEqualTo(Modifier.isAbstract(member.getModifiers()));
        Assertions.assertThat(javaMember.getModifiers().contains(FINAL))
                .as("member is final")
                .isEqualTo(Modifier.isFinal(member.getModifiers()));
        Assertions.assertThat(javaMember.getModifiers().contains(NATIVE))
                .as("member is native")
                .isEqualTo(Modifier.isNative(member.getModifiers()));
        Assertions.assertThat(javaMember.getModifiers().contains(PRIVATE))
                .as("member is private")
                .isEqualTo(Modifier.isPrivate(member.getModifiers()));
        Assertions.assertThat(javaMember.getModifiers().contains(PROTECTED))
                .as("member is protected")
                .isEqualTo(Modifier.isProtected(member.getModifiers()));
        Assertions.assertThat(javaMember.getModifiers().contains(PUBLIC))
                .as("member is public")
                .isEqualTo(Modifier.isPublic(member.getModifiers()));
        Assertions.assertThat(javaMember.getModifiers().contains(STATIC))
                .as("member is static")
                .isEqualTo(Modifier.isStatic(member.getModifiers()));
        Assertions.assertThat(javaMember.getModifiers().contains(SYNCHRONIZED))
                .as("member is synchronized")
                .isEqualTo(Modifier.isSynchronized(member.getModifiers()));
        Assertions.assertThat(javaMember.getModifiers().contains(TRANSIENT))
                .as("member is transient")
                .isEqualTo(Modifier.isTransient(member.getModifiers()));
        Assertions.assertThat(javaMember.getModifiers().contains(VOLATILE))
                .as("member is volatile")
                .isEqualTo(Modifier.isVolatile(member.getModifiers()));
    }

    public static <T extends Member & AnnotatedElement> String getExpectedNameOf(T member, String name) {
        String base = member.getDeclaringClass().getName() + "." + name;
        if (member instanceof Method) {
            return base + expectedParametersOf(((Method) member).getParameterTypes());
        }
        if (member instanceof Constructor<?>) {
            return base + expectedParametersOf(((Constructor<?>) member).getParameterTypes());
        }
        return base;
    }

    private static String expectedParametersOf(Class<?>[] parameterTypes) {
        return String.format("(%s)", formatMethodParameterTypeNames(namesOf(parameterTypes)));
    }
}
