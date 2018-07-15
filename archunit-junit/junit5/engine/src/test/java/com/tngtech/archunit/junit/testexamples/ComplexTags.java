package com.tngtech.archunit.junit.testexamples;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchRules;
import com.tngtech.archunit.junit.ArchTag;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@ArchTag("library-tag")
@AnalyzeClasses
public class ComplexTags {
    public static final String FIELD_RULE_NAME = "field_rule";
    public static final String METHOD_RULE_NAME = "method_rule";

    @ArchTag("rules-tag")
    @ArchTest
    static final ArchRules classWithTags = ArchRules.in(TestClassWithTags.class);

    @ArchTag("field-tag")
    @ArchTest
    static final ArchRule field_rule = RuleThatFails.on(UnwantedClass.class);

    @ArchTag("method-tag")
    @ArchTest
    static void method_rule(JavaClasses classes) {
    }
}
