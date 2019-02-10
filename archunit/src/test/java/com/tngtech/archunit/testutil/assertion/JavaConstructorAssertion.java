package com.tngtech.archunit.testutil.assertion;

import java.lang.reflect.Constructor;

import com.tngtech.archunit.core.domain.JavaConstructor;
import org.assertj.core.api.AbstractObjectAssert;

import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.assertion.JavaMemberAssertion.getExpectedNameOf;

public class JavaConstructorAssertion extends AbstractObjectAssert<JavaConstructorAssertion, JavaConstructor> {
    public JavaConstructorAssertion(JavaConstructor javaConstructor) {
        super(javaConstructor, JavaConstructorAssertion.class);
    }

    public void isEquivalentTo(Constructor<?> constructor) {
        JavaMembersAssertion.assertEquivalent(actual, constructor);
        assertThat(actual.getName()).isEqualTo(CONSTRUCTOR_NAME);
        assertThat(actual.getFullName()).isEqualTo(getExpectedNameOf(constructor, CONSTRUCTOR_NAME));
        assertThat(actual.getParameters()).matches(constructor.getParameterTypes());
        assertThat(actual.getReturnType()).matches(void.class);
    }
}
