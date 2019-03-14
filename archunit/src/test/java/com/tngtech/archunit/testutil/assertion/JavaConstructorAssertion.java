package com.tngtech.archunit.testutil.assertion;

import java.lang.reflect.Constructor;

import com.tngtech.archunit.core.domain.JavaConstructor;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;

import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;

public class JavaConstructorAssertion extends AbstractObjectAssert<JavaConstructorAssertion, JavaConstructor> {
    public JavaConstructorAssertion(JavaConstructor javaConstructor) {
        super(javaConstructor, JavaConstructorAssertion.class);
    }

    public void isEquivalentTo(Constructor<?> constructor) {
        JavaMembersAssertion.assertEquivalent(actual, constructor);
        Assertions.assertThat(actual.getName()).isEqualTo(CONSTRUCTOR_NAME);
        Assertions.assertThat(actual.getFullName()).isEqualTo(JavaMembersAssertion.getExpectedNameOf(constructor, CONSTRUCTOR_NAME));
        com.tngtech.archunit.testutil.Assertions.assertThat(actual.getParameters()).matches(constructor.getParameterTypes());
        com.tngtech.archunit.testutil.Assertions.assertThat(actual.getReturnType()).matches(void.class);
    }
}
