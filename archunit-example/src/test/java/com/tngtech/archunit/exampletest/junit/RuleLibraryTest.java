package com.tngtech.archunit.exampletest.junit;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchRules;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import org.junit.runner.RunWith;

@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(packages = "com.tngtech.archunit.example")
public class RuleLibraryTest {
    @ArchTest
    public static final ArchRules LIBRARY = ArchRules.in(RuleSetsTest.class);

    @ArchTest
    public static final ArchRules FURTHER_CODING_RULES = ArchRules.in(CodingRulesWithRunnerMethodsTest.class);

    @ArchTest
    public static final ArchRules SLICES_ISOLATION_RULES = ArchRules.in(SlicesIsolationTest.class);
}
