package com.tngtech.archunit.testutil.assertion;

import com.tngtech.archunit.core.domain.AccessTarget.FieldAccessTarget;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import org.assertj.core.api.Condition;

import static com.tngtech.archunit.core.domain.TestUtils.targetFrom;
import static org.assertj.core.api.Assertions.assertThat;

public class AccessToFieldAssertion extends BaseAccessAssertion<AccessToFieldAssertion, JavaFieldAccess, FieldAccessTarget> {
    public AccessToFieldAssertion(JavaFieldAccess access) {
        super(access);
    }

    @Override
    protected AccessToFieldAssertion newAssertion(JavaFieldAccess access) {
        return new AccessToFieldAssertion(access);
    }

    public AccessToFieldAssertion isTo(final String name) {
        return isTo(new Condition<FieldAccessTarget>("field with name '" + name + "'") {
            @Override
            public boolean matches(FieldAccessTarget fieldAccessTarget) {
                return fieldAccessTarget.getName().equals(name);
            }
        });
    }

    public AccessToFieldAssertion isTo(final Class<?> owner, final String name) {
        return isTo(new Condition<FieldAccessTarget>("field " + owner.getName() + "." + name) {
            @Override
            public boolean matches(FieldAccessTarget fieldAccessTarget) {
                return fieldAccessTarget.getOwner().isEquivalentTo(owner) && fieldAccessTarget.getName().equals(name);
            }
        });
    }

    public AccessToFieldAssertion isTo(JavaField field) {
        return isTo(targetFrom(field));
    }

    public AccessToFieldAssertion isOfType(JavaFieldAccess.AccessType type) {
        assertThat(access.getAccessType()).isEqualTo(type);
        return newAssertion(access);
    }
}
