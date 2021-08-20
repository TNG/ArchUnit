package com.tngtech.archunit.testutil.assertion;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaTypeVariable;
import org.assertj.core.api.AbstractObjectAssert;

import static com.tngtech.archunit.core.domain.properties.HasName.Utils.namesOf;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatType;
import static com.tngtech.archunit.testutil.Assertions.assertThatTypeVariable;
import static com.tngtech.archunit.testutil.Assertions.assertThatTypes;
import static com.tngtech.archunit.testutil.ReflectionTestUtils.constructor;
import static com.tngtech.archunit.testutil.ReflectionTestUtils.method;
import static com.tngtech.archunit.testutil.assertion.JavaTypeVariableAssertion.getTypeVariableWithName;

public class JavaCodeUnitAssertion<T extends JavaCodeUnit, SELF extends JavaCodeUnitAssertion<T, SELF>>
        extends JavaMemberAssertion<T, SELF> {

    public JavaCodeUnitAssertion(T javaMember, Class<SELF> selfType) {
        super(javaMember, selfType);
    }

    public void isEquivalentTo(Method method) {
        super.isEquivalentTo(method);
        assertThatTypes(actual.getRawParameterTypes()).matchExactly(method.getParameterTypes());
        assertThatType(actual.getRawReturnType()).matches(method.getReturnType());
    }

    public void isEquivalentTo(Constructor<?> constructor) {
        super.isEquivalentTo(constructor);
        assertThatTypes(actual.getRawParameterTypes()).matchExactly(constructor.getParameterTypes());
        assertThatType(actual.getRawReturnType()).matches(void.class);
    }

    public JavaTypeVariableOfCodeUnitAssertion hasTypeParameter(String name) {
        JavaTypeVariable<JavaCodeUnit> typeVariable = getTypeVariableWithName(name, actual.getTypeParameters());
        return new JavaTypeVariableOfCodeUnitAssertion(typeVariable);
    }

    public JavaCodeUnitAssertion<T, SELF> hasTypeParameters(String... names) {
        assertThat(namesOf(actual.getTypeParameters())).as("names of type parameters").containsExactly(names);
        return this;
    }

    public JavaTypeVariableOfCodeUnitAssertion hasOnlyTypeParameter(String name) {
        assertThat(actual.getTypeParameters()).as("Type parameters").hasSize(1);
        return hasTypeParameter(name);
    }

    public void matchesConstructor(Class<?> owner, Class<?>... parameterTypes) {
        isEquivalentTo(constructor(owner, parameterTypes));
    }

    public void matchesMethod(Class<?> owner, String methodName, Class<?>... parameterTypes) {
        isEquivalentTo(method(owner, methodName, parameterTypes));
    }

    public class JavaTypeVariableOfCodeUnitAssertion extends AbstractObjectAssert<JavaTypeVariableOfCodeUnitAssertion, JavaTypeVariable<JavaCodeUnit>> {
        private JavaTypeVariableOfCodeUnitAssertion(JavaTypeVariable<JavaCodeUnit> actual) {
            super(actual, JavaTypeVariableOfCodeUnitAssertion.class);
        }

        public JavaCodeUnitAssertion<T, SELF> withBoundsMatching(Class<?>... bounds) {
            assertThatTypeVariable(actual).hasBoundsMatching(bounds);
            return JavaCodeUnitAssertion.this;
        }

        public JavaCodeUnitAssertion<T, SELF> withBoundsMatching(ExpectedConcreteType... bounds) {
            assertThatTypeVariable(actual).hasBoundsMatching(bounds);
            return JavaCodeUnitAssertion.this;
        }
    }
}
