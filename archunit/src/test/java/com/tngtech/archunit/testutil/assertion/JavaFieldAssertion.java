package com.tngtech.archunit.testutil.assertion;

import java.lang.reflect.Field;

import com.tngtech.archunit.core.domain.JavaField;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;

public class JavaFieldAssertion extends AbstractObjectAssert<JavaFieldAssertion, JavaField> {
    public JavaFieldAssertion(JavaField javaField) {
        super(javaField, JavaFieldAssertion.class);
    }

    public void isEquivalentTo(Field field) {
        JavaMembersAssertion.assertEquivalent(actual, field);
        Assertions.assertThat(actual.getName()).isEqualTo(field.getName());
        Assertions.assertThat(actual.getFullName()).isEqualTo(JavaMembersAssertion.getExpectedNameOf(field, field.getName()));
        com.tngtech.archunit.testutil.Assertions.assertThat(actual.getType()).matches(field.getType());
    }
}
