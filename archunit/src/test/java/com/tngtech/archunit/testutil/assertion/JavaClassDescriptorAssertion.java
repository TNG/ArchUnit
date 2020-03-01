package com.tngtech.archunit.testutil.assertion;

import com.tngtech.archunit.core.domain.JavaClassDescriptor;
import org.assertj.core.api.AbstractObjectAssert;

import static com.tngtech.archunit.testutil.assertion.JavaTypeAssertion.getExpectedPackageName;
import static org.assertj.core.api.Assertions.assertThat;

public class JavaClassDescriptorAssertion extends AbstractObjectAssert<JavaClassDescriptorAssertion, JavaClassDescriptor> {
    public JavaClassDescriptorAssertion(JavaClassDescriptor actual) {
        super(actual, JavaClassDescriptorAssertion.class);
    }

    public void isEquivalentTo(Class<?> clazz) {
        assertThat(actual.getFullyQualifiedClassName()).as("name").isEqualTo(clazz.getName());
        assertThat(actual.getSimpleClassName()).as("simple name").isEqualTo(clazz.getSimpleName());
        String expectedPackageName = getExpectedPackageName(clazz);
        assertThat(actual.getPackageName()).as("package").isEqualTo(expectedPackageName);
    }
}
