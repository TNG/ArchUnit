package com.tngtech.archunit.junit.testexamples;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchRules;
import com.tngtech.archunit.junit.ArchTag;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.testexamples.subone.SimpleRuleField;
import com.tngtech.archunit.lang.ArchRule;

@ArchTag("tag-one")
@ArchTag("tag-two")
@AnalyzeClasses
public class TestClassWithTags {
    public static final String FIELD_RULE_NAME = "rule_in_class_with_tags";

    @ArchTest
    public static final ArchRule rule_in_class_with_tags = RuleThatFails.on(UnwantedClass.class);

    @ArchTest
    public static final ArchRules rules_in_class_with_tags = ArchRules.in(SimpleRuleField.class);

    @ArchTest
    static void method_in_class_with_tags(JavaClasses classes) {
    }
}
