package com.tngtech.archunit.testutil.assertion;

import com.tngtech.archunit.core.domain.JavaField;
import org.assertj.core.api.AbstractIterableAssert;

public class JavaFieldsAssertion
        extends AbstractIterableAssert<JavaFieldsAssertion, Iterable<? extends JavaField>, JavaField, JavaFieldAssertion> {

    public JavaFieldsAssertion(Iterable<JavaField> methods) {
        super(methods, JavaFieldsAssertion.class);
    }

    @Override
    protected JavaFieldAssertion toAssert(JavaField value, String description) {
        return new JavaFieldAssertion(value).as(description);
    }

    public JavaFieldsAssertion contain(Class<?> owner, String fieldName) {
        for (JavaField field : actual) {
            if (field.getOwner().isEquivalentTo(owner) && field.getName().equals(fieldName)) {
                return this;
            }
        }
        throw new AssertionError(String.format("There is no field %s.%s contained in %s",
                owner.getName(), fieldName, actual));
    }
}
