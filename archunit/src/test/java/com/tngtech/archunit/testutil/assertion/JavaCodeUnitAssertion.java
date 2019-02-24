package com.tngtech.archunit.testutil.assertion;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import com.tngtech.archunit.core.domain.JavaCodeUnit;

import static com.tngtech.archunit.testutil.Assertions.assertThat;

public class JavaCodeUnitAssertion<T extends JavaCodeUnit, SELF extends JavaCodeUnitAssertion<T, SELF>>
        extends JavaMemberAssertion<T, SELF> {

    public JavaCodeUnitAssertion(T javaMember, Class<SELF> selfType) {
        super(javaMember, selfType);
    }

    public void isEquivalentTo(Method method) {
        super.isEquivalentTo(method);
        assertThat(actual.getParameters()).matches(method.getParameterTypes());
        assertThat(actual.getRawParameterTypes()).matches(method.getParameterTypes());
        assertThat(actual.getReturnType()).matches(method.getReturnType());
        assertThat(actual.getRawReturnType()).matches(method.getReturnType());
    }

    public void isEquivalentTo(Constructor<?> constructor) {
        super.isEquivalentTo(constructor);
        assertThat(actual.getParameters()).matches(constructor.getParameterTypes());
        assertThat(actual.getRawParameterTypes()).matches(constructor.getParameterTypes());
        assertThat(actual.getReturnType()).matches(void.class);
        assertThat(actual.getRawReturnType()).matches(void.class);
    }
}
