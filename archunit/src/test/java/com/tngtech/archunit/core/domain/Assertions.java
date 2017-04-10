package com.tngtech.archunit.core.domain;

import org.assertj.core.api.AbstractObjectAssert;

public class Assertions extends com.tngtech.archunit.testutil.Assertions {
    public static JavaTypeAssertion assertThat(JavaType javaType) {
        return new JavaTypeAssertion(javaType);
    }

    public static class JavaTypeAssertion extends AbstractObjectAssert<JavaTypeAssertion, JavaType> {
        private JavaTypeAssertion(JavaType actual) {
            super(actual, JavaTypeAssertion.class);
        }

        public void isEquivalentTo(Class<?> clazz) {
            assertThat(actual.getName()).as("name").isEqualTo(clazz.getName());
            assertThat(actual.getSimpleName()).as("simple name").isEqualTo(clazz.getSimpleName());
            assertThat(actual.getPackage()).as("package").isEqualTo(clazz.getPackage() != null ? clazz.getPackage().getName() : "");
        }
    }
}
