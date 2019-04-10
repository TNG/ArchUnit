package com.tngtech.archunit.testutil.assertion;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import org.assertj.core.api.AbstractObjectAssert;

import static org.assertj.core.api.Assertions.assertThat;

public class ArchRuleAssertion extends AbstractObjectAssert<ArchRuleAssertion, ArchRule> {
    public ArchRuleAssertion(ArchRule rule) {
        super(rule, ArchRuleAssertion.class);
    }

    public ArchRuleAssertion hasDescriptionContaining(String descriptionPart) {
        assertThat(actual.getDescription()).contains(descriptionPart);
        return this;
    }

    public ArchRuleCheckAssertion checking(JavaClasses classes) {
        return new ArchRuleCheckAssertion(actual, classes);
    }
}
