package com.tngtech.archunit.testutil.assertion;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import com.tngtech.archunit.core.domain.JavaMember;
import org.assertj.core.api.AbstractObjectAssert;

import static com.tngtech.archunit.core.domain.Formatters.formatMethodParameterTypeNames;
import static com.tngtech.archunit.core.domain.JavaClass.namesOf;
import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.testutil.assertion.JavaMembersAssertion.assertEquivalent;
import static org.assertj.core.api.Assertions.assertThat;

public class JavaMemberAssertion<T extends JavaMember, SELF extends JavaMemberAssertion<T, SELF>>
        extends AbstractObjectAssert<SELF, T> {
    public JavaMemberAssertion(T javaMember, Class<SELF> selfType) {
        super(javaMember, selfType);
    }

    public <M extends Member & AnnotatedElement> void isEquivalentTo(M member) {
        assertEquivalent(actual, member);
        String expectedName = getExpectedNameOf(member);
        assertThat(actual.getName()).isEqualTo(expectedName);
        assertThat(actual.getFullName()).isEqualTo(getExpectedFullNameOf(member));
    }

    static String getExpectedNameOf(Member member) {
        return member instanceof Constructor ? CONSTRUCTOR_NAME : member.getName();
    }

    static <T extends Member & AnnotatedElement> String getExpectedFullNameOf(T member) {
        String base = member.getDeclaringClass().getName() + "." + getExpectedNameOf(member);
        if (member instanceof Method) {
            return base + expectedParametersOf(((Method) member).getParameterTypes());
        }
        if (member instanceof Constructor<?>) {
            return base + expectedParametersOf(((Constructor<?>) member).getParameterTypes());
        }
        return base;
    }

    static <T extends Member & AnnotatedElement> String getExpectedNameOf(T member, String name) {
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
