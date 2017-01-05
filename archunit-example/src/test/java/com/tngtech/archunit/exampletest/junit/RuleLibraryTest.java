package com.tngtech.archunit.exampletest.junit;

import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.junit.AnalyseClasses;
import com.tngtech.archunit.junit.ArchRules;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import org.junit.runner.RunWith;

@RunWith(ArchUnitRunner.class)
@AnalyseClasses(packages = "com.tngtech.archunit.example")
public class RuleLibraryTest {
    @ArchTest
    public static final ArchRules<JavaClass> LIBRARY = ArchRules.in(RuleSetsTest.class);

    @ArchTest
    public static final ArchRules<JavaClass> FURTHER_CODING_RULES = ArchRules.in(CodingRulesWithRunnerMethodsTest.class);

    @ArchTest
    public static final ArchRules<JavaClass> SLICES_ISOLATION_RULES = ArchRules.in(SlicesIsolationTest.class);
}
