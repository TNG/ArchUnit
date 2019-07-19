package com.tngtech.archunit.exampletest.junit4;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchRules;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

@Category(Example.class)
@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(packages = "com.tngtech.archunit.example.layers")
public class RuleLibraryTest {
    @ArchTest
    public static final ArchRules LIBRARY = ArchRules.in(RuleSetsTest.class);

    @ArchTest
    public static final ArchRules FURTHER_CODING_RULES = ArchRules.in(CodingRulesTest.class);

    @ArchTest
    public static final ArchRules SLICES_ISOLATION_RULES = ArchRules.in(SlicesIsolationTest.class);
}
