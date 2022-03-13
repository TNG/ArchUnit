package com.tngtech.archunit.testutil.assertion;

import com.google.common.base.Joiner;
import com.tngtech.archunit.core.domain.JavaEnumConstant;
import org.assertj.core.api.AbstractObjectAssert;

import static com.google.common.base.Strings.emptyToNull;
import static org.assertj.core.api.Assertions.assertThat;

public class JavaEnumConstantAssertion extends AbstractObjectAssert<JavaEnumConstantAssertion, JavaEnumConstant> {
    public JavaEnumConstantAssertion(JavaEnumConstant enumConstant) {
        super(enumConstant, JavaEnumConstantAssertion.class);
    }

    public void isEquivalentTo(Enum<?> enumConstant) {
        assertThat(actual).as(describePartialAssertion()).isNotNull();
        assertThat(actual.getDeclaringClass().getName()).as(describePartialAssertion("type")).isEqualTo(enumConstant.getDeclaringClass().getName());
        assertThat(actual.name()).as(describePartialAssertion("name")).isEqualTo(enumConstant.name());
    }

    private String describePartialAssertion() {
        return describePartialAssertion("");
    }

    private String describePartialAssertion(String partialAssertionDescription) {
        return Joiner.on(": ").skipNulls().join(emptyToNull(descriptionText()), emptyToNull(partialAssertionDescription));
    }
}
