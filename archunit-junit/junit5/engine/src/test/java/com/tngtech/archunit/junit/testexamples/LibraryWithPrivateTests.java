package com.tngtech.archunit.junit.testexamples;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;

@AnalyzeClasses(packages = "some.dummy.package")
public class LibraryWithPrivateTests {
    @ArchTest
    private final ArchTests privateRulesField = ArchTests.in(SubRules.class);

    public static final String PRIVATE_RULES_FIELD_NAME = "privateRulesField";

    public static class SubRules {
        @ArchTest
        private final ArchTests privateRulesField = ArchTests.in(ClassWithPrivateTests.class);

        public static final String PRIVATE_RULES_FIELD_NAME = "privateRulesField";
    }
}
