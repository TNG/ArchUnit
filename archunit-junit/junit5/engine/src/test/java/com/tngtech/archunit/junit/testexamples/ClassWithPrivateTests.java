package com.tngtech.archunit.junit.testexamples;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "some.dummy.package")
public class ClassWithPrivateTests {
    @ArchTest
    private final ArchRule privateRuleField = RuleThatFails.on(UnwantedClass.CLASS_VIOLATING_RULES);

    @ArchTest
    private void privateRuleMethod(JavaClasses classes) {
        RuleThatFails.on(UnwantedClass.CLASS_VIOLATING_RULES).check(classes);
    }

    public static final String PRIVATE_RULE_FIELD_NAME = "privateRuleField";
    public static final String PRIVATE_RULE_METHOD_NAME = "privateRuleMethod";
}
