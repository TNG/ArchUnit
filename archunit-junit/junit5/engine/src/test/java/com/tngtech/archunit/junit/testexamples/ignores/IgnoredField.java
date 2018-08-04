package com.tngtech.archunit.junit.testexamples.ignores;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchIgnore;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.testexamples.RuleThatFails;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.junit.testexamples.UnwantedClass.CLASS_VIOLATING_RULES;

@AnalyzeClasses(packages = "some.dummy.package")
public class IgnoredField {

    @ArchTest
    static final ArchRule unignored_rule = RuleThatFails.on(CLASS_VIOLATING_RULES);

    @ArchTest
    @ArchIgnore(reason = "some example description")
    static final ArchRule ignored_rule = RuleThatFails.on(CLASS_VIOLATING_RULES);

    public static final String UNIGNORED_RULE_FIELD = "unignored_rule";
    public static final String IGNORED_RULE_FIELD = "ignored_rule";
}
