package com.tngtech.archunit.testutil.assertion;

import java.lang.reflect.Field;

import com.tngtech.archunit.core.domain.JavaField;

import static com.tngtech.archunit.testutil.Assertions.assertThatType;

public class JavaFieldAssertion extends JavaMemberAssertion<JavaField, JavaFieldAssertion> {
    public JavaFieldAssertion(JavaField javaField) {
        super(javaField, JavaFieldAssertion.class);
    }

    public void isEquivalentTo(Field field) {
        super.isEquivalentTo(field);
        assertThatType(actual.getRawType()).matches(field.getType());
    }
}
