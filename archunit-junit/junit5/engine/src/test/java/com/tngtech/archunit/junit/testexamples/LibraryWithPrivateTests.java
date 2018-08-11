package com.tngtech.archunit.junit.testexamples;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchRules;
import com.tngtech.archunit.junit.ArchTest;

@AnalyzeClasses(packages = "some.dummy.package")
public class LibraryWithPrivateTests {
    @ArchTest
    private final ArchRules privateRulesField = ArchRules.in(SubRules.class);

    public static final String PRIVATE_RULES_FIELD_NAME = "privateRulesField";

    public static class SubRules {
        @ArchTest
        private final ArchRules privateRulesField = ArchRules.in(ClassWithPrivateTests.class);

        public static final String PRIVATE_RULES_FIELD_NAME = "privateRulesField";
    }
}
