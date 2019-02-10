package com.tngtech.archunit.testutil.assertion;

import java.lang.reflect.Method;

import com.tngtech.archunit.core.domain.JavaMethod;
import org.assertj.core.api.AbstractObjectAssert;

import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.assertion.JavaMembersAssertion.assertEquivalent;
import static com.tngtech.archunit.testutil.assertion.JavaMemberAssertion.getExpectedNameOf;

public class JavaMethodAssertion extends AbstractObjectAssert<JavaMethodAssertion, JavaMethod> {
    public JavaMethodAssertion(JavaMethod javaMethod) {
        super(javaMethod, JavaMethodAssertion.class);
    }

    public JavaMethodAssertion isEquivalentTo(Method method) {
        assertEquivalent(actual, method);
        assertThat(actual.getFullName()).isEqualTo(getExpectedNameOf(method, method.getName()));
        assertThat(actual.getName()).isEqualTo(method.getName());
        assertThat(actual.getParameters()).matches(method.getParameterTypes());
        assertThat(actual.getReturnType()).matches(method.getReturnType());
        return this;
    }

    public JavaMethodAssertion isEquivalentTo(Class<?> owner, String methodName, Class<?>... parameterTypes) {
        assertThat(actual.getOwner()).matches(owner);
        assertThat(actual.getName()).isEqualTo(methodName);
        assertThat(actual.getParameters()).matches(parameterTypes);
        return this;
    }
}
